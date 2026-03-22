package iuh.fit.goat.util;

import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.entity.Message;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message) {
        if (message == null) {
            return null;
        }

        return new MessageResponse(
                message.getMessageId(),
                message.getChatRoomId(),
                message.getSender(),
                message.getContent(),
                message.getMessageType(),
                message.getReplyTo(),
                message.getIsHidden(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}
