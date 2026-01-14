package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.service.ChatRoomService;
import iuh.fit.goat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final String MESSAGE_FALLBACK = "Không thể tải tin nhắn này.";

    private final ChatRoomRepository chatRoomRepository;
    private final MessageService messageService;

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

    @Override
    public List<Message> getMessagesInChatRoom(User user, Long chatRoomId, Pageable pageable) throws InvalidException {

        // Check if user belong to chat room or not
        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId).orElse(null);

        if (chatRoom == null) {
            throw new InvalidException("Chat room not found");
        }

        if (!this.isUserInChatRoom(chatRoom, user.getAccountId())) {
            throw new InvalidException("User is not in chat room");
        }

        return this.messageService.getMessagesByChatRoom(chatRoomId, pageable);
    }


    // =============== HELPER FUNCTIONS ====================

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

    public boolean isUserInChatRoom(ChatRoom chatRoom, Long accountId) {
        return chatRoom.getMembers()
                .stream()
                .anyMatch(member ->
                        member.getUser() != null &&
                                member.getUser().getAccountId() == accountId
                );
    }

    private ChatRoomResponse mapToChatRoomResponse(ChatRoom chatRoom) {
        // Get last message
        Message lastMessage;
        try {
            lastMessage = messageService.getLastMessageByChatRoom(chatRoom.getRoomId());
        } catch (InvalidException e) {
            e.printStackTrace();
            lastMessage = null;
        }

        // Count active members
        int memberCount = (int) chatRoom.getMembers().stream()
                .filter(m -> m.getDeletedAt() == null)
                .count();

        // Generate name dynamically if GROUP type
        String name = chatRoom.getName();
        if (chatRoom.getType() == ChatRoomType.GROUP && (name == null || name.isBlank())) {
            name = generateGroupName(chatRoom.getMembers());
        }

        // Convert Instant to LocalDateTime if lastMessage exists
        LocalDateTime lastMessageTime = null;
        String lastMessageContent = MESSAGE_FALLBACK;

        if (lastMessage != null && !Boolean.TRUE.equals(lastMessage.getIsHidden())) {
            lastMessageContent = lastMessage.getContent();
            if (lastMessage.getCreatedAt() != null) {
                lastMessageTime = LocalDateTime.ofInstant(
                        lastMessage.getCreatedAt(),
                        ZoneId.systemDefault()
                );
            }
        }

        return ChatRoomResponse.builder()
                .chatRoomId(chatRoom.getRoomId())
                .type(chatRoom.getType())
                .name(name)
                .avatar(chatRoom.getAvatar())
                .memberCount(memberCount)
                .lastMessagePreview(lastMessageContent)
                .lastMessageTime(lastMessageTime)
                .build();
    }
}