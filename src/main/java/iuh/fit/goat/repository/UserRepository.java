package iuh.fit.goat.repository;

import iuh.fit.goat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByContact_Email(String email);
    boolean existsByContact_Email(String email);
    User findByRefreshTokenAndContact_Email(String refreshToken, String email);
}
