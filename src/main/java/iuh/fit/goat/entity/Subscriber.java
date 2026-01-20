package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "subscribers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"skills"})
@FilterDef(name = "activeSubscriberFilter")
public class Subscriber extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long subscriberId;
    @NotBlank(message = "Name is not empty")
    private String name;
    @NotBlank(message = "Email is not empty")
    private String email;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "subscriber_skill",
            joinColumns = @JoinColumn(name = "subscriber_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    @JsonIgnoreProperties(value = { "subscribers" })
    @Filter(
            name = "activeSkillFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Skill> skills = new ArrayList<>();
}
