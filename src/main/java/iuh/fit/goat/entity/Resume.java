package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.*;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"applicant", "applications"})
@FilterDef(name = "activeResumeFilter")
public class Resume extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long resumeId;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Resume URL is required")
    private String fileUrl;
    private String fileName;
    private long fileSize;
    private boolean isDefault = false;
    private boolean isPublic = false;
    private Double aiScore;
    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;
    @Column(columnDefinition = "TEXT")
    private String aiSuggestions;
    private Instant analyzedAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "applicant_id")
    private Applicant applicant;

    @OneToMany(mappedBy = "resume", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeApplicationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Application> applications = new ArrayList<>();
}
