package iuh.fit.goat.dto.response.chat;

import iuh.fit.goat.enumeration.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    private Long chatMemberId;
    private Long accountId;
    private String fullName;
    private String username;
    private String email;
    private String avatar;
    private ChatRole role;
    private Instant joinedAt;
}