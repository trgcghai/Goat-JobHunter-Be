package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Permission;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.PermissionService;
import iuh.fit.goat.util.SecurityUtil;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    @PostMapping
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody Permission permission)
            throws InvalidException {
        if(this.permissionService.handleExistPermission(permission)) {
            throw new InvalidException("Permission exists");
        }
        Permission res = this.permissionService.handleCreatePermission(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping
    public ResponseEntity<Permission> updatePermission(@Valid @RequestBody Permission permission)
            throws InvalidException {
        if(this.permissionService.handleGetPermissionById(permission.getPermissionId()) == null){
            throw new InvalidException("Permission doesn't exist");
        }
        Permission res = this.permissionService.handleUpdatePermission(permission);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Delete a permission")
    public ResponseEntity<Void> deletePermission(@PathVariable("id") String id) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(id)){
            throw new InvalidException("Id is number");
        }
        if(this.permissionService.handleGetPermissionById(Long.parseLong(id)) == null){
            throw new InvalidException("Permission doesn't exist");
        }
        this.permissionService.handleDeletePermission(Long.parseLong(id));
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable("id") long id) throws InvalidException {
        Permission permission = this.permissionService.handleGetPermissionById(id);
        if(permission == null){
            throw new InvalidException("Permission doesn't exist");
        }
        return ResponseEntity.status(HttpStatus.OK).body(permission);
    }

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllPermissions(
            @Filter Specification<Permission> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.permissionService.handleGetAllPermissions(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
