package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.entity.Company;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface CompanyService {
    Company handleGetCompanyById(long id);

    ResultPaginationResponse handleGetAllCompanies(Specification<Company>  spec, Pageable pageable);

    Company handleGetCompanyByEmail(String email);

    CompanyResponse convertToCompanyResponse(Company company);
}
