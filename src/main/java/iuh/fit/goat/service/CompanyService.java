package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.entity.Company;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

public interface CompanyService {
    Company handleGetCompanyById(long id);

    ResultPaginationResponse handleGetAllCompanies(Specification<Company>  spec, Pageable pageable);

    Company handleGetCompanyByEmail(String email);

    Map<String, List<String>> handleGroupAddressesCityByCompany(long id);

    CompanyResponse convertToCompanyResponse(Company company);
}
