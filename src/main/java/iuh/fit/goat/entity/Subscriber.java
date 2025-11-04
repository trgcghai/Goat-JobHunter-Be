package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "subscribers")
public class Subscriber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long subscriberId;
    @NotBlank(message = "Name is not empty")
    private String name;
    @NotBlank(message = "Email is not empty")
    private String email;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "subscriber_skill",
            joinColumns = @JoinColumn(name = "subscriber_id"), inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @JsonIgnoreProperties(value = { "subscribers" })
    private List<Skill> skills;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedAt = Instant.now();
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
    }
}
