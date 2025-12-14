package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "recruiters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"conductedInterviews"})
public class Recruiter extends User{
    private String position;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @OneToMany(mappedBy = "interviewer", fetch = LAZY)
    @JsonIgnore
    @Filter(
            name = "activeInterviewFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Interview> conductedInterviews = new ArrayList<>();
}
