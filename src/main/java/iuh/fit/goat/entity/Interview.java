package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.InterviewStatus;
import iuh.fit.goat.enumeration.InterviewType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "interviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"interviewer", "application"})
@FilterDef(name = "activeInterviewFilter")
public class Interview extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long interviewId;
    @NotNull(message = "Interview date is required")
    private Instant scheduledAt;
    private Integer durationMinutes = 60;
    @Enumerated(EnumType.STRING)
    private InterviewType type;
    @Enumerated(EnumType.STRING)
    private InterviewStatus status;
    private String location;
    private String meetingLink;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(columnDefinition = "TEXT")
    private String feedback;
    private Integer rating;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "interviewer_id")
    private Recruiter interviewer;

    @OneToOne
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;
}
