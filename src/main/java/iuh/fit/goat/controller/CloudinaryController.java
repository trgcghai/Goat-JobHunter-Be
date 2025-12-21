package iuh.fit.goat.controller;

import iuh.fit.goat.dto.response.CloudinaryResponse;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.StorageException;
import iuh.fit.goat.service.CloudinaryService;
import iuh.fit.goat.util.FileUploadUtil;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CloudinaryController {
    private final CloudinaryService cloudinaryService;

    public CloudinaryController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/files")
    public ResponseEntity<?> uploadFile(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "folder") String folder
    ) throws InvalidException {
        if(file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty. Please upload file");
        }

        FileUploadUtil.assertAllowed(file, FileUploadUtil.FILE_PATTERN);

        String fileName = FileUploadUtil.getFileName(FilenameUtils.getBaseName(file.getOriginalFilename()));
        CloudinaryResponse response = this.cloudinaryService.handleUploadFile(file, folder, fileName);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/files/multiple")
    public ResponseEntity<?> uploadMultipleFiles(
            @RequestParam(name = "files", required = false) MultipartFile[] files,
            @RequestParam(name = "folder") String folder
    ) throws InvalidException {
        if(files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Files are empty. Please upload files");
        }

        List<CloudinaryResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            FileUploadUtil.assertAllowed(file, FileUploadUtil.FILE_PATTERN);

            String fileName = FileUploadUtil.getFileName(FilenameUtils.getBaseName(file.getOriginalFilename()));
            CloudinaryResponse response = this.cloudinaryService.handleUploadFile(file, folder, fileName);
            responses.add(response);
        }

        if (responses.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("All uploaded files are empty. Please upload valid files");
        }

        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }
}
