package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.MessageType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"chatRoom", "sender", "readBy"})
@FilterDef(name = "activeMessageFilter")
public class Message extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long messageId;
    @Column(columnDefinition = "TEXT")
    @NotBlank(message = "Content is required")
    private String content;
    @Enumerated(EnumType.STRING)
    private MessageType type;
    private String fileUrl;
    private String fileName;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToMany(fetch = LAZY)
    @JoinTable(
            name = "message_read_by",
            joinColumns = @JoinColumn(name = "message_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnore
    @Filter(
            name = "activeAccountFilter",
            condition = "deleted_at IS NULL"
    )
    private List<User> readBy = new ArrayList<>();
}
