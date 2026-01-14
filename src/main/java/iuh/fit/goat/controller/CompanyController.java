package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.company.CompanyUpdateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.dto.response.job.JobResponse;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.Job;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.CompanyService;
import iuh.fit.goat.service.JobService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;
    private final JobService jobService;
    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(@Valid @RequestBody Company company) throws InvalidException
    {
        if (this.userService.handleExistsByEmail(company.getEmail())) {
            throw new InvalidException("Email exists: " + company.getEmail());
        }

        if(this.companyService.handleGetCompanyByName(company.getName()) != null) {
            throw new InvalidException("Company name exists: " + company.getName());
        }

        String hashPassword = this.passwordEncoder.encode(company.getPassword());
        company.setPassword(hashPassword);

        Company newCompany = this.companyService.handleCreateCompany(company);
        CompanyResponse response = this.companyService.convertToCompanyResponse(newCompany);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<CompanyResponse> updateCompany(
            @Valid @RequestBody CompanyUpdateRequest request
    ) throws InvalidException {

        Company updatedCompany = this.companyService.handleUpdateCompany(request);

        if (updatedCompany != null) {
            CompanyResponse response = this.companyService.convertToCompanyResponse(updatedCompany);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } else {
            throw new InvalidException("Company not found");
        }
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Soft delete a company")
    public ResponseEntity<Void> deleteCompany(@PathVariable("id") long id) {
        this.companyService.handleDeleteCompany(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable("id") String id) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(id)) {
            throw new InvalidException("Id is number");
        }

        Company company = this.companyService.handleGetCompanyById(Long.parseLong(id));
        if(company == null) {
            throw new InvalidException("Company not found");
        }

        CompanyResponse response = this.companyService.convertToCompanyResponse(company);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/slug/{name}")
    public ResponseEntity<CompanyResponse> getCompanyByName(@PathVariable("name") String name) throws InvalidException {
        String normalizedName = name.replace("-", " ");
        Company company = this.companyService.handleGetCompanyByName(normalizedName);
        if(company == null) {
            throw new InvalidException("Company not found");
        }

        CompanyResponse response = this.companyService.convertToCompanyResponse(company);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllCompanies(
            @Filter Specification<Company> spec, Pageable pageable
    ) {
        ResultPaginationResponse res = this.companyService.handleGetAllCompanies(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/{id}/group-addresses")
    public ResponseEntity<Map<String, List<String>>> groupAddressesCityByCompany(
            @PathVariable("id") String id
    ) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(id)) {
            throw new InvalidException("Id is number");
        }

        Company company = this.companyService.handleGetCompanyById(Long.parseLong(id));
        if(company == null) {
            throw new InvalidException("Company not found");
        }

        Map<String, List<String>> res = this.companyService.handleGroupAddressesCityByCompany(Long.parseLong(id));
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/available")
    public ResponseEntity<ResultPaginationResponse> getAllAvailableCompanies(
            @Filter Specification<Company> spec, Pageable pageable
    ) {
        Specification<Company> baseSpec = (spec != null) ? spec : Specification.unrestricted();

        Specification<Company> finalSpec = baseSpec
                .and((root, query, criteriaBuilder)
                    -> criteriaBuilder.and(
                            criteriaBuilder.isTrue(root.get("enabled")),
                            criteriaBuilder.isNull(root.get("deletedAt"))
                    )
                );

        ResultPaginationResponse res = this.companyService.handleGetAllCompanies(finalSpec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @GetMapping("/{companyId}/jobs/skills")
    public ResponseEntity<Map<Long, String>> findDistinctSkillsByCompany(
            @PathVariable("companyId") String companyId
    ) throws InvalidException {
        if(!SecurityUtil.checkValidNumber(companyId)) {
            throw new InvalidException("Id is number");
        }

        Company company = this.companyService.handleGetCompanyById(Long.parseLong(companyId));
        if(company == null) {
            throw new InvalidException("Company not found");
        }

        return ResponseEntity.ok(this.companyService.handleFindDistinctSkillsByCompany(Long.parseLong(companyId)));
    }

    @GetMapping("/{companyId}/jobs")
    public ResponseEntity<List<JobResponse>> getAllAvailableJobsByCompanyId(
            @PathVariable("companyId") String companyId, @Filter Specification<Job> spec
    ) throws InvalidException {
        if (!SecurityUtil.checkValidNumber(companyId)) {
            throw new InvalidException("Id is number");
        }

        List<JobResponse> result = this.jobService.handleGetAllAvailableJobsByCompanyId(Long.parseLong(companyId), spec);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/name")
    public ResponseEntity<List<String>> getAllCompanyNames() {
        List<String> result = this.companyService.handleGetAllCompanyNames();
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

}
