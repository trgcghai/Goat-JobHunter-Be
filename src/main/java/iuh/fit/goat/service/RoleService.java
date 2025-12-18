package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.role.RoleCreateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface RoleService {
    Role handleCreateRole(RoleCreateRequest role);

    Role handleUpdateRole(Role role);

    Role handleActivateRole(long id) throws InvalidException;

    Role handleDeactivateRole(long id) throws InvalidException;

    Role handleGetRoleById(long id);

    ResultPaginationResponse handleGetAllRoles(Specification<Role> spec, Pageable pageable);
}
