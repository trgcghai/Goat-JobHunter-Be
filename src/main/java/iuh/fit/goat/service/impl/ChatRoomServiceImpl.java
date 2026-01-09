package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationResponse getMyChatRooms(Long accountId, Pageable pageable) {
        Page<ChatRoom> chatRoomPage = chatRoomRepository.findChatRoomsByMemberAccountId(accountId, pageable);

        List<ChatRoomResponse> chatRooms = chatRoomPage.getContent().stream()
                .map(this::mapToChatRoomResponse)
                .collect(Collectors.toList());

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(chatRoomPage.getNumber() + 1);
        meta.setPageSize(chatRoomPage.getSize());
        meta.setPages(chatRoomPage.getTotalPages());
        meta.setTotal(chatRoomPage.getTotalElements());

        return new ResultPaginationResponse(meta, chatRooms);
    }

    /**
     * Generate dynamic group name based on member count
     * Rules:
     * - 3 members: "UserA, UserB và UserC"
     * - 4+ members: "UserA, UserB và (X) người khác"
     */
    private String generateGroupName(List<ChatMember> members) {
        List<String> displayNames = members.stream()
                .filter(m -> m.getDeletedAt() == null)
                .map(m -> getDisplayName(m.getUser()))
                .collect(Collectors.toList());

        int totalMembers = displayNames.size();

        if (totalMembers == 3) {
            return String.format("%s, %s và %s",
                    displayNames.get(0),
                    displayNames.get(1),
                    displayNames.get(2));
        } else if (totalMembers > 3) {
            int otherCount = totalMembers - 2;
            return String.format("%s, %s và (%d) người khác",
                    displayNames.get(0),
                    displayNames.get(1),
                    otherCount);
        }

        return String.join(", ", displayNames);
    }

    private String getDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }

    private ChatRoomResponse mapToChatRoomResponse(ChatRoom chatRoom) {
        // Get last message
        Message lastMessage = chatRoom.getMessages().stream()
                .filter(m -> m.getDeletedAt() == null)
                .max(Comparator.comparing(Message::getCreatedAt))
                .orElse(null);

        // Count active members
        int memberCount = (int) chatRoom.getMembers().stream()
                .filter(m -> m.getDeletedAt() == null)
                .count();

        // Generate name dynamically if GROUP type
        String name = chatRoom.getName();
        if (chatRoom.getType() == ChatRoomType.GROUP && (name == null || name.isBlank())) {
            name = generateGroupName(chatRoom.getMembers());
        }

        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getRoomId())
                .type(chatRoom.getType())
                .name(name)
                .avatar(chatRoom.getAvatar())
                .memberCount(memberCount)
                .lastMessagePreview(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageTime(lastMessage != null ? LocalDateTime.from(lastMessage.getCreatedAt()) : null)
                .unreadCount(0) // TODO: Implement unread count logic
                .createdAt(LocalDateTime.from(chatRoom.getCreatedAt()))
                .status(chatRoom.getDeletedAt() == null ? "ACTIVE" : "DELETED")
                .build();
    }
}