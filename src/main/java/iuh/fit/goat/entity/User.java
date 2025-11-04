package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import iuh.fit.goat.common.Gender;
import iuh.fit.goat.entity.embeddable.Contact;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import iuh.fit.goat.util.annotation.RequireAddressIfRecruiter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@RequireAddressIfRecruiter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Recruiter.class, name = "recruiter"),
        @JsonSubTypes.Type(value = Applicant.class, name = "applicant")
})
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected long userId;
    protected String address;
    @Embedded
    @Valid
    @NotNull(message = "Contact is not empty")
    protected Contact contact;
    protected LocalDate dob;
    protected String fullName;
    @Enumerated(EnumType.STRING)
    protected Gender gender;
    @NotBlank(message = "Password is not empty")
    protected String password;
    @Column(columnDefinition = "MEDIUMTEXT")
    protected String refreshToken;
    protected String username;
    protected String avatar;
    protected String verificationCode;
    protected LocalDateTime verificationCodeExpiresAt;
    protected boolean enabled;


    protected Instant createdAt;
    protected String createdBy;
    protected Instant updatedAt;
    protected String updatedBy;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_saved_job",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "job_id")
    )
    @JsonIgnore
    @ToString.Exclude
    private List<Job> savedJobs;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_followed_recruiter",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "recruiter_id")
    )
    @JsonIgnore
    @ToString.Exclude
    private List<Recruiter> followedRecruiters;

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<Blog> blogs;

    @OneToMany(mappedBy = "commentedBy", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<Comment> comments;

    @OneToMany(mappedBy = "actor", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<Notification> actorNotifications;

    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<Notification> recipientNotifications;

    @PrePersist
    public void handleBeforeCreate(){
        this.createdAt = Instant.now();
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
    }
    @PreUpdate
    public void handleBeforeUpdate(){
        this.updatedAt = Instant.now();
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
    }
}
