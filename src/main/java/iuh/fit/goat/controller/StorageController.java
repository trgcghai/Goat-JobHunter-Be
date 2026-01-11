package iuh.fit.goat.controller;

import com.amazonaws.services.s3.model.S3ObjectInputStream;
import iuh.fit.goat.dto.response.StorageResponse;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class StorageController {
    private final StorageService storageService;

    @PostMapping
    public ResponseEntity<?> uploadFile(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "folder") String folder
    ) throws InvalidException
    {
        if(file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty. Please upload file");
        }

        FileUploadUtil.assertAllowed(file, FileUploadUtil.FILE_PATTERN);

        StorageResponse response = this.storageService.handleUploadFile(file, folder);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/multiple")
    public ResponseEntity<?> uploadMultipleFiles(
            @RequestParam(name = "files", required = false) MultipartFile[] files,
            @RequestParam(name = "folder") String folder
    ) throws InvalidException {
        if(files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Files are empty. Please upload files");
        }

        List<StorageResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            FileUploadUtil.assertAllowed(file, FileUploadUtil.FILE_PATTERN);

            StorageResponse response = this.storageService.handleUploadFile(file, folder);
            responses.add(response);
        }

        if (responses.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("All uploaded files are empty. Please upload valid files");
        }

        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFile(@RequestParam("key") String key) throws InvalidException {
        if(key == null || key.isEmpty()) {
           throw new InvalidException("File key is required for deletion");
        }
        this.storageService.handleDeleteFile(key);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("key") String key) throws InvalidException {
        if(key == null || key.isEmpty()) {
            throw new InvalidException("File key is required for deletion");
        }

        S3ObjectInputStream stream = this.storageService.handleDownloadFile(key);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"file.pdf\"")
                .body(new InputStreamResource(stream));
    }

}
