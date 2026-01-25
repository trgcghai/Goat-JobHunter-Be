package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.message.MessageToNewChatRoom;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatRoomService {
    ResultPaginationResponse getMyChatRooms(Long accountId, Pageable pageable);

    List<Message> getMessagesInChatRoom(User user, Long chatRoomId, Pageable pageable) throws InvalidException;

    boolean isUserInChatRoom(ChatRoom chatRoom, Long accountId);

    boolean isUserInChatRoom(Long chatRoomId, Long accountId) throws InvalidException;

    ChatRoom createNewSingleChatRoom(User currentUser, MessageToNewChatRoom request) throws InvalidException;

    ChatRoom createNewSingleChatRoomWithFiles(User currentUser, MessageToNewChatRoom request, List<MultipartFile> files) throws InvalidException;

    ChatRoom existsDirectChatRoom(Long currentUserId, Long otherUserId);
}
