package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatRoomService {
    ResultPaginationResponse getMyChatRooms(Long accountId, Pageable pageable);

    List<Message> getMessagesInChatRoom(User user, Long chatRoomId, Pageable pageable) throws InvalidException;
}
