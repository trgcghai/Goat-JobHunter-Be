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
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/messages")
    public ResponseEntity<ChatRoom> sendMessageToNewChatRoom(@Valid @RequestBody MessageToNewChatRoom request) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        ChatRoom chatRoom = this.chatRoomService.createNewSingleChatRoom(currentUser, request);
        return ResponseEntity.ok(chatRoom);
    }

    @PostMapping("/{id}/messages")
    public Message sendMessageToExistChatRoom(@PathVariable Long id, @Valid @RequestBody MessageCreateRequest request) throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new InvalidException("User not found");
        }

        if (!this.chatRoomService.isUserInChatRoom(id, currentUser.getAccountId())) {
            throw new InvalidException("User is not belong to this chat room");
        }

        return this.messageService.sendMessage(id, request, currentUser);
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