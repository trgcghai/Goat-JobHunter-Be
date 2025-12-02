package iuh.fit.goat.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface AdminService {
    ResponseEntity<Resource> handleBackupDatabase();
}
