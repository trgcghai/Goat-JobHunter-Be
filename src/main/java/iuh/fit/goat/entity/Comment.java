package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"children", "reactions", "tickets", "notifications"})
@FilterDef(name = "activeCommentFilter")
public class Comment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long commentId;
    @NotBlank(message = "comment is not empty")
    @Column(columnDefinition = "TEXT")
    private String comment;
    private boolean reply;
    private boolean pinned = false;

    @ManyToOne
    @JoinColumn(name = "blog_id")
    private Blog blog;

    @ManyToOne
    @JoinColumn(name = "commented_by_id")
    private User commentedBy;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeCommentFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Comment> children = new ArrayList<>();

    @OneToMany(mappedBy = "comment", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @JsonIgnore
    private List<CommentReaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "comment", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeTicketFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "comment", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeNotificationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Notification> notifications = new ArrayList<>();
}
