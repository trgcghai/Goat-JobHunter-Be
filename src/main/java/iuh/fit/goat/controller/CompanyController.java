package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(@PathVariable("id") String id) throws InvalidException {
        Pattern pattern = Pattern.compile("^[0-9]+$");
        if(!pattern.matcher(id).matches()) {
            throw new InvalidException("Id is number");
        }

        Company company = this.companyService.handleGetCompanyById(Long.parseLong(id));
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
        Pattern pattern = Pattern.compile("^[0-9]+$");
        if(!pattern.matcher(id).matches()) {
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

        Specification<Company> finalSpec = baseSpec.and(
                (root, query, criteriaBuilder)
                        -> criteriaBuilder.isTrue(root.get("enabled"))
        );

        ResultPaginationResponse res = this.companyService.handleGetAllCompanies(finalSpec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
