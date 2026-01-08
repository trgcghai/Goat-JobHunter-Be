package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.chat.CreateGroupChatRequest;
import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatRoomResponse createGroupChatRoom(CreateGroupChatRequest request, Long ownerAccountId) {
        // Remove duplicates and owner from member list
        Set<Long> uniqueMemberIds = new LinkedHashSet<>(request.getMemberAccountIds());
        uniqueMemberIds.remove(ownerAccountId);

        if (uniqueMemberIds.size() < 2) {
            throw new IllegalArgumentException("Group chat requires at least 2 members besides owner");
        }

        // Fetch owner
        User ownerAccount = userRepository.findById(ownerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Owner account not found"));

        // Fetch members
        List<User> memberAccounts = userRepository.findByAccountIdIn(new ArrayList<>(uniqueMemberIds));

        if (memberAccounts.size() != uniqueMemberIds.size()) {
            throw new IllegalArgumentException("Some member accounts not found");
        }

        // Create ChatRoom
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setType(ChatRoomType.GROUP);
        chatRoom.setAvatar(null); // Always null for group chats

        // Create owner member
        ChatMember ownerMember = new ChatMember();
        ownerMember.setUser(ownerAccount);
        ownerMember.setRoom(chatRoom);
        ownerMember.setRole(ChatRole.OWNER);
        chatRoom.getMembers().add(ownerMember);

        // Create regular members (preserve insertion order)
        for (User memberAccount : memberAccounts) {
            ChatMember chatMember = new ChatMember();
            chatMember.setUser(memberAccount);
            chatMember.setRoom(chatRoom);
            chatMember.setRole(ChatRole.MEMBER);
            chatRoom.getMembers().add(chatMember);
        }

        // Generate and set group name
        String groupName = generateGroupName(chatRoom.getMembers());
        chatRoom.setName(groupName);

        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

        return mapToChatRoomResponse(savedRoom);
    }

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
                .sorted(Comparator.comparing(ChatMember::getCreatedAt))
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

        // Fallback for less than 3 members (shouldn't happen in group chat)
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