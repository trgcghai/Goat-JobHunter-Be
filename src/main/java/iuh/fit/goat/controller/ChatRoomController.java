package iuh.fit.goat.controller;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.ChatRoomService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;


    @GetMapping("/me")
    public ResponseEntity<ResultPaginationResponse> getMyChatRooms(Pageable pageable) {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalArgumentException("User not authenticated"));

        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IllegalArgumentException("User not found");
        }

        ResultPaginationResponse response = chatRoomService.getMyChatRooms(currentUser.getAccountId(), pageable);
        return ResponseEntity.ok(response);
    }
}