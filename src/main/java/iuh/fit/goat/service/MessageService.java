package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface  MessageService {

    Message getLastMessageByChatRoom(Long chatRoomId) throws InvalidException;

    List<Message> getMessagesByChatRoom(Long chatRoomId, Pageable pageable);

    Message sendMessage(Long chatRoomId, MessageCreateRequest request, User currentUser) throws InvalidException;

    List<Message> sendMessagesWithFiles(Long chatRoomId, MessageCreateRequest request, List<MultipartFile> files, User currentUser) throws InvalidException;

    void sendMessageToUsers(Long chatRoomId, Message message);
}
