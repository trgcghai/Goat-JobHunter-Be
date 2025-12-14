package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.ChatRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.FilterDef;

@Entity
@Table(name = "chat_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"room"})
@FilterDef(name = "activeChatMemberFilter")
public class ChatMember extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long memberId;
    @Enumerated(EnumType.STRING)
    private ChatRole role;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
