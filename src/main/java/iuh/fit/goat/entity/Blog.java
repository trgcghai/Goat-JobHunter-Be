package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.entity.embeddable.BlogActivity;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Blog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long blogId;
    @NotBlank(message = "Title is not empty")
    private String title;
    private String banner;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;
    @ElementCollection
    @Column(columnDefinition = "MEDIUMTEXT")
    private List<String> content;
    @ElementCollection
    private List<String> tags;
    private boolean draft;
    private boolean enabled;
    @Embedded
    private BlogActivity activity = new BlogActivity();
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User author;

    @OneToMany(mappedBy = "blog", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<Comment> comments;

    @OneToMany(mappedBy = "blog", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<Notification> notifications;

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
