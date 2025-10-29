package iuh.fit.goat.entity;

import iuh.fit.goat.common.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long applicationId;
    private String email;
    @NotBlank(message = "Resume is required")
    private String resumeUrl;
    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    private Applicant applicant;

    @PrePersist
    public void handleBeforeCreate(){
//        this.createdAt = Instant.now();
//        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
//                ? SecurityUtil.getCurrentUserLogin().get()
//                : "";
    }
    @PreUpdate
    public void handleBeforeUpdate(){
//        this.updatedAt = Instant.now();
//        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent()
//                ? SecurityUtil.getCurrentUserLogin().get()
//                : "";
    }
}
