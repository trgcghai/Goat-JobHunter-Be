package iuh.fit.goat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import iuh.fit.goat.enumeration.ChatRoomType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

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
    private long roomId;
    private String name;
    private String avatar;
    @Enumerated(EnumType.STRING)
    private ChatRoomType type;
    private String aiModel;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeChatMemberFilter",
            condition = "deleted_at IS NULL"
    )
    private List<ChatMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonIgnore
    @Filter(
            name = "activeMessageFilter",
            condition = "deleted_at IS NULL"
    )
    private List<Message> messages = new ArrayList<>();
}
