# Despliegue de la API

Flujo: push a `main` → **CI** (build + pruebas + integración) → si pasa, **Deploy** construye la imagen
**arm64** (el VPS es Oracle Ampere), la publica en GHCR (`ghcr.io/guyacevedo/tendencias-pos/api:<sha12>`
y `:latest`) y por SSH ejecuta en el VPS `docker compose pull && up -d`. Al final comprueba
`GET https://apipos.comercializadora-neymar.com/api/version`.

Mientras falten los secretos del VPS, Deploy solo publica la imagen y termina con un aviso.

## Cómo encaja en el VPS

El VPS ya tiene un **proxy de borde** en Docker (`/opt/pos-neymar-edge`, contenedor `pos-cn-proxy`,
Nginx 1.27) que publica 80/443 y enruta por dominio a cada stack por una red Docker compartida.
Tendencias se suma igual que `pos-prod`/`pos-test`:

```
internet ─443─▶ pos-cn-proxy ──red-tpos──▶ tpos-api:8080 ──db (interna)──▶ postgres
```

- La API **no publica puertos** en el host; el proxy la resuelve como `tpos-api` en `red-tpos`.
- Config de Nginx: `deploy/nginx/apipos.conf.template` → `/opt/pos-neymar-edge/proxy/conf.d/`.
- Certificado: certbot (contenedor) en modo webroot sobre `proxy/acme`, guardado en `proxy/certs`.
  La tarea cron diaria del edge (`certbot renew` + `restart proxy`) ya lo renueva.

Todo lo de abajo se hace **una sola vez**, como `ubuntu` (tiene sudo) salvo donde dice `deploy`.

## 1. DNS

Registro `A` de `apipos.comercializadora-neymar.com` → IP del VPS.
Comprobar: `getent hosts apipos.comercializadora-neymar.com`.

## 2. Usuario `deploy`, carpeta y red

```bash
sudo adduser --disabled-password --gecos "" deploy
sudo usermod -aG docker deploy          # sin sudo: solo Docker
sudo install -d -o deploy -g deploy -m 750 /opt/tendencias-pos
docker network create red-tpos
```

## 3. Llave SSH para GitHub Actions

El VPS solo acepta llaves (no contraseñas), así que `ssh-copy-id` no sirve para `deploy`: la llave
pública se instala a mano.

```bash
ssh-keygen -t ed25519 -C "github-deploy-tpos" -f ~/.ssh/tpos_deploy -N ""
sudo install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
sudo tee -a /home/deploy/.ssh/authorized_keys < ~/.ssh/tpos_deploy.pub > /dev/null
sudo chown deploy:deploy /home/deploy/.ssh/authorized_keys
sudo chmod 600 /home/deploy/.ssh/authorized_keys
ssh -i ~/.ssh/tpos_deploy deploy@IP_DEL_VPS docker ps     # debe listar contenedores
```

La llave **privada** (`cat ~/.ssh/tpos_deploy`) va solo al secreto de GitHub (paso 7); no se pega en
ningún otro lado. Después: `rm ~/.ssh/tpos_deploy`.

## 4. Archivo `.env` (como `deploy`)

```bash
sudo -iu deploy
cd /opt/tendencias-pos
nano .env            # contenido de deploy/.env.example, con valores reales
chmod 600 .env
exit
```

- `TPOS_IMAGE=ghcr.io/guyacevedo/tendencias-pos/api:latest` (el despliegue lo actualiza solo).
- `POSTGRES_PASSWORD`: `openssl rand -base64 32 | tr -d '/+='`. Guardarla también fuera del VPS.
- `JWT_SECRET`: `openssl rand -base64 48 | tr -d '/+='` (mínimo 32 bytes). Sin ella la API no arranca;
  cambiarla invalida los access tokens vigentes (los usuarios vuelven a entrar).
- `ADMIN_INITIAL_PASSWORD`: clave del usuario `admin` que se crea solo si la base no tiene usuarios
  (10 a 128 caracteres). En el primer ingreso el sistema obliga a cambiarla; luego puede quitarse del `.env`.

`compose.prod.yml` no hace falta copiarlo: el workflow lo sube en cada despliegue.

## 5. Proxy de borde

**El orden importa:** si Nginx carga un bloque 443 cuyo certificado no existe, el proxy no arranca y
se caen también `pos` y `testpos`. Los cambios se aplican con `nginx -t && nginx -s reload` dentro
del contenedor (sin reiniciarlo).

1. Conectar el proxy a la red de Tendencias, en vivo y en el compose del edge (para que sobreviva a un
   `up`):

   ```bash
   cd /opt/pos-neymar-edge
   docker network connect red-tpos pos-cn-proxy
   cp docker-compose.edge.yml docker-compose.edge.yml.bak
   # en services.proxy.networks agregar "- red-tpos", y en networks al final:
   #   red-tpos:
   #     external: true
   nano docker-compose.edge.yml
   docker compose -f docker-compose.edge.yml --env-file .env.edge config --quiet && echo compose-ok
   ```

2. Bloque provisional solo HTTP para el desafío ACME:

   ```bash
   cat > proxy/conf.d/apipos.conf.template <<'NGINX'
   server {
       listen 80;
       server_name apipos.comercializadora-neymar.com;
       location /.well-known/acme-challenge/ { root /var/www/acme; }
       location / { return 301 https://$host$request_uri; }
   }
   NGINX
   docker exec pos-cn-proxy sh -c 'cp /etc/nginx/templates/apipos.conf.template /etc/nginx/conf.d/apipos.conf && nginx -t && nginx -s reload'
   ```

3. Certificado (mismo método que los otros dominios):

   ```bash
   docker run --rm -v "$PWD/proxy/certs:/etc/letsencrypt" -v "$PWD/proxy/acme:/var/www/acme" \
     certbot/certbot certonly --webroot -w /var/www/acme \
     -d apipos.comercializadora-neymar.com --email TU_CORREO --agree-tos --no-eff-email
   sudo ls proxy/certs/live/apipos.comercializadora-neymar.com/    # fullchain.pem, privkey.pem
   ```

4. Configuración definitiva: copiar el contenido de `deploy/nginx/apipos.conf.template` del repo
   encima de `proxy/conf.d/apipos.conf.template` y aplicar:

   ```bash
   nano proxy/conf.d/apipos.conf.template
   docker exec pos-cn-proxy sh -c 'cp /etc/nginx/templates/apipos.conf.template /etc/nginx/conf.d/apipos.conf && nginx -t && nginx -s reload'
   curl -sI https://apipos.comercializadora-neymar.com/api/version   # 502 hasta el primer despliegue
   ```

## 6. Imagen pública en GHCR (opcional)

El workflow inicia sesión en GHCR desde el VPS con un token temporal, así que no hace falta. Si se
prefiere, el paquete `tendencias-pos/api` puede marcarse público en GitHub → *Packages*.

## 7. Secretos en GitHub

Repo → *Settings → Secrets and variables → Actions → New repository secret*:

| Secreto | Valor |
| --- | --- |
| `VPS_HOST` | IP del VPS |
| `VPS_USER` | `deploy` |
| `VPS_SSH_KEY` | contenido completo de `~/.ssh/tpos_deploy` (llave **privada**) |

## 8. Primer despliegue

Repo → *Actions → Deploy → Run workflow* (o un push a `main`). Verificar:

```bash
curl https://apipos.comercializadora-neymar.com/api/version
```

## Operación

```bash
cd /opt/tendencias-pos
docker compose -f compose.prod.yml ps
docker compose -f compose.prod.yml logs -f api
# Volver a una versión anterior: poner su etiqueta (sha de 12 caracteres) en TPOS_IMAGE y
docker compose -f compose.prod.yml up -d
```

Los datos viven en el volumen `tendencias-pos_pgdata`. Las copias de seguridad llegan en una fase posterior.
