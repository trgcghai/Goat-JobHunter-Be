package iuh.fit.goat.util;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRole;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageHelper {

    public static String generateSystemMessage(MessageEvent type, User actor, Object... params) {
        String actorName = getDisplayName(actor);

        return switch (type) {
            case MEMBER_ADDED -> {
                String memberName = (String) params[0];
                yield String.format("%s đã thêm %s vào nhóm", actorName, memberName);
            }
            case MEMBER_REMOVED -> {
                String memberName = (String) params[0];
                yield String.format("%s đã xóa %s khỏi nhóm", actorName, memberName);
            }
            case MEMBER_LEFT ->
                    String.format("%s đã rời khỏi nhóm", actorName);
            case ROLE_CHANGED -> {
                String memberName = (String) params[0];
                ChatRole newRole = (ChatRole) params[1];
                String roleText = getRoleText(newRole);
                yield String.format("%s đã thay đổi vai trò của %s thành %s",
                        actorName, memberName, roleText);
            }
            case GROUP_CREATED -> {
                String groupName = (String) params[0];
                yield String.format("%s đã tạo nhóm \"%s\"", actorName, groupName);
            }
            case GROUP_NAME_CHANGED -> {
                String oldName = (String) params[0];
                String newName = (String) params[1];
                yield String.format("%s đã đổi tên nhóm từ \"%s\" thành \"%s\"",
                        actorName, oldName, newName);
            }
            case GROUP_AVATAR_CHANGED ->
                    String.format("%s đã thay đổi ảnh đại diện nhóm", actorName);
        };
    }

    private static String getDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }

    private static String getRoleText(ChatRole role) {
        return switch (role) {
            case OWNER -> "Chủ nhóm";
            case MODERATOR -> "Quản trị viên";
            default -> "Thành viên";
        };
    }
}