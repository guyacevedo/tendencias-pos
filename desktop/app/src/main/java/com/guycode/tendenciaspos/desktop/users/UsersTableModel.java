package com.guycode.tendenciaspos.desktop.users;

import com.guycode.tendenciaspos.contracts.identity.UserResponse;
import com.guycode.tendenciaspos.contracts.identity.UserRole;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.table.AbstractTableModel;

/** Filas de la lista de usuarios. */
final class UsersTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Usuario", "Nombre", "Roles", "Estado"};

    private List<UserResponse> users = List.of();

    void setUsers(List<UserResponse> users) {
        this.users = List.copyOf(users);
        fireTableDataChanged();
    }

    UserResponse at(int row) {
        return users.get(row);
    }

    int rowOf(long id) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).id() == id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return users.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        var user = users.get(row);
        return switch (column) {
            case 0 -> user.username();
            case 1 -> user.fullName();
            case 2 -> roles(user.roles());
            default -> user.active() ? "Activo" : "Inactivo";
        };
    }

    private static String roles(Set<UserRole> roles) {
        return roles.stream().map(UsersTableModel::label).sorted().collect(Collectors.joining(", "));
    }

    static String label(UserRole role) {
        return role == UserRole.ADMIN ? "Administrador" : "Cajero";
    }
}
