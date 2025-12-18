package iuh.fit.goat.service;

import iuh.fit.goat.entity.Company;

public interface CompanyService {
    Company handleGetCompanyByEmail(String email);
}
