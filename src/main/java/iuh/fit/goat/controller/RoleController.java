package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.role.RoleCreateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.RoleService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<Role> createRole(@Valid @RequestBody RoleCreateRequest role) {
        Role res = this.roleService.handleCreateRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping
    public ResponseEntity<Role> updateRole(@Valid @RequestBody Role role) throws InvalidException {
        if(this.roleService.handleGetRoleById(role.getRoleId()) == null){
            throw new InvalidException("Role doesn't exist");
        }
        Role res = this.roleService.handleUpdateRole(role);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Role> activateRole(@PathVariable("id") String id) throws InvalidException {
        if(!SecurityUtil.checkValidNumber("id")){
            throw new InvalidException("Id is number");
        }
        Role res = this.roleService.handleActivateRole(Long.parseLong(id));
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Role> deactivateRole(@PathVariable("id") String id) throws InvalidException {
        if(!SecurityUtil.checkValidNumber("id")){
            throw new InvalidException("Id is number");
        }
        Role res = this.roleService.handleDeactivateRole(Long.parseLong(id));
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable("id") long id) throws InvalidException {
        Role role = this.roleService.handleGetRoleById(id);
        if(role == null){
            throw new InvalidException("Role doesn't exist");
        }
        return ResponseEntity.status(HttpStatus.OK).body(role);
    }

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllRoles(
            @Filter Specification<Role> spec, Pageable pageable
    ) {
        ResultPaginationResponse result = this.roleService.handleGetAllRoles(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
