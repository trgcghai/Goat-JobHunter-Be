package iuh.fit.goat.service.impl;

import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    @Value("${spring.datasource.username}")
    private String dbUsername;
    @Value("${spring.datasource.password}")
    private String dbPassword;
    @Value("${database-name}")
    private String dbDatabase;

    @Override
    public ResponseEntity<Resource> handleBackupDatabase() {
        try {
            File tempFile = File.createTempFile("backup_", ".sql");

            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "-u", dbUsername,
                    "-p" + dbPassword,
                    dbDatabase
            );
            pb.redirectOutput(tempFile);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new InvalidException("Backup failed. Exit code: " + exitCode);
            }

            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(tempFile.toPath()));

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=backup.sql")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            throw new RuntimeException("Backup failed: " + e.getMessage());
        }
    }
}
