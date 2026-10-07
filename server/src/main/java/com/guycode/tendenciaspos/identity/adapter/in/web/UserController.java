package com.guycode.tendenciaspos.identity.adapter.in.web;

import com.guycode.tendenciaspos.contracts.identity.CreateUserRequest;
import com.guycode.tendenciaspos.contracts.identity.ResetPasswordRequest;
import com.guycode.tendenciaspos.contracts.identity.UpdateUserRequest;
import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.identity.application.UserAdminService;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administración de usuarios. Los usuarios no se borran: se desactivan. */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
class UserController {
    private final UserAdminService service;
    private final Clock clock;

    UserController(UserAdminService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    List<UserResponse> list() {
        return service.list().stream().map(u -> IdentityMapper.user(u, clock)).toList();
    }

    @GetMapping("/{id}")
    UserResponse get(@PathVariable long id) {
        return IdentityMapper.user(service.get(id), clock);
    }

    @PostMapping
    ResponseEntity<UserResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateUserRequest body) {
        var user = service.create(
                IdentityMapper.actor(jwt),
                body.username(),
                body.fullName(),
                body.password(),
                IdentityMapper.toDomain(body.roles()));
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(IdentityMapper.user(user, clock));
    }

    @PutMapping("/{id}")
    UserResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id, @RequestBody UpdateUserRequest body) {
        return IdentityMapper.user(
                service.update(IdentityMapper.actor(jwt), id, body.fullName(), IdentityMapper.toDomain(body.roles())),
                clock);
    }

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(
            @AuthenticationPrincipal Jwt jwt, @PathVariable long id, @RequestBody ResetPasswordRequest body) {
        service.resetPassword(IdentityMapper.actor(jwt), id, body.newPassword());
    }

    @PostMapping("/{id}/deactivate")
    UserResponse deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return IdentityMapper.user(service.deactivate(IdentityMapper.actor(jwt), id), clock);
    }

    @PostMapping("/{id}/activate")
    UserResponse activate(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return IdentityMapper.user(service.activate(IdentityMapper.actor(jwt), id), clock);
    }
}
