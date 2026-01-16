package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.request.message.MessageToNewChatRoom;
import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.enumeration.ChatRole;
import iuh.fit.goat.enumeration.ChatRoomType;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.ChatRoomService;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final String MESSAGE_FALLBACK = "Không thể tải tin nhắn này.";
    private final String MESSAGE_FALLBACK_HDDEN = "Tin nhắn đã được ẩn.";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageService messageService;
    private final UserService userService;
    private final UserRepository userRepository;

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

    @Override
    public boolean isUserInChatRoom(ChatRoom chatRoom, Long accountId) {
        return chatRoom.getMembers()
                .stream()
                .anyMatch(member ->
                        member.getUser() != null &&
                                member.getUser().getAccountId() == accountId
                );
    }

    @Override
    public boolean isUserInChatRoom(Long chatRoomId, Long accountId) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId).orElse(null);

        if (chatRoom == null) {
            throw new InvalidException("Chat room not found");
        }

        return this.isUserInChatRoom(chatRoom, accountId);
    }

    @Override
    @Transactional
    public Message createNewSingleChatRoom(User currentUser, MessageToNewChatRoom request) throws InvalidException {
        try {
            // Validate if receiver is valid
            User uReceiver = this.userRepository.findById(request.getAccountId()).orElse(null);
            if (uReceiver == null) {
                throw new InvalidException("Receiver not found");
            }

            // Check if direct chat room already exists between these 2 users
            Optional<ChatRoom> existingRoom = findExistingDirectChatRoom(
                    currentUser.getAccountId(),
                    uReceiver.getAccountId()
            );

            if (existingRoom.isPresent()) {
                // Return message in existing room instead of creating new one
                return this.messageService.sendMessage(
                        existingRoom.get().getRoomId(),
                        new MessageCreateRequest(request.getContent()),
                        currentUser
                );
            }

            // Create and save chat room first (no members yet) to avoid transient reference
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setType(ChatRoomType.DIRECT);
            chatRoom.setName("Không có tên");
            chatRoom = this.chatRoomRepository.saveAndFlush(chatRoom);

            // Since current user is validated, create chat member
            ChatMember sender = new ChatMember();
            sender.setUser(currentUser);
            sender.setRole(ChatRole.OWNER);

            ChatMember receiver = new ChatMember();
            receiver.setUser(uReceiver);
            receiver.setRole(ChatRole.OWNER);

            // Save chat members without room to avoid transient reference
            this.chatMemberRepository.saveAllAndFlush(Arrays.asList(sender, receiver));

            // Update chat room with members and save, use ArrayList to avoid immutable list
            chatRoom.setMembers(new ArrayList<>(Arrays.asList(sender, receiver)));
            chatRoom = this.chatRoomRepository.saveAndFlush(chatRoom);

            // Update chat members with room and save
            sender.setRoom(chatRoom);
            receiver.setRoom(chatRoom);
            this.chatMemberRepository.saveAllAndFlush(Arrays.asList(sender, receiver));

            // Send message
            return this.messageService.sendMessage(chatRoom.getRoomId(), new MessageCreateRequest(request.getContent()), currentUser);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
                .toList();

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

    private Optional<ChatRoom> findExistingDirectChatRoom(Long userId1, Long userId2) {
        return chatRoomRepository.findDirectChatRoomBetweenUsers(userId1, userId2);
    }

    private ChatRoomResponse mapToChatRoomResponse(ChatRoom chatRoom) {
        // Get last message
        Message lastMessage = null;
        try {
            lastMessage = messageService.getLastMessageByChatRoom(chatRoom.getRoomId());
        } catch (InvalidException e) {
            e.printStackTrace();
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

        if (lastMessage != null) {

            if (lastMessage.getIsHidden()) {
                lastMessageContent = MESSAGE_FALLBACK_HDDEN;
            } else {
                lastMessageContent = lastMessage.getContent();
            }

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