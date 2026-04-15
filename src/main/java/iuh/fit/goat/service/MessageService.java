package iuh.fit.goat.service;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.request.message.ForwardMessageRequest;
import iuh.fit.goat.dto.request.message.MessageCreateRequest;
import iuh.fit.goat.dto.response.message.ForwardMessageResponse;
import iuh.fit.goat.dto.response.message.MessageDeletedEventResponse;
import iuh.fit.goat.dto.response.message.MessageResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.exception.ConflictException;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.exception.PermissionException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {

    Message getLastMessageByChatRoom(Long chatRoomId) throws InvalidException;

    List<Message> getMessagesByChatRoom(Long chatRoomId, Pageable pageable);

    Message sendMessage(Long chatRoomId, MessageCreateRequest request, Account currentAccount) throws InvalidException;

    List<Message> sendMessagesWithFiles(Long chatRoomId, MessageCreateRequest request, List<MultipartFile> files, Account currentAccount) throws InvalidException;

    List<Message> sendContactCardMessages(Long chatRoomId, List<Long> userIds, Account currentAccount) throws InvalidException;

    void sendMessageToUsers(Long chatRoomId, Message message);

    MessageResponse toMessageResponse(Message message);

    List<MessageResponse> toMessageResponses(List<Message> messages);

    List<Message> getMediaMessagesByChatRoom(Long chatRoomId, Pageable pageable) throws InvalidException;

    List<Message> getFileMessagesByChatRoom(Long chatRoomId, Pageable pageable) throws InvalidException;

    Message revokeMessage(Long chatRoomId, String messageId, Account currentAccount)
            throws InvalidException, NotFoundException, ConflictException, PermissionException;

    MessageDeletedEventResponse deleteMessagePermanently(Long chatRoomId, String messageId, Account currentAccount)
            throws InvalidException, NotFoundException, PermissionException;

    ForwardMessageResponse forwardMessage(Long sourceChatRoomId, String messageId, ForwardMessageRequest request,
                                          Account currentAccount)
            throws InvalidException, NotFoundException, PermissionException;

    void createAndSendSystemMessage(Long chatRoomId, MessageEvent type, Account actor, Object... params);
}
