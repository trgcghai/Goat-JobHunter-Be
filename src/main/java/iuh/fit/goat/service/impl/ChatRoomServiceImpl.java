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
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
                .sorted((cr1, cr2) -> {
                    // Sort theo lastMessageTime DESC (mới nhất lên đầu)
                    LocalDateTime time1 = cr1.getLastMessageTime();
                    LocalDateTime time2 = cr2.getLastMessageTime();

                    // Null safety: Phòng ở cuối
                    if (time1 == null && time2 == null) return 0;
                    if (time1 == null) return 1;  // cr1 xuống dưới
                    if (time2 == null) return -1; // cr2 xuống dưới

                    // So sánh DESC: mới nhất trước
                    return time2.compareTo(time1);
                })
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
    public ChatRoom createNewSingleChatRoom(User currentUser, MessageToNewChatRoom request) throws InvalidException {
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
            // Send message in existing room instead of creating new one
            this.messageService.sendMessage(
                    existingRoom.get().getRoomId(),
                    new MessageCreateRequest(request.getContent()),
                    currentUser
            );

            // Return existing room
            return existingRoom.orElse(null);
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
        this.messageService.sendMessage(chatRoom.getRoomId(), new MessageCreateRequest(request.getContent()), currentUser);

        return chatRoom;
    }

    @Override
    @Transactional
    public ChatRoom createNewSingleChatRoomWithFiles(
            User currentUser,
            MessageToNewChatRoom request,
            List<MultipartFile> files
    ) throws InvalidException {

        // Validate receiver exists
        User uReceiver = this.userRepository.findById(request.getAccountId()).orElse(null);

        if (uReceiver == null) {
            throw new InvalidException("Receiver not found");
        }

        // Check if direct chat room already exists
        Optional<ChatRoom> existingRoom = findExistingDirectChatRoom(currentUser.getAccountId(), uReceiver.getAccountId());

        if (existingRoom.isPresent()) {
            // Send messages in existing room
            this.messageService.sendMessagesWithFiles(
                    existingRoom.get().getRoomId(),
                    new MessageCreateRequest(request.getContent()),
                    files,
                    currentUser
            );

            return existingRoom.get();
        }

        // Create new chat room
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setType(ChatRoomType.DIRECT);
        chatRoom.setName("Không có tên");
        chatRoom = this.chatRoomRepository.saveAndFlush(chatRoom);

        // Create chat members
        ChatMember sender = new ChatMember();
        sender.setUser(currentUser);
        sender.setRole(ChatRole.OWNER);

        ChatMember receiver = new ChatMember();
        receiver.setUser(uReceiver);
        receiver.setRole(ChatRole.OWNER);

        this.chatMemberRepository.saveAllAndFlush(
                Arrays.asList(sender, receiver));

        // Update chat room with members
        chatRoom.setMembers(new ArrayList<>(
                Arrays.asList(sender, receiver)));
        chatRoom = this.chatRoomRepository.saveAndFlush(chatRoom);

        // Update members with room
        sender.setRoom(chatRoom);
        receiver.setRoom(chatRoom);
        this.chatMemberRepository.saveAllAndFlush(
                Arrays.asList(sender, receiver));

        // Send messages with files
        this.messageService.sendMessagesWithFiles(
                chatRoom.getRoomId(),
                new MessageCreateRequest(request.getContent()),
                files,
                currentUser
        );

        return chatRoom;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoom existsDirectChatRoom(Long currentUserId, Long otherUserId) {
        return findExistingDirectChatRoom(currentUserId, otherUserId).orElse(null);
    }

    @Override
    public List<Message> getMediaMessagesInChatRoom(User user, Long chatRoomId, Pageable pageable) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new InvalidException("Chat room not found"));

        if (!this.isUserInChatRoom(chatRoom, user.getAccountId())) {
            throw new InvalidException("User is not in chat room");
        }

        return this.messageService.getMediaMessagesByChatRoom(chatRoomId, pageable);
    }

    @Override
    public List<Message> getFileMessagesInChatRoom(User user, Long chatRoomId, Pageable pageable) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new InvalidException("Chat room not found"));

        if (!this.isUserInChatRoom(chatRoom, user.getAccountId())) {
            throw new InvalidException("User is not in chat room");
        }

        return this.messageService.getFileMessagesByChatRoom(chatRoomId, pageable);
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
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();

        try {
            Message lastMessage = getLastMessageSafely(chatRoom.getRoomId());
            int memberCount = countActiveMembers(chatRoom);
            String name = resolveChatRoomName(chatRoom, currentUserEmail);
            String avatar = resolveChatRoomAvatar(chatRoom, currentUserEmail);
            LastMessageInfo lastMessageInfo = buildLastMessageInfo(lastMessage, currentUserEmail);

            return ChatRoomResponse.builder()
                    .roomId(chatRoom.getRoomId())
                    .type(chatRoom.getType())
                    .name(name)
                    .avatar(avatar)
                    .memberCount(memberCount)
                    .lastMessagePreview(lastMessageInfo.content())
                    .lastMessageTime(lastMessageInfo.time())
                    .currentUserSentLastMessage(lastMessageInfo.isCurrentUserSender())
                    .build();

        } catch (Exception e) {
            log.error("Error mapping ChatRoom to ChatRoomResponse: {}", e.getMessage(), e);
            return buildFallbackResponse(chatRoom);
        }
    }

    // =============== HELPER METHODS FOR mapToChatRoomResponse ====================

    private record LastMessageInfo(String content, LocalDateTime time, boolean isCurrentUserSender) {}

    private Message getLastMessageSafely(Long chatRoomId) {
        try {
            return messageService.getLastMessageByChatRoom(chatRoomId);
        } catch (InvalidException e) {
            log.warn("Failed to get last message for chatRoom {}: {}", chatRoomId, e.getMessage());
            return null;
        }
    }

    private int countActiveMembers(ChatRoom chatRoom) {
        return (int) chatRoom.getMembers().stream()
                .filter(m -> m.getDeletedAt() == null)
                .count();
    }

    private String resolveChatRoomName(ChatRoom chatRoom, String currentUserEmail) {
        if (chatRoom.getType() == ChatRoomType.DIRECT) {
            return getOtherMemberDisplayName(chatRoom, currentUserEmail);
        }

        if (chatRoom.getType() == ChatRoomType.GROUP) {
            String name = chatRoom.getName();
            if (name == null || name.isBlank()) {
                return generateGroupName(chatRoom.getMembers());
            }
            return name;
        }

        return chatRoom.getName();
    }

    private String resolveChatRoomAvatar(ChatRoom chatRoom, String currentUserEmail) {
        if (chatRoom.getType() != ChatRoomType.DIRECT) {
            return chatRoom.getAvatar();
        }

        return chatRoom.getMembers().stream()
                .filter(m -> isOtherActiveMember(m, currentUserEmail))
                .map(ChatMember::getUser)
                .map(User::getAvatar)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String getOtherMemberDisplayName(ChatRoom chatRoom, String currentUserEmail) {
        return chatRoom.getMembers().stream()
                .filter(m -> isOtherActiveMember(m, currentUserEmail))
                .map(ChatMember::getUser)
                .map(this::getDisplayName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Không có tên");
    }

    private boolean isOtherActiveMember(ChatMember member, String currentUserEmail) {
        return member.getDeletedAt() == null
                && member.getUser() != null
                && !member.getUser().getEmail().equalsIgnoreCase(currentUserEmail);
    }

    private LastMessageInfo buildLastMessageInfo(Message lastMessage, String currentUserEmail) {
        if (lastMessage == null) {
            return new LastMessageInfo(MESSAGE_FALLBACK, null, false);
        }

        String content = resolveMessageContent(lastMessage);
        LocalDateTime time = convertToLocalDateTime(lastMessage.getCreatedAt());
        boolean isCurrentUserSender = isMessageFromCurrentUser(lastMessage, currentUserEmail);

        return new LastMessageInfo(content, time, isCurrentUserSender);
    }

    private String resolveMessageContent(Message message) {
        if (message.getIsHidden()) {
            return MESSAGE_FALLBACK_HDDEN;
        }
        return formatMessageContent(message);
    }

    private boolean isMessageFromCurrentUser(Message message, String currentUserEmail) {
        if (message.getSenderId() == null) {
            return false;
        }

        return userRepository.findById(Long.parseLong(message.getSenderId()))
                .map(sender -> sender.getEmail().equalsIgnoreCase(currentUserEmail))
                .orElse(false);
    }

    private LocalDateTime convertToLocalDateTime(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private ChatRoomResponse buildFallbackResponse(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .roomId(chatRoom.getRoomId())
                .type(chatRoom.getType())
                .name("Không có tên")
                .avatar(chatRoom.getAvatar())
                .memberCount(0)
                .lastMessagePreview(MESSAGE_FALLBACK)
                .currentUserSentLastMessage(false)
                .lastMessageTime(null)
                .build();
    }

    /**
     * Format message content based on type
     */
    private String formatMessageContent(Message message) {
        return switch (message.getMessageType()) {
            case TEXT -> message.getContent() != null ? message.getContent() : MESSAGE_FALLBACK;
            case IMAGE -> "[Hình ảnh]";
            case VIDEO -> "[Video]";
            case FILE -> "[Tệp tin]";
            case AUDIO -> "[Âm thanh]";
            default -> "[Tin nhắn không xác định]";
        };
    }
}