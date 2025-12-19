package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.repository.CompanyRepository;
import iuh.fit.goat.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;

    @Override
    public Company handleGetCompanyById(long id) {
        return this.companyRepository.findById(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllCompanies(Specification<Company> spec, Pageable pageable) {
        Page<Company> page = this.companyRepository.findAll(spec, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<CompanyResponse> companies = page.getContent().stream().map(this :: convertToCompanyResponse).toList();

        return new ResultPaginationResponse(meta, companies);
    }

    @Override
    public Company handleGetCompanyByEmail(String email) {
        return this.companyRepository.findByEmailWithRole(email).orElse(null);
    }

    @Override
    public CompanyResponse convertToCompanyResponse(Company company) {
        CompanyResponse companyResponse = new CompanyResponse();

        companyResponse.setEmail(company.getEmail());
        companyResponse.setName(company.getName());
        companyResponse.setDescription(company.getDescription());
        companyResponse.setLogo(company.getLogo());
        companyResponse.setCoverPhoto(company.getCoverPhoto());
        companyResponse.setWebsite(company.getWebsite());
        companyResponse.setAddress(company.getAddress());
        companyResponse.setPhone(company.getPhone());
        companyResponse.setSize(company.getSize());
        companyResponse.setVerified(company.isVerified());
        companyResponse.setCreatedAt(company.getCreatedAt());
        companyResponse.setUpdatedAt(company.getUpdatedAt());

        return companyResponse;
    }
}
