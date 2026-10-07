package com.guycode.tendenciaspos.desktop.api;

import com.guycode.tendenciaspos.contracts.identity.CreateUserRequest;
import com.guycode.tendenciaspos.contracts.identity.ResetPasswordRequest;
import com.guycode.tendenciaspos.contracts.identity.UpdateUserRequest;
import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import java.util.List;
import java.util.Set;

/** Endpoints de usuarios ({@code /api/users}). */
public final class UsersApi implements UserGateway {
    public static final String USERS_PATH = "/api/users";

    private final ApiClient client;

    public UsersApi(ApiClient client) {
        this.client = client;
    }

    @Override
    public List<UserResponse> list() {
        return List.of(client.get(USERS_PATH, UserResponse[].class));
    }

    @Override
    public UserResponse create(String username, String fullName, String password, Set<UserRole> roles) {
        return client.post(USERS_PATH, new CreateUserRequest(username, fullName, password, roles), UserResponse.class);
    }

    @Override
    public UserResponse update(long id, String fullName, Set<UserRole> roles) {
        return client.put(USERS_PATH + "/" + id, new UpdateUserRequest(fullName, roles), UserResponse.class);
    }

    @Override
    public void resetPassword(long id, String newPassword) {
        client.post(USERS_PATH + "/" + id + "/password", new ResetPasswordRequest(newPassword), Void.class);
    }

    @Override
    public UserResponse deactivate(long id) {
        return client.post(USERS_PATH + "/" + id + "/deactivate", null, UserResponse.class);
    }

    @Override
    public UserResponse activate(long id) {
        return client.post(USERS_PATH + "/" + id + "/activate", null, UserResponse.class);
    }
}
