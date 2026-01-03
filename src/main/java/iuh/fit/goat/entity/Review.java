package iuh.fit.goat.entity;

import iuh.fit.goat.entity.embeddable.Rating;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Null;
import lombok.*;
import org.hibernate.annotations.FilterDef;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "company_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "company"})
@FilterDef(name = "activeReviewFilter")
public class Review extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long reviewId;
    @Embedded
    private Rating rating;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @Column(columnDefinition = "TEXT")
    private String experience;
    @Column(columnDefinition = "TEXT")
    private String suggestion;
    private boolean recommended;
    private boolean verified = false;
    private boolean enabled = true;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}
