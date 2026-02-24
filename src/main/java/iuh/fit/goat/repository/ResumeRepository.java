package iuh.fit.goat.repository;

import iuh.fit.goat.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long>, JpaSpecificationExecutor<Resume> {
    Optional<Resume> findByResumeIdAndDeletedAtIsNull(Long resumeId);

    Optional<Resume> findByFileUrlAndDeletedAtIsNull(String resumeUrl);

    @Modifying
    @Query(
        """
        update Resume r
        set r.isDefault = false
        where r.applicant.accountId = :accountId
        and r.isDefault = true
        and r.resumeId <> :resumeId
        and r.deletedAt is null
        """
    )
    void unsetOtherDefaults(Long accountId, Long resumeId);
}
