package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import iuh.fit.goat.enumeration.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.LAZY;

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
        "relationshipsAsLowUser",
        "relationshipsAsHighUser",
        "blockedRelationships",
        "reviews"}
)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Recruiter.class, name = "recruiter"),
        @JsonSubTypes.Type(value = Applicant.class, name = "applicant"),
})
public class User extends Account {
    protected String phone;
    protected LocalDate dob;
    protected String fullName;
    @Enumerated(EnumType.STRING)
    protected Gender gender;
    protected String coverPhoto;
    protected String headline;
    @Column(columnDefinition = "TEXT")
    protected String bio;

    @OneToMany(mappedBy = "sender", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeFriendRequestFilter",
            condition = "deleted_at IS NULL"
    )
    private List<FriendRequest> sentFriendRequests = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeFriendRequestFilter",
            condition = "deleted_at IS NULL"
    )
    private List<FriendRequest> receivedFriendRequests = new ArrayList<>();

    @OneToMany(mappedBy = "pairLowUser", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeUserRelationshipFilter",
            condition = "deleted_at IS NULL"
    )
    private List<UserRelationship> relationshipsAsLowUser = new ArrayList<>();

    @OneToMany(mappedBy = "pairHighUser", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeUserRelationshipFilter",
            condition = "deleted_at IS NULL"
    )
    private List<UserRelationship> relationshipsAsHighUser = new ArrayList<>();

    @OneToMany(mappedBy = "blockedBy", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeUserRelationshipFilter",
            condition = "deleted_at IS NULL"
    )
    private List<UserRelationship> blockedRelationships = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeReviewFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Review> reviews = new ArrayList<>();
}
