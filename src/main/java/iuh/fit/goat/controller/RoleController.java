package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.role.RoleCreateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleController {
//    private final RoleService roleService;
//
//    @PostMapping("/roles")
//    public ResponseEntity<Role> createRole(@Valid @RequestBody RoleCreateRequest role) {
//        Role res = this.roleService.handleCreateRole(role);
//        return ResponseEntity.status(HttpStatus.CREATED).body(res);
//    }
//
//    @PutMapping("/roles")
//    public ResponseEntity<Role> updateRole(@Valid @RequestBody Role role) throws InvalidException {
//        if(this.roleService.handleGetRoleById(role.getRoleId()) == null){
//            throw new InvalidException("Role doesn't exist");
//        }
//        Role res = this.roleService.handleUpdateRole(role);
//        return ResponseEntity.status(HttpStatus.OK).body(res);
//    }
//
//    @PutMapping("/roles/{id}/activate")
//    public ResponseEntity<Role> activateRole(@PathVariable("id") String id) throws InvalidException {
//        Pattern pattern = Pattern.compile("^[0-9]+$");
//        if(!pattern.matcher(id).matches()){
//            throw new InvalidException("Id is number");
//        }
//        Role res = this.roleService.handleActivateRole(Long.parseLong(id));
//        return ResponseEntity.status(HttpStatus.OK).body(res);
//    }
//
//    @PutMapping("/roles/{id}/deactivate")
//    public ResponseEntity<Role> deactivateRole(@PathVariable("id") String id) throws InvalidException {
//        Pattern pattern = Pattern.compile("^[0-9]+$");
//        if(!pattern.matcher(id).matches()){
//            throw new InvalidException("Id is number");
//        }
//        Role res = this.roleService.handleDeactivateRole(Long.parseLong(id));
//        return ResponseEntity.status(HttpStatus.OK).body(res);
//    }
//
//    @DeleteMapping("/roles/{id}")
//    public ResponseEntity<Void> deleteRole(@PathVariable("id") String id) throws InvalidException {
//        Pattern pattern = Pattern.compile("^[0-9]+$");
//        if(!pattern.matcher(id).matches()){
//            throw new InvalidException("Id is number");
//        }
//        if(this.roleService.handleGetRoleById(Long.parseLong(id)) == null){
//            throw new InvalidException("Role doesn't exist");
//        }
//        this.roleService.handleDeleteRole(Long.parseLong(id));
//        return ResponseEntity.status(HttpStatus.OK).body(null);
//    }
//
//    @GetMapping("/roles/{id}")
//    public ResponseEntity<Role> getRoleById(@PathVariable("id") long id) throws InvalidException {
//        Role role = this.roleService.handleGetRoleById(id);
//        if(role == null){
//            throw new InvalidException("Role doesn't exist");
//        }
//        return ResponseEntity.status(HttpStatus.OK).body(role);
//    }
//
//    @GetMapping("/roles")
//    public ResponseEntity<ResultPaginationResponse> getAllRoles(
//            @Filter Specification<Role> spec, Pageable pageable
//    ) {
//        ResultPaginationResponse result = this.roleService.handleGetAllRoles(spec, pageable);
//        return ResponseEntity.status(HttpStatus.OK).body(result);
//    }
}
