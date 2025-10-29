package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "recruiters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Recruiter extends User{
    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;
    private String website;

    @OneToMany(mappedBy = "recruiter", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Job> jobs;

    @ManyToMany(mappedBy = "followedRecruiters", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<User> users;
}
