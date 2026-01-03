package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.RatingType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "company_awards",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"company_id", "award_type", "year"}
        )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompanyAward extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long companyAwardId;
    @Enumerated(EnumType.STRING)
    @Column(name = "award_type", nullable = false)
    private RatingType type;
    private int year;
    private double average;
    private long totalReviews;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
}
