package iuh.fit.goat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.FilterDef;

@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
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

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
