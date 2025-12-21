package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.entity.Address;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.repository.CompanyRepository;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final AiService aiService;
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

        companyResponse.setAccountId(company.getAccountId());
        companyResponse.setEmail(company.getEmail());
        companyResponse.setName(company.getName());
        companyResponse.setDescription(company.getDescription());
        companyResponse.setLogo(company.getLogo());
        companyResponse.setCoverPhoto(company.getCoverPhoto());
        companyResponse.setWebsite(company.getWebsite());
        companyResponse.setPhone(company.getPhone());
        companyResponse.setSize(company.getSize());
        companyResponse.setVerified(company.isVerified());
        companyResponse.setCreatedAt(company.getCreatedAt());
        companyResponse.setUpdatedAt(company.getUpdatedAt());

        if(company.getAddresses() != null) {
            List<CompanyResponse.CompanyAddress> addresses = company.getAddresses()
                    .stream()
                    .map(addr -> {
                        CompanyResponse.CompanyAddress companyAddress = new CompanyResponse.CompanyAddress();
                        companyAddress.setAddressId(addr.getAddressId());
                        companyAddress.setProvince(addr.getProvince());
                        companyAddress.setFullAddress(addr.getFullAddress());
                        return companyAddress;
                    })
                    .toList();
            companyResponse.setAddresses(addresses);
        }

        return companyResponse;
    }

    @Override
    public Map<String, List<String>> handleGroupAddressesCityByCompany(long id) {
        Company company = this.handleGetCompanyById(id);
        if(company == null) return Map.of();

        return company.getAddresses()
                .stream()
                .filter(addr -> addr.getProvince() != null && addr.getFullAddress() != null)
                .collect(
                        Collectors.groupingBy(
                            Address::getProvince,
                            Collectors.mapping(Address::getFullAddress, Collectors.toList())
                        )
                );
    }
}
