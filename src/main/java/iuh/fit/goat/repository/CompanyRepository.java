package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Company;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>,
        JpaSpecificationExecutor<Company> {

    Optional<Company> findByNameIgnoreCase(String name);

    List<Company> findByAccountIdIn(List<Long> accountIds);

    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.role WHERE c.email = :email")
    Optional<Company> findByEmailWithRole(@Param("email") String email);

    @Query(
        """
        SELECT DISTINCT s.skillId, s.name
        FROM Company c
        JOIN c.jobs j
        JOIN j.skills s
        WHERE c.accountId = :companyId
        """
    )
    List<Object[]> findDistinctSkillsByCompanyId(@Param("companyId") Long companyId);

}
