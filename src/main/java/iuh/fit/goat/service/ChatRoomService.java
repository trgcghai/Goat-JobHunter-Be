package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.chat.CreateGroupChatRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.chat.ChatRoomResponse;
import org.springframework.data.domain.Pageable;

public interface ChatRoomService {
    ChatRoomResponse createGroupChatRoom(CreateGroupChatRequest request, Long ownerAccountId);
    ResultPaginationResponse getMyChatRooms(Long accountId, Pageable pageable);
}
