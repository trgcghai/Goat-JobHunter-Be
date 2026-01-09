package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.ResultPaginationResponse;
import org.springframework.data.domain.Pageable;

public interface ChatRoomService {
    ResultPaginationResponse getMyChatRooms(Long accountId, Pageable pageable);
}
