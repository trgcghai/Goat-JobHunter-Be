package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"job", "applicant", "resume", "interview"})
@FilterDef(name = "activeApplicationFilter")
public class Application extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long applicationId;
    private String email;
    @Column(columnDefinition = "TEXT")
    private String coverLetter;
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "applicant_id")
    private Applicant applicant;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @OneToOne(mappedBy = "application", cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeInterviewFilter",
            condition = "deleted_at IS NULL"
    )
    private Interview interview;
}
