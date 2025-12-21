package iuh.fit.goat.entity;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"account"})
public class Address extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long addressId;
    private String province;
    private String fullAddress;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
}
