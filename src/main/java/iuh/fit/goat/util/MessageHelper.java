package iuh.fit.goat.util;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Company;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRole;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class MessageHelper {

    public static String generateSystemMessage(MessageEvent type, Account actor, Object... params) {
        String actorName = getDisplayName(actor);

        return switch (type) {
            case MEMBER_ADDED -> {
                String memberName = (String) params[0];

                yield String.format("(event:%s) %s đã thêm %s vào nhóm", MessageEvent.MEMBER_ADDED, actorName, memberName);
            }
            case MEMBER_REMOVED -> {
                String memberName = (String) params[0];

                yield String.format("(event:%s) %s đã xóa %s khỏi nhóm", MessageEvent.MEMBER_REMOVED, actorName, memberName);
            }
            case MEMBER_LEFT ->
                    String.format("(event:%s) %s đã rời khỏi nhóm", MessageEvent.MEMBER_LEFT, actorName);
            case ROLE_CHANGED -> {
                String memberName = (String) params[0];
                ChatRole newRole = (ChatRole) params[1];
                String roleText = getRoleText(newRole);

                yield String.format("(event:%s) %s đã thay đổi vai trò của %s thành %s",
                        MessageEvent.ROLE_CHANGED, actorName, memberName, roleText);
            }
            case GROUP_CREATED -> {
                String groupName = (String) params[0];

                yield String.format("(event:%s) %s đã tạo nhóm \"%s\"", MessageEvent.GROUP_CREATED, actorName, groupName);
            }
            case GROUP_NAME_CHANGED -> {
                String oldName = (String) params[0];
                String newName = (String) params[1];

                yield String.format("(event:%s) %s đã đổi tên nhóm từ \"%s\" thành \"%s\"",
                        MessageEvent.GROUP_NAME_CHANGED, actorName, oldName, newName);
            }
            case GROUP_AVATAR_CHANGED ->
                    String.format("(event:%s) %s đã thay đổi ảnh đại diện nhóm", MessageEvent.GROUP_AVATAR_CHANGED, actorName);
            case MESSAGE_PINNED -> {

                log.info("Generating message for MESSAGE_PINNED event with params: {}", params);

                MessageResponse message = (MessageResponse) params[0];
                String messageId = (String) params[1];

                yield String.format("(event:%s) %s đã ghim một tin nhắn: %s (Xem %s)",
                        MessageEvent.MESSAGE_PINNED, actorName, formatMessageContent(message), messageId
                );
            }
            case MESSAGE_UNPINNED -> {
                MessageResponse message = (MessageResponse) params[0];
                String messageId = (String) params[1];

                yield String.format("(event:%s) %s đã bỏ ghim một tin nhắn: %s (Xem %s)",
                        MessageEvent.MESSAGE_UNPINNED, actorName, formatMessageContent(message), messageId
                );
            }
        };
    }

    private static String getDisplayName(Account account) {
        String fullName = account instanceof Company ? ((Company) account).getName()
                : ((User) account).getFullName();

        if (!fullName.isEmpty()) return fullName;

        return account.getUsername();
    }

    private static String getRoleText(ChatRole role) {
        return switch (role) {
            case OWNER -> "Chủ nhóm";
            case MODERATOR -> "Quản trị viên";
            default -> "Thành viên";
        };
    }

    private static String formatMessageContent(MessageResponse message) {
        String MESSAGE_FALLBACK = "Không thể tải tin nhắn này.";
        return switch (message.getMessageType()) {
            case TEXT -> message.getContent() != null ? message.getContent() : MESSAGE_FALLBACK;
            case IMAGE -> "[Hình ảnh]";
            case VIDEO -> "[Video]";
            case FILE -> "[Tệp tin]";
            case AUDIO -> "[Âm thanh]";
            case CONTACT_CARD -> "[Danh thiếp]";
            default -> "[Tin nhắn không xác định]";
        };
    }
}