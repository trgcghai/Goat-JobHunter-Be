package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.chat.*;
import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.request.message.MessageToNewChatRoom;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import iuh.fit.goat.dto.response.chat.GroupMemberResponse;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.ChatRoomService;
import iuh.fit.goat.service.MessageService;
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
    private final UserRepository userRepository;


    @GetMapping("/me")
    public ResponseEntity<?> getMyChatRooms(Pageable pageable) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        ResultPaginationResponse response = chatRoomService.getMyChatRooms(currentUser.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetailChatRoomInformation(@PathVariable Long id) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        ChatRoomResponse response = chatRoomService.getDetailChatRoomInformation(currentUser, id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessagesInChatRoom(@PathVariable Long id, Pageable pageable) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        List<Message> messages = chatRoomService.getMessagesInChatRoom(currentUser, id, pageable);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{id}/media")
    public ResponseEntity<List<Message>> getMediaMessagesInChatRoom(
            @PathVariable Long id,
            Pageable pageable
    ) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        List<Message> mediaMessages = chatRoomService.getMediaMessagesInChatRoom(currentUser, id, pageable);
        return ResponseEntity.ok(mediaMessages);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<List<Message>> getFileMessagesInChatRoom(
            @PathVariable Long id,
            Pageable pageable
    ) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        List<Message> fileMessages = chatRoomService.getFileMessagesInChatRoom(currentUser, id, pageable);
        return ResponseEntity.ok(fileMessages);
    }

    @PostMapping("/group")
    public ResponseEntity<ChatRoom> createGroupChat(
            @Valid @RequestBody CreateGroupChatRequest request
    ) throws InvalidException {
        User currentUser = getCurrentUser();
        ChatRoom groupChat = chatRoomService.createGroupChat(currentUser, request);
        return ResponseEntity.ok(groupChat);
    }

    @GetMapping("/group/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(
            @PathVariable Long groupId
    ) throws InvalidException {
        User currentUser = getCurrentUser();
        List<GroupMemberResponse> members = chatRoomService.getGroupMembers(currentUser, groupId);
        return ResponseEntity.ok(members);
    }

    @PutMapping("/group/{chatRoomId}")
    public ResponseEntity<ChatRoom> updateGroupInfo(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody UpdateGroupInfoRequest request
    ) throws InvalidException {
        User currentUser = getCurrentUser();
        ChatRoom updatedChatRoom = chatRoomService.updateGroupInfo(currentUser, chatRoomId, request);
        return ResponseEntity.ok(updatedChatRoom);
    }

    @DeleteMapping("/group/{chatRoomId}")
    public ResponseEntity<Void> leaveGroupChat(
            @PathVariable Long chatRoomId
    ) throws InvalidException {
        User currentUser = getCurrentUser();
        chatRoomService.leaveGroupChat(currentUser, chatRoomId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/group/{chatRoomId}/member")
    public ResponseEntity<ChatMember> addMemberToGroup(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody AddMemberRequest request
    ) throws InvalidException {
        User currentUser = getCurrentUser();
        ChatMember newMember = chatRoomService.addMemberToGroup(currentUser, chatRoomId, request);
        return ResponseEntity.ok(newMember);
    }

    @DeleteMapping("/group/{chatRoomId}/member/{chatMemberId}")
    public ResponseEntity<Void> removeMemberFromGroup(
            @PathVariable Long chatRoomId,
            @PathVariable Long chatMemberId
    ) throws InvalidException {
        User currentUser = getCurrentUser();
        chatRoomService.removeMemberFromGroup(currentUser, chatRoomId, chatMemberId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/group/{chatRoomId}/member/{chatMemberId}")
    public ResponseEntity<ChatMember> updateMemberRole(
            @PathVariable Long chatRoomId,
            @PathVariable Long chatMemberId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) throws InvalidException {
        User currentUser = getCurrentUser();
        ChatMember updatedMember = chatRoomService.updateMemberRole(
                currentUser, chatRoomId, chatMemberId, request);
        return ResponseEntity.ok(updatedMember);
    }

    private User getCurrentUser() throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }
        return currentUser;
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

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        if (request.getAccountId() == null) {
            throw new InvalidException("Receiver account ID is required");
        }

        // Case 1: Files + optional text (multipart)
        if (files != null && !files.isEmpty()) {
            log.info("Creating new chatRoom with {} files to receiver: {}",
                    files.size(), request.getAccountId());

            ChatRoom chatRoom = chatRoomService.createNewSingleChatRoomWithFiles(
                    currentUser,
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
                    currentUser, textRequest);
            return ResponseEntity.ok(chatRoom);
        }

        throw new InvalidException("Message content or files are required");
//        ChatRoom chatRoom = this.chatRoomService.createNewSingleChatRoom(currentUser, request);
//        return ResponseEntity.ok(chatRoom);
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
    public ResponseEntity<List<Message>> sendMessageToExistChatRoom(
            @PathVariable Long id,
            @RequestPart(required = false) List<MultipartFile> files,
            @RequestPart(required = false) @Valid MessageCreateRequest request
    ) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        if (!this.chatRoomService.isUserInChatRoom(id, currentUser.getAccountId())) {
            throw new InvalidException("User is not belong to this chat room");
        }

        // Case 1: Files + optional text (multipart)
        if (files != null && !files.isEmpty()) {
            log.info("Sending messages with {} files to chatRoom: {}", files.size(), id);

            List<Message> savedMessages = messageService.sendMessagesWithFiles(id, request, files, currentUser);
            return ResponseEntity.ok(savedMessages);
        }

        // Case 2: Text only (JSON - backward compatible)
        if (request != null && request.getContent() != null && !request.getContent().isBlank()) {

            log.info("Sending text-only message to chatRoom: {}", id);

            MessageCreateRequest textRequest = new MessageCreateRequest(request.getContent());
            Message textMessage = messageService.sendMessage(id, textRequest, currentUser);

            return ResponseEntity.ok(new ArrayList<>(Collections.singletonList(textMessage)));
        }

        throw new InvalidException("At least one file or text content is required");
//        return this.messageService.sendMessage(id, request, currentUser);
    }

    @GetMapping("/direct/exists")
    public ResponseEntity<ChatRoom> checkDirectChatRoomExists(@RequestParam Long accountId) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        ChatRoom chatRoom = chatRoomService.existsDirectChatRoom(currentUser.getAccountId(), accountId);
        return ResponseEntity.ok(chatRoom);
    }
}