package iuh.fit.goat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;

@Entity
@Table(name = "resume_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "resume")
@FilterDef(name = "activeResumeEvaluationFilter")
public class ResumeEvaluation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long resumeEvaluationId;
    private Double score;
    @Column(columnDefinition = "TEXT")
    private String strengths;
    @Column(columnDefinition = "TEXT")
    private String weaknesses;
    @Column(columnDefinition = "TEXT")
    private String missingSkills;
    @Column(columnDefinition = "TEXT")
    private String suggestions;
    private Instant analyzedAt;
    private String aiModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;
}
