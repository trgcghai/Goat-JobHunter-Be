package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"roles"})
@FilterDef(name = "activePermissionFilter")
public class Permission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long permissionId;
    @NotBlank(message = "API path is not empty")
    private String apiPath;
    @NotBlank(message = "Method is not empty")
    private String method;
    @NotBlank(message = "Module is not empty")
    private String module;
    @NotBlank(message = "Permission name is not empty")
    private String name;

    public Permission (String name, String apiPath, String method, String module) {
        this.name = name;
        this.apiPath = apiPath;
        this.method = method;
        this.module = module;
    }

    @ManyToMany(mappedBy = "permissions", fetch = LAZY)
    @JsonIgnore
    @Filter(
            name = "activeRoleFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Role> roles = new ArrayList<>();
}
