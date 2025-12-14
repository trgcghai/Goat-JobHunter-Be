package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "careers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"jobs"})
@FilterDef(name = "activeCareerFilter")
public class Career extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long careerId;
    @NotBlank(message = "Career name is not empty")
    private String name;

    public Career(String name) {
        this.name = name;
    }

    @OneToMany(mappedBy = "career", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeJobFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Job> jobs = new ArrayList<>();
}
