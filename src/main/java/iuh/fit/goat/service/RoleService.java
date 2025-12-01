package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.role.RoleCreateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface RoleService {
    Role handleCreateRole(RoleCreateRequest role);

    Role handleUpdateRole(Role role);

    void handleDeleteRole(long id);

    Role handleGetRoleById(long id);

    Role handleGetRoleByName(String name);

    ResultPaginationResponse handleGetAllRoles(Specification<Role> spec, Pageable pageable);
}
