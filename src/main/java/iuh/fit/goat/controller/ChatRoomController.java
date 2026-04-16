package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.chat.*;
import iuh.fit.goat.dto.request.message.ContactCardMessageRequest;
import iuh.fit.goat.dto.request.message.ForwardMessageRequest;
import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.request.message.MessageToNewChatRoom;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.dto.response.chat.GroupMemberResponse;
import iuh.fit.goat.dto.response.message.ForwardMessageResponse;
import iuh.fit.goat.dto.response.message.MessageDeletedEventResponse;
import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.dto.response.message.PinnedMessageResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.service.AccountService;
import iuh.fit.goat.service.ChatRoomService;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.PinnedMessageService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/chatrooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final AccountService accountService;
    private final PinnedMessageService pinnedMessageService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyChatRooms(Pageable pageable) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        ResultPaginationResponse response = this.chatRoomService.getMyChatRooms(currentAccount.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetailChatRoomInformation(@PathVariable Long id) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        ChatRoomResponse response = this.chatRoomService.getDetailChatRoomInformation(currentAccount, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponse>> getMessagesInChatRoom(@PathVariable Long id, Pageable pageable) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        List<Message> messages = this.chatRoomService.getMessagesInChatRoom(currentAccount, id, pageable);
        List<MessageResponse> response = this.messageService.toMessageResponses(messages);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/media")
    public ResponseEntity<List<MessageResponse>> getMediaMessagesInChatRoom(
            @PathVariable Long id,
            Pageable pageable
    ) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        List<Message> mediaMessages = this.chatRoomService.getMediaMessagesInChatRoom(currentAccount, id, pageable);
        List<MessageResponse> response = this.messageService.toMessageResponses(mediaMessages);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<List<MessageResponse>> getFileMessagesInChatRoom(
            @PathVariable Long id,
            Pageable pageable
    ) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        List<Message> fileMessages = this.chatRoomService.getFileMessagesInChatRoom(currentAccount, id, pageable);
        List<MessageResponse> response = this.messageService.toMessageResponses(fileMessages);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/group")
    public ResponseEntity<ChatRoom> createGroupChat(
            @Valid @RequestBody CreateGroupChatRequest request
    ) throws InvalidException {
        Account currentAccount = getCurrentAccount();
        ChatRoom groupChat = this.chatRoomService.createGroupChat(currentAccount, request);
        return ResponseEntity.ok(groupChat);
    }

    @GetMapping("/group/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(
            @PathVariable Long groupId
    ) throws InvalidException {
        Account currentAccount = getCurrentAccount();
        List<GroupMemberResponse> members = this.chatRoomService.getGroupMembers(currentAccount, groupId);
        return ResponseEntity.ok(members);
    }

    @PutMapping("/group/{chatRoomId}")
    public ResponseEntity<ChatRoom> updateGroupInfo(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody UpdateGroupInfoRequest request
    ) throws InvalidException {
        Account currentAccount = getCurrentAccount();
        ChatRoom updatedChatRoom = this.chatRoomService.updateGroupInfo(currentAccount, chatRoomId, request);
        return ResponseEntity.ok(updatedChatRoom);
    }

    @DeleteMapping("/group/{chatRoomId}")
    public ResponseEntity<Void> leaveGroupChat(@PathVariable Long chatRoomId) throws InvalidException {
        Account currentAccount = getCurrentAccount();
        this.chatRoomService.leaveGroupChat(currentAccount, chatRoomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/group/{chatRoomId}/member")
    public ResponseEntity<ChatMember> addMemberToGroup(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody AddMemberRequest request
    ) throws InvalidException {
        Account currentAccount = getCurrentAccount();
        ChatMember newMember = this.chatRoomService.addMemberToGroup(currentAccount, chatRoomId, request);
        return ResponseEntity.ok(newMember);
    }

    @DeleteMapping("/group/{chatRoomId}/member/{chatMemberId}")
    public ResponseEntity<Void> removeMemberFromGroup(
            @PathVariable Long chatRoomId,
            @PathVariable Long chatMemberId
    ) throws InvalidException {
        Account currentAccount = getCurrentAccount();
        this.chatRoomService.removeMemberFromGroup(currentAccount, chatRoomId, chatMemberId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/group/{chatRoomId}/member/{chatMemberId}")
    public ResponseEntity<ChatMember> updateMemberRole(
            @PathVariable Long chatRoomId,
            @PathVariable Long chatMemberId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) throws InvalidException {
        Account currentAccount = getCurrentAccount();
        ChatMember updatedMember = this.chatRoomService.updateMemberRole(
                currentAccount, chatRoomId, chatMemberId, request);
        return ResponseEntity.ok(updatedMember);
    }

    private Account getCurrentAccount() throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }
        return currentAccount;
    }

    /**
     * Tạo chat room mới và gửi messages
     * Hỗ trợ:
     * - Text only (JSON)
     * - Files only (multipart)
     * - Files + text (multipart)
     */
    @PostMapping(
            value = "/messages",
            consumes = {
                    MediaType.MULTIPART_FORM_DATA_VALUE,
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public ResponseEntity<ChatRoom> sendMessageToNewChatRoom(
            @RequestPart(required = false) @Valid MessageToNewChatRoom request,
            @RequestPart(required = false) List<MultipartFile> files
    ) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        if (request.getAccountId() == null) {
            throw new InvalidException("Receiver account ID is required");
        }

        // Case 1: Files + optional text (multipart)
        if (files != null && !files.isEmpty()) {
            log.info("Creating new chatRoom with {} files to receiver: {}",
                    files.size(), request.getAccountId());

            ChatRoom chatRoom = this.chatRoomService.createNewSingleChatRoomWithFiles(
                    currentAccount,
                    request,
                    files
            );
            return ResponseEntity.ok(chatRoom);
        }

        // Case 2: Text only (JSON - backward compatible)
        if (request.getContent() != null && !request.getContent().isBlank()) {
            log.info("Creating new chatRoom with text to receiver: {}",
                    request.getAccountId());

            MessageToNewChatRoom textRequest = new MessageToNewChatRoom(
                    request.getContent(), request.getAccountId());

            ChatRoom chatRoom = chatRoomService.createNewSingleChatRoom(
                    currentAccount, textRequest);
            return ResponseEntity.ok(chatRoom);
        }

        ChatRoom chatRoom = this.chatRoomService.createNewSingleChatRoom(currentAccount, request);
        return ResponseEntity.ok(chatRoom);
    }

    /**
     * Send messages tới chat room có sẵn
     * Hỗ trợ:
     * - Text only (JSON)
     * - Files only (multipart)
     * - Files + text (multipart)
     */
    @PostMapping(
            value = "/{id}/messages",
            consumes = {
                    MediaType.MULTIPART_FORM_DATA_VALUE,
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public ResponseEntity<List<MessageResponse>> sendMessageToExistChatRoom(
            @PathVariable Long id,
            @RequestPart(required = false) List<MultipartFile> files,
            @RequestPart(required = false) @Valid MessageCreateRequest request
    ) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        if (!this.chatRoomService.isUserInChatRoom(id, currentAccount.getAccountId())) {
            throw new InvalidException("User is not belong to this chat room");
        }

        // Case 1: Files + optional text (multipart)
        if (files != null && !files.isEmpty()) {
            log.info("Sending messages with {} files to chatRoom: {}", files.size(), id);

            List<Message> savedMessages = this.messageService.sendMessagesWithFiles(id, request, files, currentAccount);
            List<MessageResponse> response = this.messageService.toMessageResponses(savedMessages);
            return ResponseEntity.ok(new ArrayList<>(response));
        }

        // Case 2: Text only (JSON - backward compatible)
        if (request != null && request.getContent() != null && !request.getContent().isBlank()) {

            log.info("Sending text-only message to chatRoom: {}", id);

            MessageCreateRequest textRequest = new MessageCreateRequest(
                    request.getContent(),
                    request.getReplyToMessageId()
            );
            Message textMessage = messageService.sendMessage(id, textRequest, currentAccount);
            MessageResponse response = this.messageService.toMessageResponse(textMessage);
            return ResponseEntity.ok(new ArrayList<>(Collections.singletonList(response)));
        }

        throw new InvalidException("At least one file or text content is required");
//        return this.messageService.sendMessage(id, request, currentUser);
    }

    @PostMapping("/{id}/messages/contact")
    public ResponseEntity<List<MessageResponse>> sendContactCardMessages(
            @PathVariable Long id,
            @Valid @RequestBody ContactCardMessageRequest request
    ) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        if (!this.chatRoomService.isUserInChatRoom(id, currentAccount.getAccountId())) {
            throw new InvalidException("User is not belong to this chat room");
        }

        List<Message> savedMessages = this.messageService.sendContactCardMessages(id, request.getUserIds(), currentAccount);
        List<MessageResponse> response = this.messageService.toMessageResponses(savedMessages);
        return ResponseEntity.ok(new ArrayList<>(response));
    }

    @DeleteMapping("/{chatRoomId}/messages/{messageId}")
    public ResponseEntity<MessageResponse> revokeMessage(
            @PathVariable Long chatRoomId,
            @PathVariable String messageId
    ) throws InvalidException, NotFoundException, ConflictException, PermissionException {
        Account currentAccount = getCurrentAccount();
        Message revokedMessage = this.messageService.revokeMessage(chatRoomId, messageId, currentAccount);
        return ResponseEntity.ok(this.messageService.toMessageResponse(revokedMessage));
    }

    @DeleteMapping("/{chatRoomId}/messages/{messageId}/permanent")
    public ResponseEntity<MessageDeletedEventResponse> permanentlyDeleteMessage(
            @PathVariable Long chatRoomId,
            @PathVariable String messageId
    ) throws InvalidException, NotFoundException, PermissionException {
        Account currentAccount = getCurrentAccount();
        MessageDeletedEventResponse response = this.messageService
                .deleteMessagePermanently(chatRoomId, messageId, currentAccount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{chatRoomId}/messages/{messageId}/forward")
    public ResponseEntity<ForwardMessageResponse> forwardMessage(
            @PathVariable Long chatRoomId,
            @PathVariable String messageId,
            @Valid @RequestBody ForwardMessageRequest request
    ) throws InvalidException, NotFoundException, PermissionException {
        Account currentAccount = getCurrentAccount();
        ForwardMessageResponse response = this.messageService
                .forwardMessage(chatRoomId, messageId, request, currentAccount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/direct/exists")
    public ResponseEntity<ChatRoom> checkDirectChatRoomExists(@RequestParam Long accountId) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        ChatRoom chatRoom = this.chatRoomService.existsDirectChatRoom(currentAccount.getAccountId(), accountId);
        return ResponseEntity.ok(chatRoom);
    }

    // ========== PINNED MESSAGES ENDPOINTS ==========
    @PostMapping("/{chatRoomId}/messages/{messageId}/pin")
    public ResponseEntity<PinnedMessageResponse> pinMessage(
            @PathVariable Long chatRoomId,
            @PathVariable String messageId
    ) throws InvalidException
    {
        Account currentAccount = getCurrentAccount();
        PinnedMessageResponse response = this.pinnedMessageService.pinMessage(chatRoomId, messageId, currentAccount);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chatRoomId}/messages/{messageId}/pin")
    public ResponseEntity<Void> unpinMessage(
            @PathVariable Long chatRoomId,
            @PathVariable String messageId
    ) throws InvalidException
    {
        Account currentAccount = getCurrentAccount();
        this.pinnedMessageService.unpinMessage(chatRoomId, messageId, currentAccount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{chatRoomId}/pinned-messages")
    public ResponseEntity<List<PinnedMessageResponse>> getPinnedMessages(
            @PathVariable Long chatRoomId
    ) throws InvalidException
    {
        Account currentAccount = getCurrentAccount();
        List<PinnedMessageResponse> response = this.pinnedMessageService.getPinnedMessages(chatRoomId, currentAccount);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{chatRoomId}/pinned-messages")
    public ResponseEntity<Void> clearAllPinnedMessages( @PathVariable Long chatRoomId) throws InvalidException {
        Account currentAccount = getCurrentAccount();
        this.pinnedMessageService.clearAllPinnedMessages(chatRoomId, currentAccount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{chatRoomId}/pinned-messages/{messageId}")
    public ResponseEntity<PinnedMessageResponse> getPinnedMessageDetail(
            @PathVariable Long chatRoomId,
            @PathVariable String messageId
    ) throws InvalidException
    {
        PinnedMessageResponse response = this.pinnedMessageService.getPinnedMessageDetail(chatRoomId, messageId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chatRoomId}/messages/{messageId}/pinned")
    public ResponseEntity<Boolean> isMessagePinned(
            @PathVariable Long chatRoomId,
            @PathVariable String messageId
    ) {
        boolean isPinned = this.pinnedMessageService.isMessagePinned(chatRoomId, messageId);
        return ResponseEntity.ok(isPinned);
    }

    // ========== GROUP DISSOLUTION ENDPOINTS ==========
    @DeleteMapping("/group/{chatRoomId}/dissolve")
    public ResponseEntity<Void> dissolveGroup(@PathVariable Long chatRoomId, @RequestParam String groupNameConfirmation) throws InvalidException {
        String email = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if(currentAccount == null) throw new InvalidException("Tài khoản không tồn tại");

        this.chatRoomService.smartGroupDissolution(chatRoomId, currentAccount, groupNameConfirmation);

        return ResponseEntity.ok(null);
    }
}
