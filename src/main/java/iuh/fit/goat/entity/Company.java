package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.CompanySize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, exclude = {"jobs", "recruiters", "followers"})
public class Company extends Account {
    @NotBlank(message = "Company name is required")
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String logo;
    private String website;
    private String address;
    private String phone;
    @Enumerated(EnumType.STRING)
    private CompanySize size;
    private boolean verified = false;

    @OneToMany(mappedBy = "company", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeJobFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Job> jobs = new ArrayList<>();

    @OneToMany(mappedBy = "company", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeAccountFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Recruiter> recruiters = new ArrayList<>();

    @ManyToMany(mappedBy = "followedCompanies", fetch = LAZY)
    @JsonIgnore
    @Filter(
            name = "activeAccountFilter",
            condition = "deleted_at IS NULL"
    )
    private List<User> followers = new ArrayList<>();
}
