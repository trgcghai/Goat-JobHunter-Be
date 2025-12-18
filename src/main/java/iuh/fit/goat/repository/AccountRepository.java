package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>,
        JpaSpecificationExecutor<Account> {

    Optional<Account> findByEmail(String email);

}
