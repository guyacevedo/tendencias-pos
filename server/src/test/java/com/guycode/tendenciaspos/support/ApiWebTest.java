package com.guycode.tendenciaspos.support;

import com.guycode.tendenciaspos.shared.config.ClockConfig;
import com.guycode.tendenciaspos.shared.security.SecurityConfig;
import com.guycode.tendenciaspos.shared.web.GlobalExceptionHandler;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** Para pruebas {@code @WebMvcTest}: seguridad real (JWT), errores Problem Details y reloj. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, ClockConfig.class, TestTokens.class})
@TestPropertySource(properties = "tpos.security.jwt-secret=" + TestTokens.SECRET)
public @interface ApiWebTest {}
