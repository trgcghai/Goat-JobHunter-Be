package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.chat.*;
import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.request.message.MessageToNewChatRoom;
import iuh.fit.goat.dto.response.StorageResponse;
import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.chat.GroupMemberResponse;
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
import iuh.fit.goat.service.StorageService;
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
    private final String MESSAGE_FALLBACK_HIDDEN = "Tin nhắn đã được ẩn.";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageService messageService;
    private final StorageService storageService;
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


    @Override
    @Transactional
    public ChatRoom createGroupChat(User currentUser, CreateGroupChatRequest request) throws InvalidException {
        // Validate all member IDs exist
        List<User> members = validateAndGetUsers(request.getAccountIds());

        // Create group chat room
        ChatRoom groupChatRoom = new ChatRoom();
        groupChatRoom.setType(ChatRoomType.GROUP);
        groupChatRoom.setName(request.getName() != null ? request.getName() : "Nhóm mới");

        StorageResponse storageResponse = storageService.handleUploadFile(request.getAvatar(), "/chatgroup/avatars");
        String avatarUrl = storageResponse.getUrl();

        groupChatRoom.setAvatar(avatarUrl);
        groupChatRoom = chatRoomRepository.saveAndFlush(groupChatRoom);

        // Create chat members
        List<ChatMember> chatMembers = new ArrayList<>();

        // Add current user as OWNER
        ChatMember owner = new ChatMember();
        owner.setUser(currentUser);
        owner.setRole(ChatRole.OWNER);
        owner.setRoom(groupChatRoom);
        chatMembers.add(owner);

        // Add other members as MEMBER
        for (User member : members) {
            if (member.getAccountId() != currentUser.getAccountId()) {
                ChatMember chatMember = new ChatMember();
                chatMember.setUser(member);
                chatMember.setRole(ChatRole.MEMBER);
                chatMember.setRoom(groupChatRoom);
                chatMembers.add(chatMember);
            }
        }

        // Save all members
        chatMemberRepository.saveAllAndFlush(chatMembers);
        groupChatRoom.setMembers(chatMembers);

        log.info("Created group chat: {} with {} members",
                groupChatRoom.getRoomId(), chatMembers.size());

        return groupChatRoom;
    }

    @Override
    @Transactional
    public ChatRoom updateGroupInfo(User currentUser, Long chatRoomId, UpdateGroupInfoRequest request) throws InvalidException {
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        validateGroupChatRoom(chatRoom);

        // Check if user is member and has permission to update
        ChatMember currentMember = getCurrentMemberInChatRoom(chatRoom, currentUser.getAccountId());
        validateModeratorOrOwnerPermission(currentMember, "update group info");

        // Update group info
        if (request.getName() != null) {
            chatRoom.setName(request.getName());
        }
        if (request.getAvatar() != null) {
            StorageResponse storageResponse = storageService.handleUploadFile(request.getAvatar(), "/chatgroup/avatars");
            String avatarUrl = storageResponse.getUrl();
            chatRoom.setAvatar(avatarUrl);
        }

        return chatRoomRepository.save(chatRoom);
    }

    @Override
    @Transactional
    public void leaveGroupChat(User currentUser, Long chatRoomId) throws InvalidException {
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        validateGroupChatRoom(chatRoom);

        ChatMember currentMember = getCurrentMemberInChatRoom(chatRoom, currentUser.getAccountId());

        // OWNER cannot leave unless ownership is transferred
        if (currentMember.getRole() == ChatRole.OWNER) {
            throw new InvalidException("Owner cannot leave group. Please transfer ownership first.");
        }

        // Delete member
        chatMemberRepository.delete(currentMember);

        log.info("User: {} left group chat: {}", currentUser.getAccountId(), chatRoomId);
    }

    @Override
    @Transactional
    public ChatMember addMemberToGroup(User currentUser, Long chatRoomId, AddMemberRequest request) throws InvalidException {
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        validateGroupChatRoom(chatRoom);

        // Check requester permission
        ChatMember requesterMember = getCurrentMemberInChatRoom(chatRoom, currentUser.getAccountId());
        validateModeratorOrOwnerPermission(requesterMember, "add member");

        // Validate target user exists
        User targetUser = userRepository.findById(request.getAccountId())
                .orElseThrow(() -> new InvalidException("User to be added not found"));

        // Check if user is already a member
        boolean isAlreadyMember = chatRoom.getMembers().stream()
                .anyMatch(m -> m.getDeletedAt() == null &&
                        m.getUser().getAccountId() == targetUser.getAccountId());

        if (isAlreadyMember) {
            throw new InvalidException("User is already a member of this group");
        }

        // Create new member
        ChatMember newMember = new ChatMember();
        newMember.setUser(targetUser);
        newMember.setRole(ChatRole.MEMBER);
        newMember.setRoom(chatRoom);

        return chatMemberRepository.save(newMember);
    }

    @Override
    @Transactional
    public void removeMemberFromGroup(User currentUser, Long chatRoomId, Long chatMemberId) throws InvalidException {
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        validateGroupChatRoom(chatRoom);

        // Check requester permission
        ChatMember requesterMember = getCurrentMemberInChatRoom(chatRoom, currentUser.getAccountId());
        validateModeratorOrOwnerPermission(requesterMember, "remove member");

        // Get target member
        ChatMember targetMember = chatMemberRepository.findById(chatMemberId)
                .orElseThrow(() -> new InvalidException("Chat member not found"));

        // Validate target member belongs to this chat room
        if (!targetMember.getRoom().getRoomId().equals(chatRoomId)) {
            throw new InvalidException("Member does not belong to this chat room");
        }

        // Cannot remove yourself via this endpoint
        if (targetMember.getUser().getAccountId() == currentUser.getAccountId()) {
            throw new InvalidException("Cannot remove yourself. Use leave group endpoint instead");
        }

        // MODERATOR cannot remove OWNER
        if (requesterMember.getRole() == ChatRole.MODERATOR && targetMember.getRole() == ChatRole.OWNER) {
            throw new InvalidException("Moderator cannot remove owner");
        }

        // Delete member
        chatMemberRepository.delete(targetMember);

        log.info("Removed member: {} from group: {} by user: {}",
                chatMemberId, chatRoomId, currentUser.getAccountId());
    }

    @Override
    @Transactional
    public ChatMember updateMemberRole(User currentUser, Long chatRoomId, Long chatMemberId, UpdateMemberRoleRequest request) throws InvalidException {
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        validateGroupChatRoom(chatRoom);

        // Check requester permission
        ChatMember requesterMember = getCurrentMemberInChatRoom(chatRoom, currentUser.getAccountId());
        validateModeratorOrOwnerPermission(requesterMember, "update member role");

        // Get target member
        ChatMember targetMember = chatMemberRepository.findById(chatMemberId)
                .orElseThrow(() -> new InvalidException("Chat member not found"));

        // Validate target member belongs to this chat room
        if (!targetMember.getRoom().getRoomId().equals(chatRoomId)) {
            throw new InvalidException("Member does not belong to this chat room");
        }

        // MODERATOR cannot change OWNER role
        if (requesterMember.getRole() == ChatRole.MODERATOR && targetMember.getRole() == ChatRole.OWNER) {
            throw new InvalidException("Moderator cannot change owner role");
        }

        // Prevent removing last OWNER
        if (targetMember.getRole() == ChatRole.OWNER && request.getRole() != ChatRole.OWNER) {
            long ownerCount = chatRoom.getMembers().stream()
                    .filter(m -> m.getDeletedAt() == null && m.getRole() == ChatRole.OWNER)
                    .count();

            if (ownerCount <= 1) {
                throw new InvalidException("Cannot remove the last owner. Assign another owner first");
            }
        }

        // Update role
        targetMember.setRole(request.getRole());
        return chatMemberRepository.save(targetMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(User currentUser, Long chatRoomId) throws InvalidException {
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        validateGroupChatRoom(chatRoom);

        // Check if current user is a member
        getCurrentMemberInChatRoom(chatRoom, currentUser.getAccountId());

        // Fetch active members using repository
        List<ChatMember> members = chatMemberRepository.findByRoomRoomIdAndDeletedAtIsNull(chatRoomId);

        // Map to DTO
        return members.stream()
                .map(this::mapToGroupMemberResponse)
                .toList();
    }

    // =============== HELPER METHODS FOR GROUP CHAT ====================

    private GroupMemberResponse mapToGroupMemberResponse(ChatMember member) {
        User user = member.getUser();
        return GroupMemberResponse.builder()
                .chatMemberId(member.getMemberId())
                .accountId(user.getAccountId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(member.getRole())
                .joinedAt(member.getCreatedAt())
                .build();
    }

    private List<User> validateAndGetUsers(List<Long> accountIds) throws InvalidException {
        List<User> users = userRepository.findAllById(accountIds);

        if (users.size() != accountIds.size()) {
            throw new InvalidException("One or more users not found");
        }

        return users;
    }

    private ChatRoom getChatRoomById(Long chatRoomId) throws InvalidException {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new InvalidException("Chat room not found"));
    }

    private void validateGroupChatRoom(ChatRoom chatRoom) throws InvalidException {
        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new InvalidException("This operation is only available for group chats");
        }
    }

    private ChatMember getCurrentMemberInChatRoom(ChatRoom chatRoom, Long accountId) throws InvalidException {
        return chatRoom.getMembers().stream()
                .filter(m -> m.getDeletedAt() == null &&
                        m.getUser().getAccountId() == accountId)
                .findFirst()
                .orElseThrow(() -> new InvalidException("User is not a member of this chat room"));
    }

    private void validateModeratorOrOwnerPermission(ChatMember member, String action) throws InvalidException {
        if (member.getRole() != ChatRole.OWNER && member.getRole() != ChatRole.MODERATOR) {
            throw new InvalidException("Only owners and moderators can " + action);
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

    private record LastMessageInfo(String content, LocalDateTime time, boolean isCurrentUserSender) {
    }

    private Message getLastMessageSafely(Long chatRoomId) {
        try {
            return messageService.getLastMessageByChatRoom(chatRoomId);
        } catch (InvalidException e) {
            log.debug("No messages found for chatRoom {}: {}", chatRoomId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error getting last message for chatRoom {}: {}", chatRoomId, e.getMessage());
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
            // Trả về tên group ngay cả khi chưa có tin nhắn
            if (name == null || name.isBlank() || "Không có tên".equals(name)) {
                return generateGroupName(chatRoom.getMembers());
            }
            return name;
        }

        return chatRoom.getName() != null ? chatRoom.getName() : "";
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
            return new LastMessageInfo("", null, false);
        }

        String content = resolveMessageContent(lastMessage);
        LocalDateTime time = convertToLocalDateTime(lastMessage.getCreatedAt());
        boolean isCurrentUserSender = isMessageFromCurrentUser(lastMessage, currentUserEmail);

        return new LastMessageInfo(content, time, isCurrentUserSender);
    }

    private String resolveMessageContent(Message message) {
        if (message.getIsHidden()) {
            return MESSAGE_FALLBACK_HIDDEN;
        }
        return formatMessageContent(message);
    }

    private boolean isMessageFromCurrentUser(Message message, String currentUserEmail) {
        return message.getSender().getEmail().equalsIgnoreCase(currentUserEmail);
    }

    private LocalDateTime convertToLocalDateTime(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private ChatRoomResponse buildFallbackResponse(ChatRoom chatRoom) {
        String currentUserEmail = SecurityUtil.getCurrentUserEmail();

        return ChatRoomResponse.builder()
                .roomId(chatRoom.getRoomId())
                .type(chatRoom.getType())
                .name(resolveChatRoomName(chatRoom, currentUserEmail))
                .avatar(resolveChatRoomAvatar(chatRoom, currentUserEmail))
                .memberCount(countActiveMembers(chatRoom))
                .lastMessagePreview("") // Để trống thay vì "Không thể tải tin nhắn này"
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