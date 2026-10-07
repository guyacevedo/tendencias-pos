package com.guycode.tendenciaspos.identity.application;

/** Usuario autenticado que ejecuta un caso de uso (para permisos propios y auditoría). */
public record Actor(long userId, String username) {}
