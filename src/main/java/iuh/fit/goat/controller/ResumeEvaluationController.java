package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.resume.ResumeEvaluationResponse;
import iuh.fit.goat.entity.ResumeEvaluation;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.EvaluationService;
import iuh.fit.goat.service.ResumeService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class ResumeEvaluationController {
    private final AiService aiService;
    private final ResumeService resumeService;
    private final EvaluationService evaluationService;

    @PostMapping("/resume")
    public ResponseEntity<ResumeEvaluationResponse> evaluateResume(@RequestParam String resumeUrl) throws InvalidException {
        ResumeEvaluationResponse response = this.aiService.evaluateResume(resumeUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/resumes/{resumeId}")
    public ResponseEntity<ResultPaginationResponse> getAllResumeEvaluationByResume(
            @Filter Specification<ResumeEvaluation> spec, Pageable pageable,
            @PathVariable("resumeId") String resumeId
    ) throws InvalidException
    {
        if(!SecurityUtil.checkValidNumber(resumeId)) throw new InvalidException("Invalid resume id");

        ResultPaginationResponse result = this.resumeService.handleGetAllResumeEvaluationByResume(
                spec, pageable, Long.parseLong(resumeId)
        );
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeEvaluationResponse> getResumeEvaluationById(@PathVariable("id") String id) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(id)) throw new InvalidException("Id is number");

        ResumeEvaluation resumeEvaluation = this.evaluationService.handleGetResumeEvaluationById(Long.parseLong(id));
        if(resumeEvaluation == null) throw new InvalidException("Evaluation not found");

        return ResponseEntity.status(HttpStatus.OK).body(
                this.evaluationService.handleConvertToResumeEvaluationResponse(resumeEvaluation)
        );
    }

}
