package iuh.fit.goat.entity;

import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@MappedSuperclass
@Getter
public abstract class BaseEntity {
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private Instant deletedAt;
    private String deletedBy;

    @PrePersist
    public void onCreate(){
        this.createdAt = Instant.now();
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "System";

        if (this instanceof Job job && job.getEndDate() != null && job.getEndDate().isBefore(LocalDate.now())) {
            job.setActive(false);
        }
    }

    @PreUpdate
    public void onUpdate(){
        this.updatedAt = Instant.now();
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "System";

        if (this instanceof Job job && job.getEndDate() != null && job.getEndDate().isBefore(LocalDate.now())) {
            job.setActive(false);
        }
    }

    public void onDelete() {
        this.deletedAt = Instant.now();
        this.deletedBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "System";
    }
}
