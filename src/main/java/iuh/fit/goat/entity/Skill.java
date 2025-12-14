package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"jobs", "subscribers"})
@FilterDef(name = "activeSkillFilter")
public class Skill extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long skillId;
    @NotBlank(message = "Skill name is not empty")
    private String name;

    public Skill(String name) {
        this.name = name;
    }

    @ManyToMany(mappedBy = "skills", fetch = FetchType.LAZY)
    @JsonIgnore
    @Filter(
            name = "activeJobFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Job> jobs = new ArrayList<>();

    @ManyToMany(mappedBy = "skills", fetch = FetchType.LAZY)
    @JsonIgnore
    @Filter(
            name = "activeSubscriberFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Subscriber> subscribers = new ArrayList<>();
}
