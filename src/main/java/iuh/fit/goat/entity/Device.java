package iuh.fit.goat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.FilterDef;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "account")
@FilterDef(name = "activeDeviceFilter")
public class Device extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long deviceId;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
}
