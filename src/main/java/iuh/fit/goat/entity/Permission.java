package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Permission {
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

    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    public Permission (String name, String apiPath, String method, String module) {
        this.name = name;
        this.apiPath = apiPath;
        this.method = method;
        this.module = module;
    }

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Role> roles;

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
