package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Permission;
import iuh.fit.goat.entity.Role;
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
    public Role handleCreateRole(Role role) {
        if(role.getPermissions() != null){
            List<Long> permissionIds = role.getPermissions().stream().map(Permission::getPermissionId).toList();
            List<Permission> permissions = this.permissionRepository.findByPermissionIdIn(permissionIds);
            role.setPermissions(permissions);
        }
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
    public void handleDeleteRole(long id) {
        Role currentRole = this.handleGetRoleById(id);
        if(currentRole.getUsers() != null){
            currentRole.getUsers().forEach(user -> {
                user.setRole(null);
                this.userRepository.save(user);
            });
        }
        this.roleRepository.deleteById(id);
    }

    @Override
    public Role handleGetRoleById(long id) {
        return this.roleRepository.findById(id).orElse(null);
    }

    @Override
    public Role handleGetRoleByName(String name) {
        return this.roleRepository.findByName(name);
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

    @Override
    public boolean handleExistRole(Role role) {
        return this.roleRepository.existsByName(role.getName());
    }
}
