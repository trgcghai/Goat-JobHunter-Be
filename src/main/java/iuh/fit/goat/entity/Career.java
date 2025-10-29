package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "careers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Career {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long careerId;
    @NotBlank(message = "Career name is not empty")
    private String name;

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    @OneToMany(mappedBy = "career", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Job> jobs;

    public Career(String name) {
        this.name = name;
    }

    @PrePersist
    public void handleBeforeCreate(){
//        this.createdAt = Instant.now();
//        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
//                ? SecurityUtil.getCurrentUserLogin().get()
//                : "";
    }
    @PreUpdate
    public void handleBeforeUpdate(){
//        this.updatedAt = Instant.now();
//        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent()
//                ? SecurityUtil.getCurrentUserLogin().get()
//                : "";
    }
}
