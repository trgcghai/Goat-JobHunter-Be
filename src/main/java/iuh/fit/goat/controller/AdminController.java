package iuh.fit.goat.controller;

import iuh.fit.goat.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
//    private final AdminService adminService;
//
//    @GetMapping("/backup")
//    public ResponseEntity<Resource> backup() {
//        return this.adminService.handleBackupDatabase();
//    }
}
