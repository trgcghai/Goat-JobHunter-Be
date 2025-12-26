package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Company;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>,
        JpaSpecificationExecutor<Company> {

    Optional<Company> findByNameIgnoreCase(String name);

    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.role WHERE c.email = :email")
    Optional<Company> findByEmailWithRole(@Param("email") String email);

}
