# Despliegue de la API

Flujo: push a `main` → **CI** (build + pruebas + integración) → si pasa, **Deploy** construye la imagen,
la publica en GHCR (`ghcr.io/<usuario>/<repo>/api:<sha>` y `:latest`) y por SSH ejecuta en el VPS
`docker compose pull && up -d`. Al final comprueba `GET https://apipos.comercializadora-neymar.com/api/version`.

Mientras falten los secretos del VPS, Deploy solo publica la imagen y termina con un aviso.

Los pasos de abajo se hacen **una sola vez**. Los comandos asumen Ubuntu/Debian con Nginx ya instalado.

## 1. DNS

Registro `A` (y `AAAA` si el VPS tiene IPv6) de `apipos.comercializadora-neymar.com` a la IP del VPS.
Comprobar: `dig +short apipos.comercializadora-neymar.com`.

## 2. Docker en el VPS

```bash
curl -fsSL https://get.docker.com | sudo sh     # instala Docker Engine + plugin compose
docker compose version                          # debe responder v2.x
```

## 3. Usuario `deploy` y carpeta

```bash
sudo adduser --disabled-password --gecos "" deploy
sudo usermod -aG docker deploy
sudo install -d -o deploy -g deploy -m 750 /opt/tendencias-pos
```

Llave SSH solo para GitHub Actions (en tu Mac):

```bash
ssh-keygen -t ed25519 -C "github-deploy-tpos" -f ~/.ssh/tpos_deploy -N ""
ssh-copy-id -i ~/.ssh/tpos_deploy.pub deploy@IP_DEL_VPS
ssh -i ~/.ssh/tpos_deploy deploy@IP_DEL_VPS docker ps   # debe funcionar sin sudo
```

## 4. Archivo `.env`

```bash
sudo -iu deploy
cd /opt/tendencias-pos
nano .env            # pegar el contenido de deploy/.env.example y ajustar valores
chmod 600 .env
```

- `TPOS_IMAGE`: `ghcr.io/<tu-usuario-en-minúsculas>/<repo>/api:latest` (el despliegue lo actualiza solo).
- `POSTGRES_PASSWORD`: `openssl rand -base64 32 | tr -d '/+='`. Guardarla también fuera del VPS.

`compose.prod.yml` no hace falta copiarlo: el workflow lo sube en cada despliegue.

## 5. Nginx + certificado

1. Configuración provisional solo HTTP para obtener el certificado:

   ```bash
   sudo mkdir -p /var/www/certbot
   sudo tee /etc/nginx/sites-available/apipos.conf >/dev/null <<'NGINX'
   server {
       listen 80;
       listen [::]:80;
       server_name apipos.comercializadora-neymar.com;
       location ^~ /.well-known/acme-challenge/ { root /var/www/certbot; }
   }
   NGINX
   sudo ln -s /etc/nginx/sites-available/apipos.conf /etc/nginx/sites-enabled/apipos.conf
   sudo nginx -t && sudo systemctl reload nginx
   ```

2. Certificado (si no está certbot: `sudo apt install certbot`):

   ```bash
   sudo certbot certonly --webroot -w /var/www/certbot \
     -d apipos.comercializadora-neymar.com --email TU_CORREO --agree-tos --no-eff-email
   ```

3. Configuración definitiva: copiar `deploy/nginx/apipos.conf` del repo encima de
   `/etc/nginx/sites-available/apipos.conf`, luego `sudo nginx -t && sudo systemctl reload nginx`.

4. Renovación: certbot instala un timer que usa el mismo webroot. Para recargar Nginx tras renovar:

   ```bash
   echo -e '#!/bin/sh\nsystemctl reload nginx' | sudo tee /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
   sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
   sudo certbot renew --dry-run
   ```

La API escucha solo en `127.0.0.1:8080`; no hace falta abrir puertos nuevos en el firewall (80/443
ya los usa Nginx). `/actuator/*` no se expone hacia afuera.

## 6. Secretos en GitHub

Repo → *Settings → Secrets and variables → Actions → New repository secret*:

| Secreto | Valor |
| --- | --- |
| `VPS_HOST` | IP o dominio del VPS |
| `VPS_USER` | `deploy` |
| `VPS_SSH_KEY` | contenido completo de `~/.ssh/tpos_deploy` (la llave **privada**) |

La imagen se descarga en el VPS con el token temporal del workflow (no hay que crear otro token).

## 7. Primer despliegue

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
