package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Permission;
import iuh.fit.goat.repository.PermissionRepository;
import iuh.fit.goat.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    @Override
    public Permission handleCreatePermission(Permission permission) {
        return this.permissionRepository.save(permission);
    }

    @Override
    public Permission handleUpdatePermission(Permission permission) {
        Permission resPermission = this.handleGetPermissionById(permission.getPermissionId());

        resPermission.setApiPath(permission.getApiPath());
        resPermission.setMethod(permission.getMethod());
        resPermission.setModule(permission.getModule());
        resPermission.setName(permission.getName());

        return this.permissionRepository.save(resPermission);
    }

    @Override
    public void handleDeletePermission(long id) {
        Permission permission = this.handleGetPermissionById(id);

        if(permission.getRoles() != null){
            permission.getRoles().forEach(role -> role.getPermissions().remove(permission));
        }

        this.permissionRepository.delete(permission);
    }

    @Override
    public Permission handleGetPermissionById(long id){
        return this.permissionRepository.findById(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllPermissions(Specification<Permission> spec, Pageable pageable) {
        Page<Permission> page = this.permissionRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());

        return new ResultPaginationResponse(meta, page.getContent());
    }

    @Override
    public boolean handleExistPermission(Permission permission) {
        return this.permissionRepository.existsByApiPathAndModuleAndMethod(
                permission.getApiPath(), permission.getModule(), permission.getMethod()
        );
    }
}
