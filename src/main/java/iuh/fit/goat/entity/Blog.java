package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.entity.embeddable.BlogActivity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"comments", "reactions", "tickets", "notifications"})
@FilterDef(name = "activeBlogFilter")
public class Blog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long blogId;
    @NotBlank(message = "Title is not empty")
    private String title;
    private String banner;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String content;
    @ElementCollection
    private List<String> tags;
    private boolean draft;
    private boolean enabled = false;
    @Embedded
    private BlogActivity activity = new BlogActivity();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User author;

    @OneToMany(mappedBy = "blog", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeCommentFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "blog", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @JsonIgnore
    private List<BlogReaction> reactions = new ArrayList<>();

    @OneToMany(mappedBy = "blog", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeTicketFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Ticket> tickets = new ArrayList<>();

    @OneToMany(mappedBy = "blog", fetch = FetchType.LAZY,  cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeNotificationFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Notification> notifications = new ArrayList<>();
}
