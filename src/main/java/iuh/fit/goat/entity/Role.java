package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"permissions", "accounts"})
@FilterDef(name = "activeRoleFilter")
public class Role extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long roleId;
    @Column(columnDefinition = "TEXT")
    private String description;
    private boolean active;
    @NotBlank(message = "Role name is not empty")
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @JsonIgnoreProperties(value = {"roles"})
    @Filter(
            name = "activePermissionFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Permission> permissions = new ArrayList<>();

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeAccountFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Account> accounts = new ArrayList<>();
}
