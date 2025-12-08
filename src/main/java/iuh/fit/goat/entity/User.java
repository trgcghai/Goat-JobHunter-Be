package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import iuh.fit.goat.enumeration.Gender;
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
import java.util.ArrayList;
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
    protected String username;
    protected String avatar;
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
    private List<Job> savedJobs = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_liked_blog",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "blog_id")
    )
    @JsonIgnore
    @ToString.Exclude
    private List<Blog> likedBlogs = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_followed_recruiter",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "recruiter_id")
    )
    @JsonIgnore
    @ToString.Exclude
    private List<Recruiter> followedRecruiters = new ArrayList<>();

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Blog> blogs = new ArrayList<>();

    @OneToMany(mappedBy = "commentedBy", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Comment> comments = new ArrayList<>();

    @ManyToMany(mappedBy = "actors", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<Notification> actorNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Notification> recipientNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<Conversation> conversations = new ArrayList<>();

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
