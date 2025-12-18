package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.role.RoleCreateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Permission;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.PermissionRepository;
import iuh.fit.goat.repository.RoleRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Override
    public Role handleCreateRole(RoleCreateRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        } else {
            role.setDescription("");
        }
        role.setActive(true);
        role.setPermissions(List.of());
        return this.roleRepository.save(role);
    }

    @Override
    public Role handleUpdateRole(Role role) {
        Role resRole = this.handleGetRoleById(role.getRoleId());

        if(role.getPermissions() != null){
            List<Long> permissionIds = role.getPermissions().stream().map(Permission::getPermissionId).toList();
            List<Permission> permissions = this.permissionRepository.findByPermissionIdIn(permissionIds);
            role.setPermissions(permissions);
        }

        resRole.setDescription(role.getDescription());
        resRole.setActive(role.isActive());
        resRole.setName(role.getName());
        resRole.setPermissions(role.getPermissions());

        return this.roleRepository.save(resRole);
    }

    @Override
    public Role handleActivateRole(long id) throws InvalidException {
        Role role = this.roleRepository.findById(id)
                .orElseThrow(() -> new InvalidException("Role doesn't exist"));
        if (!role.isActive()) {
            role.setActive(true);
            return this.roleRepository.save(role);
        }
        return role;
    }

    @Override
    public Role handleDeactivateRole(long id) throws InvalidException {
        Role role = this.roleRepository.findById(id)
                .orElseThrow(() -> new InvalidException("Role doesn't exist"));
        if (role.isActive()) {
            role.setActive(false);
            return this.roleRepository.save(role);
        }
        return role;
    }

    @Override
    public Role handleGetRoleById(long id) {
        return this.roleRepository.findById(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllRoles(Specification<Role> spec, Pageable pageable) {
        Page<Role> page = this.roleRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());

        return new ResultPaginationResponse(meta, page.getContent());
    }
}
