package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.resume.CreateResumeRequest;
import iuh.fit.goat.dto.response.resume.ResumeResponse;
import iuh.fit.goat.entity.Resume;

public interface ResumeService {
    Resume handleCreateResume(CreateResumeRequest request);

    ResumeResponse handleConvertToResumeResponse(Resume resume);
}
