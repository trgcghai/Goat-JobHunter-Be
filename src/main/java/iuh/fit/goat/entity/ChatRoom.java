package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.ChatRoomType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"members", "messages"})
@FilterDef(name = "activeChatRoomFilter")
public class ChatRoom extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;
    private String name;
    private String avatar;
    @Enumerated(EnumType.STRING)
    private ChatRoomType type;
    private String aiModel;

    @OneToMany(mappedBy = "room", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeChatMemberFilter",
            condition = "deleted_at IS NULL"
    )
    private List<ChatMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", fetch = LAZY, cascade = {PERSIST, MERGE})
    @JsonIgnore
    @Filter(
            name = "activeMessageFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Message> messages = new ArrayList<>();
}
