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

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"applications"})
@FilterDef(name = "activeResumeFilter")
public class Resume extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long resumeId;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Resume URL is required")
    private String fileUrl;
    private String fileName;
    private long fileSize;
    @Column(columnDefinition = "TEXT")
    private String summary;
    private boolean isDefault = false;
    private boolean isPublic = false;
    private Double aiScore;
    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;
    @Column(columnDefinition = "TEXT")
    private String aiSuggestions;
    private Instant analyzedAt;

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    private Applicant applicant;

    @OneToMany(mappedBy = "resume", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeApplicationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Application> applications = new ArrayList<>();
}
