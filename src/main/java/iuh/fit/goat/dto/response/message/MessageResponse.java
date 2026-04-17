package iuh.fit.goat.dto.response.message;

import iuh.fit.goat.entity.embeddable.SenderInfo;
import iuh.fit.goat.enumeration.MessageType;
import iuh.fit.goat.enumeration.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String messageId;
    private String chatRoomId;
    private SenderInfo sender;
    private String content;
    private MessageType messageType;
    private String replyToMessageId;
    private ReplyContext replyContext;
    private ContactCardContext contactCard;
    private Boolean isHidden;
    private Boolean isForwarded;
    private String originalMessageId;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplyContext {
        private String originalMessageId;
        private SenderInfo originalSender;
        private MessageType originalMessageType;
        private String originalContentPreview;
        private Boolean originalMessageUnavailable;
        private Boolean originalMessageHidden;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactCardContext {
        private Long accountId;
        private String fullName;
        private String username;
        private String avatar;
        private String headline;
        private String bio;
        private String coverPhoto;
        private Visibility visibility;
    }
}
