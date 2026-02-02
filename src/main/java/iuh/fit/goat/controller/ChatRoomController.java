package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.request.message.MessageToNewChatRoom;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
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