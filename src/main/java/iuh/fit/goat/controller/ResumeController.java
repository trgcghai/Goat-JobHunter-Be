package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.resume.CreateResumeRequest;
import iuh.fit.goat.dto.request.resume.UpdateResumeRequest;
import iuh.fit.goat.dto.response.resume.ResumeResponse;
import iuh.fit.goat.dto.response.resume.ResumeStatusResponse;
import iuh.fit.goat.entity.Resume;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.ResumeService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URL;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resumes")
public class ResumeController {
    private final ResumeService resumeService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> createResume(@Valid @ModelAttribute CreateResumeRequest request) throws InvalidException {
        Resume resume = this.resumeService.handleCreateResume(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.resumeService.handleConvertToResumeResponse(resume));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResume(@RequestParam("resumeId") String resumeId) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(resumeId)) throw new InvalidException("Invalid resume id");

        this.resumeService.handleDeleteResume(Long.parseLong(resumeId));
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PutMapping("/default")
    public ResponseEntity<ResumeStatusResponse> defaultResume(@RequestParam("resumeId") String resumeId) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(resumeId)) throw new InvalidException("Invalid resume id");

        ResumeStatusResponse response = this.resumeService.handleDefaultResume(Long.parseLong(resumeId));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/default")
    public ResponseEntity<ResumeStatusResponse> unDefaultResume(@RequestParam("resumeId") String resumeId) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(resumeId)) throw new InvalidException("Invalid resume id");

        ResumeStatusResponse response = this.resumeService.handleUnDefaultResume(Long.parseLong(resumeId));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/public")
    public ResponseEntity<ResumeStatusResponse> publicResume(@RequestParam("resumeId") String resumeId) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(resumeId)) throw new InvalidException("Invalid resume id");

        ResumeStatusResponse response = this.resumeService.handlePublicResume(Long.parseLong(resumeId));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/public")
    public ResponseEntity<ResumeStatusResponse> privateResume(@RequestParam("resumeId") String resumeId) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(resumeId)) throw new InvalidException("Invalid resume id");

        ResumeStatusResponse response = this.resumeService.handlePrivateResume(Long.parseLong(resumeId));
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> downloadResume(@PathVariable("id") String id) throws InvalidException, IOException {
        if(!SecurityUtil.checkValidNumber(id)) throw new InvalidException("Invalid resume id");

        Resume resume = this.resumeService.handleGetResumeById(Long.parseLong(id));
        if(resume == null) throw new InvalidException("Resume not found");

        URL url = new URL(resume.getFileUrl());
        InputStreamResource resource = new InputStreamResource(url.openStream());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resume.getFileName() + "\""
                )
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resume.getFileSize())
                .body(resource);
    }

    @PutMapping("/title")
    public ResponseEntity<ResumeResponse> updateTitle(@Valid @RequestBody UpdateResumeRequest request) throws InvalidException {
        Resume resume = this.resumeService.handleUpdateTitle(request.getResumeId(), request.getTitle());
        return ResponseEntity.status(HttpStatus.OK).body(this.resumeService.handleConvertToResumeResponse(resume));
    }
}
