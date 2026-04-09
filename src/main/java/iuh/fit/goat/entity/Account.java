package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.Visibility;
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
@ToString(exclude = {
        "role", "addresses", "actorNotifications", "recipientNotifications",
        "savedJobs", "savedBlogs", "followedCompanies", "blogs", "comments",
        "blogReactions", "commentReactions", "reportedTickets", "assignedTickets",
        "memberships"
})
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
    @Enumerated(EnumType.STRING)
    protected Visibility visibility = Visibility.PUBLIC;

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

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "account_saved_job",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "job_id")
    )
    @JsonIgnore
    @Filter(
            name = "activeJobFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Job> savedJobs = new ArrayList<>();

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "account_saved_blog",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "blog_id")
    )
    @JsonIgnore
    @Filter(
            name = "activeBlogFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Blog> savedBlogs = new ArrayList<>();

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "account_followed_company",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    @JsonIgnore
    @Filter(
            name = "activeAccountFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Company> followedCompanies = new ArrayList<>();

    @OneToMany(mappedBy = "author", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeBlogFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Blog> blogs = new ArrayList<>();

    @OneToMany(mappedBy = "commentedBy", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeCommentFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "account", fetch = LAZY, cascade = {PERSIST, MERGE}, orphanRemoval = true)
    @JsonIgnore
    private List<BlogReaction> blogReactions = new ArrayList<>();

    @OneToMany(mappedBy = "account", fetch = LAZY, cascade = {PERSIST, MERGE}, orphanRemoval = true)
    @JsonIgnore
    private List<CommentReaction> commentReactions = new ArrayList<>();

    @OneToMany(mappedBy = "reporter", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeTicketFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Ticket> reportedTickets = new ArrayList<>();

    @OneToMany(mappedBy = "assignee", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeTicketFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Ticket> assignedTickets = new ArrayList<>();

    @OneToMany(mappedBy = "account", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeChatMemberFilter",
            condition = "deleted_at IS NULL"
    )
    private List<ChatMember> memberships = new ArrayList<>();

    public void addAddress(Address address) {
        this.addresses.add(address);
        address.setAccount(this);
    }
}

