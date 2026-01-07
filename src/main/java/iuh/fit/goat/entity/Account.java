package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_account_email", columnList = "email")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"role", "addresses"})
@FilterDef(name = "activeAccountFilter")
public abstract class Account extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long accountId;
    @NotBlank(message = "Username is required")
    protected String username;
    @NotBlank(message = "Email is required")
    protected String email;
    @NotBlank(message = "Password is required")
    protected String password;
    protected String avatar;
    protected boolean enabled = false;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "account", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    private List<Address> addresses = new ArrayList<>();

    @ManyToMany(mappedBy = "actors", fetch = LAZY)
    @JsonIgnore
    @Filter(
            name = "activeNotificationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Notification> actorNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "recipient", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeNotificationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Notification> recipientNotifications = new ArrayList<>();

    public void addAddress(Address address) {
        this.addresses.add(address);
        address.setAccount(this);
    }
}
