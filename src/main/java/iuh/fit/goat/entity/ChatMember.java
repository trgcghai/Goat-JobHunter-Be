package iuh.fit.goat.entity;

import iuh.fit.goat.enumeration.ChatRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "chat_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"room", "account"})
@FilterDef(name = "activeChatMemberFilter")
public class ChatMember extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long memberId;
    @Enumerated(EnumType.STRING)
    private ChatRole role;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "last_read_message_sk")
    private String lastReadMessageSk;
}
