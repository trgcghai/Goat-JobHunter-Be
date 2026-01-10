package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.Education;
import iuh.fit.goat.enumeration.Level;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "applicants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"applications", "resumes"})
public class Applicant extends User{
    private boolean availableStatus = true;
    @Enumerated(EnumType.STRING)
    private Education education;
    @Enumerated(EnumType.STRING)
    private Level level;

    @OneToMany(mappedBy = "applicant", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeApplicationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Application> applications = new ArrayList<>();

    @OneToMany(mappedBy = "applicant", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeResumeFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Resume> resumes = new ArrayList<>();
}
