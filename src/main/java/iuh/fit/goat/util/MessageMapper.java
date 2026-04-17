package iuh.fit.goat.util;

import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.embeddable.MediaItem;
import iuh.fit.goat.enumeration.MediaType;
import iuh.fit.goat.enumeration.MessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        response.setMediaItems(mapMediaItems(message));
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

    private static List<MessageResponse.MediaItem> mapMediaItems(Message message) {
        if (message == null) {
            return null;
        }

        List<MessageResponse.MediaItem> mapped = mapEntityMediaItems(message.getMediaItems());
        if (!mapped.isEmpty()) {
            return mapped;
        }

        if (!isLegacySingleMediaMessage(message)) {
            return null;
        }

        MediaType mediaType = mapLegacyMediaType(message.getMessageType());
        if (mediaType == null) {
            return null;
        }

        return Collections.singletonList(
                MessageResponse.MediaItem.builder()
                        .url(message.getContent())
                        .mediaType(mediaType)
                        .mimeType(null)
                        .sizeBytes(null)
                        .displayOrder(0)
                        .build()
        );
    }

    private static List<MessageResponse.MediaItem> mapEntityMediaItems(List<MediaItem> mediaItems) {
        if (mediaItems == null || mediaItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<MessageResponse.MediaItem> mapped = new ArrayList<>();
        for (MediaItem mediaItem : mediaItems) {
            if (mediaItem == null || mediaItem.getUrl() == null || mediaItem.getUrl().isBlank()) {
                continue;
            }

            mapped.add(MessageResponse.MediaItem.builder()
                    .url(mediaItem.getUrl())
                    .mediaType(mediaItem.getMediaType())
                    .mimeType(mediaItem.getMimeType())
                    .sizeBytes(mediaItem.getSizeBytes())
                    .displayOrder(mediaItem.getDisplayOrder())
                    .build());
        }

        return mapped;
    }

    private static boolean isLegacySingleMediaMessage(Message message) {
        if (message.getContent() == null || message.getContent().isBlank()) {
            return false;
        }

        MessageType messageType = message.getMessageType();
        return messageType == MessageType.IMAGE
                || messageType == MessageType.VIDEO
                || messageType == MessageType.AUDIO;
    }

    private static MediaType mapLegacyMediaType(MessageType messageType) {
        if (messageType == null) {
            return null;
        }

        return switch (messageType) {
            case IMAGE -> MediaType.IMAGE;
            case VIDEO -> MediaType.VIDEO;
            case AUDIO -> MediaType.AUDIO;
            default -> null;
        };
    }
}
