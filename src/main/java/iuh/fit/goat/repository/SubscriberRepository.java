package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long>, JpaSpecificationExecutor<Subscriber> {
    Optional<Subscriber> findByEmailAndDeletedAtIsNull(String email);

    List<Subscriber> findByEmailIn(Set<String> emails);
}