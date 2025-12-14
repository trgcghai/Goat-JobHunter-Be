package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import iuh.fit.goat.enumeration.Gender;
import jakarta.persistence.*;
import lombok.*;
import iuh.fit.goat.util.annotation.RequireAddressIfRecruiter;
import org.hibernate.annotations.Filter;

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
@ToString(callSuper = true, exclude = {
        "sentFriendRequests",
        "receivedFriendRequests",
        "savedJobs",
        "followedCompanies",
        "blogs",
        "comments",
        "actorNotifications",
        "recipientNotifications",
        "memberships",
        "sentMessages",
        "readMessages",
        "blogReactions",
        "commentReactions",
        "reportedTickets",
        "assignedTickets"}
)
@RequireAddressIfRecruiter
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Recruiter.class, name = "recruiter"),
        @JsonSubTypes.Type(value = Applicant.class, name = "applicant"),
})
public abstract class User extends Account {
    protected String address;
    protected String phone;
    protected LocalDate dob;
    protected String fullName;
    @Enumerated(EnumType.STRING)
    protected Gender gender;
    protected String coverPhoto;
    protected String headline;
    @Column(columnDefinition = "TEXT")
    protected String bio;

    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeFriendshipFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Friendship> sentFriendRequests = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeFriendshipFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Friendship> receivedFriendRequests = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_saved_job",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "job_id")
    )
    @JsonIgnore
    @Filter(
            name = "activeJobFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Job> savedJobs = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_followed_company",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "company_id")
    )
    @JsonIgnore
    @Filter(
            name = "activeAccountFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Company> followedCompanies = new ArrayList<>();

    @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeBlogFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Blog> blogs = new ArrayList<>();

    @OneToMany(mappedBy = "commentedBy", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeCommentFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Comment> comments = new ArrayList<>();

    @ManyToMany(mappedBy = "actors", fetch = FetchType.LAZY)
    @JsonIgnore
    @Filter(
            name = "activeNotificationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Notification> actorNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeNotificationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Notification> recipientNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeChatMemberFilter",
            condition = "deleted_at IS NULL"
    )
    private List<ChatMember> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeMessageFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Message> sentMessages = new ArrayList<>();

    @ManyToMany(mappedBy = "readBy", fetch = FetchType.LAZY)
    @JsonIgnore
    @Filter(
            name = "activeMessageFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Message> readMessages = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @JsonIgnore
    private List<BlogReaction> blogReactions = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @JsonIgnore
    private List<CommentReaction> commentReactions = new ArrayList<>();

    @OneToMany(mappedBy = "reporter", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeTicketFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Ticket> reportedTickets = new ArrayList<>();

    @OneToMany(mappedBy = "assignee", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeTicketFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Ticket> assignedTickets = new ArrayList<>();
}
