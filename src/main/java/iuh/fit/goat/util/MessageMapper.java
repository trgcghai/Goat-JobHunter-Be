package iuh.fit.goat.util;

import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.entity.Message;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message) {
        return toResponse(message, null, null);
    }

    public static MessageResponse toResponse(Message message, MessageResponse.ReplyContext replyContext) {
        return toResponse(message, replyContext, null);
    }

    public static MessageResponse toResponse(
            Message message,
            MessageResponse.ReplyContext replyContext,
            MessageResponse.ContactCardContext contactCardContext
    ) {
        if (message == null) {
            return null;
        }

        MessageResponse response = new MessageResponse();
        response.setMessageId(message.getMessageId());
        response.setChatRoomId(message.getChatRoomId());
        response.setSender(message.getSender());
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setReplyToMessageId(message.getReplyTo());
        response.setReplyContext(replyContext);
        response.setContactCard(contactCardContext);
        response.setIsHidden(message.getIsHidden());
        response.setIsForwarded(Boolean.TRUE.equals(message.getIsForwarded()));
        response.setOriginalMessageId(message.getOriginalMessageId());
        response.setCreatedAt(message.getCreatedAt());
        response.setUpdatedAt(message.getUpdatedAt());
        return response;
    }
}
