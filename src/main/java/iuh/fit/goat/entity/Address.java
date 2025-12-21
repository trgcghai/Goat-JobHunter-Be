package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"account", "job"})
public class Address extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long addressId;
    private String province;
    private String fullAddress;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id")
    @JsonIgnore
    private Account account;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "job_id")
    @JsonIgnore
    private Job job;
}
