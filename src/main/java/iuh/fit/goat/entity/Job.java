package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import iuh.fit.goat.enumeration.Level;
import iuh.fit.goat.enumeration.WorkingType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"company", "skills", "applications", "career", "users"})
@FilterDef(name = "activeJobFilter")
public class Job extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long jobId;
    @Column(columnDefinition = "TEXT")
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;
    @Enumerated(EnumType.STRING)
    private Level level;
    private int quantity;
    private double salary;
    @NotBlank(message = "Title is not empty")
    private String title;
    @Enumerated(EnumType.STRING)
    private WorkingType workingType;
    private boolean enabled = false;

    @OneToOne(mappedBy = "job", cascade = {PERSIST, MERGE})
    private Address address;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "job_skill",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @JsonIgnoreProperties(value = {"jobs"})
    @Filter(
            name = "activeSkillFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Skill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "job", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeApplicationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Application> applications = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "career_id")
    private Career career;

    @ManyToMany(mappedBy = "savedJobs", fetch = LAZY)
    @JsonIgnore
    @Filter(
            name = "activeAccountFilter",
            condition = "deleted_at IS NULL"
    )
    private List<User> users = new ArrayList<>();
}
