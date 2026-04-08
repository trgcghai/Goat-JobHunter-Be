package iuh.fit.goat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.goat.dto.request.company.CompanyUpdateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.company.CompanyResponse;
import iuh.fit.goat.dto.response.user.UserResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AddressRepository;
import iuh.fit.goat.repository.CompanyRepository;
import iuh.fit.goat.service.CompanyService;
import iuh.fit.goat.service.ProfileRealtimeService;
import iuh.fit.goat.service.RoleService;
import iuh.fit.goat.service.StorageService;
import iuh.fit.goat.util.BasicUtil;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final RoleService roleService;
    private final StorageService storageService;
    private final ProfileRealtimeService profileRealtimeService;

    private final CompanyRepository companyRepository;
    private final AddressRepository addressRepository;

    private final ObjectMapper mapper;
    private final String COMPANY = "COMPANY";

    @Override
    public Company handleCreateCompany(Company company) {
        Role role;
        if(company.getRole() != null) {
            role = this.roleService.handleGetRoleById(company.getRole().getRoleId());
        } else {
            role = this.roleService.handleGetRoleByName(COMPANY);
        }
        company.setRole(role);
        company.setEnabled(false);

        return this.companyRepository.save(company);
    }

    @Override
    public Company handleUpdateCompany(CompanyUpdateRequest request) throws InvalidException {
        Company company = this.handleGetCompanyById(request.getAccountId());
        if(company == null) return null;

        if(request.getUsername() != null) company.setUsername(request.getUsername());
        if(request.getName() != null) company.setName(request.getName());
        if(request.getDescription() != null) company.setDescription(request.getDescription());
        if(request.getWebsite() != null) company.setWebsite(request.getWebsite());
        if(request.getPhone() != null) company.setPhone(request.getPhone());
        if(request.getSize() != null) company.setSize(request.getSize());
        if(request.getCountry() != null) company.setCountry(request.getCountry());
        if(request.getIndustry() != null) company.setIndustry(request.getIndustry());
        if(request.getWorkingDays() != null) company.setWorkingDays(request.getWorkingDays());
        if(request.getOvertimePolicy() != null) company.setOvertimePolicy(request.getOvertimePolicy());

        if(request.getLogo() != null) {
            String logoUrl = BasicUtil.uploadImage(request.getLogo(), "company-logos", this.storageService);

            if(company.getLogo() != null) {
                String oldKey = company.getLogo().split(".amazonaws.com/")[1];
                this.storageService.handleDeleteFile(oldKey);
            }

            company.setLogo(logoUrl);
        }

        if(request.getCoverPhoto() != null) {
            String coverPhotoUrl = BasicUtil.uploadImage(request.getCoverPhoto(), "company-cover-photos", storageService);

            if(company.getCoverPhoto() != null) {
                String oldKey = company.getCoverPhoto().split(".amazonaws.com/")[1];
                this.storageService.handleDeleteFile(oldKey);
            }

            company.setCoverPhoto(coverPhotoUrl);
        }

        if(request.getAddresses() != null) {
            List<Address> addressList;
            try {
                addressList = this.mapper.readValue(
                        request.getAddresses(),
                        new TypeReference<List<Address>>() {}
                );
            } catch (JsonProcessingException e) {
                throw new InvalidException("Invalid address format");
            }

            List<Address> addresses = company.getAddresses();
            Map<Long, Address> currentMap = addresses.stream()
                    .collect(Collectors.toMap(Address::getAddressId, Function.identity()));

            Set<Long> requestIds = addressList.stream()
                    .map(Address::getAddressId)
                    .filter(id -> id > 0)
                    .collect(Collectors.toSet());

            /* ================= DELETE ================= */
            Iterator<Address> iterator = addresses.iterator();
            while (iterator.hasNext()) {
                Address addr = iterator.next();
                if (!requestIds.contains(addr.getAddressId())) {
                    iterator.remove();
                    this.addressRepository.delete(addr);
                }
            }

            /* ================= UPDATE & CREATE ================= */
            for (Address reqAddr : addressList) {
                // ===== UPDATE =====
                if (reqAddr.getAddressId() > 0 && currentMap.containsKey(reqAddr.getAddressId())) {
                    Address currentAddr = currentMap.get(reqAddr.getAddressId());

                    if (!Objects.equals(currentAddr.getProvince(), reqAddr.getProvince()) ||
                            !Objects.equals(currentAddr.getFullAddress(), reqAddr.getFullAddress())) {
                        currentAddr.setProvince(reqAddr.getProvince());
                        currentAddr.setFullAddress(reqAddr.getFullAddress());
                    }
                }

                // ===== CREATE =====
                else if (reqAddr.getProvince() != null && reqAddr.getFullAddress() != null) {
                    Address newAddress = new Address();
                    newAddress.setProvince(reqAddr.getProvince());
                    newAddress.setFullAddress(reqAddr.getFullAddress());
                    newAddress.setAccount(company);
                    addresses.add(newAddress);
                }
            }
        }

        Company savedCompany = this.companyRepository.save(company);
        Company latestCompany = this.companyRepository.findById(savedCompany.getAccountId()).orElse(savedCompany);

        String currentEmail = SecurityUtil.getCurrentUserEmail();
        if (currentEmail != null && !currentEmail.isBlank()) {
            CompanyResponse payload = this.convertToCompanyResponse(latestCompany);
            this.profileRealtimeService.emitUserProfileUpdated(currentEmail, "COMPANY", payload);
        }

        return latestCompany;
    }

    @Transactional
    @Override
    public void handleDeleteCompany(long id) {
        Company company = this.handleGetCompanyById(id);
        if(company == null) return;

        softDeleteCompanyRelations(id);
        company.onDelete();

        this.companyRepository.save(company);
    }

    @Override
    public Company handleGetCompanyById(long id) {
        return this.companyRepository.findByAccountIdAndDeletedAtIsNull(id).orElse(null);
    }

    @Override
    public Company handleGetCompanyByName(String name) {
        return this.companyRepository.findByNameIgnoreCaseAndDeletedAtIsNull(name).orElse(null);
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

    @Override
    public Map<Long, String> handleFindDistinctSkillsByCompany(long id) {
        List<Object[]> results = this.companyRepository.findDistinctSkillsByCompanyId(id);
        return results.stream()
                .collect(
                        Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (String) row[1]
                        )
                );
    }

    @Override
    public List<String> handleGetAllCompanyNames() {
        return this.companyRepository.getAllCompanyNames();
    }

    @Override
    public CompanyResponse convertToCompanyResponse(Company company) {
        CompanyResponse companyResponse = new CompanyResponse();

        companyResponse.setAccountId(company.getAccountId());
        companyResponse.setEmail(company.getEmail());
        companyResponse.setName(company.getName());
        companyResponse.setUsername(company.getUsername());
        companyResponse.setDescription(company.getDescription());
        companyResponse.setLogo(company.getLogo());
        companyResponse.setCoverPhoto(company.getCoverPhoto());
        companyResponse.setWebsite(company.getWebsite());
        companyResponse.setPhone(company.getPhone());
        companyResponse.setSize(company.getSize());
        companyResponse.setVerified(company.isVerified());
        companyResponse.setCountry(company.getCountry());
        companyResponse.setIndustry(company.getIndustry());
        companyResponse.setWorkingDays(company.getWorkingDays());
        companyResponse.setOvertimePolicy(company.getOvertimePolicy());
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

        if(company.getAwards() != null) {
            List<CompanyResponse.CompanyAward> awards = company.getAwards()
                    .stream()
                    .sorted(Comparator.comparing(CompanyAward :: getCreatedAt).reversed())
                    .map(award -> {
                        CompanyResponse.CompanyAward companyAward = new CompanyResponse.CompanyAward();
                        companyAward.setCompanyAwardId(award.getCompanyAwardId());
                        companyAward.setType(award.getType().getValue());
                        companyAward.setYear(award.getYear());
                        return companyAward;
                    })
                    .toList();
            companyResponse.setAwards(awards);
        }

        if(company.getRole() != null) {
            CompanyResponse.RoleAccount roleAccount = new CompanyResponse.RoleAccount();
            roleAccount.setRoleId(company.getRole().getRoleId());
            roleAccount.setName(company.getRole().getName());
            companyResponse.setRole(roleAccount);
        }

        return companyResponse;
    }

    private void softDeleteCompanyRelations(long id) {
        this.companyRepository.softDeleteAddresses(id);
        this.companyRepository.softDeleteRecipientNotifications(id);
        this.companyRepository.softDeleteJobs(id);
        this.companyRepository.softDeleteRecruiters(id);
        this.companyRepository.softDeleteReviews(id);
        this.companyRepository.softDeleteAwards(id);
    }
}
