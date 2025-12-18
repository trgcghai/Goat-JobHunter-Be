package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.Company;
import iuh.fit.goat.repository.CompanyRepository;
import iuh.fit.goat.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyRepository companyRepository;

    @Override
    public Company handleGetCompanyByEmail(String email) {
        return this.companyRepository.findByEmailWithRole(email).orElse(null);
    }
}
