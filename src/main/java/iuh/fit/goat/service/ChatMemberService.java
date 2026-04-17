package iuh.fit.goat.service;

import iuh.fit.goat.entity.Message;
import iuh.fit.goat.exception.InvalidException;

public interface ChatMemberService {

    void updateLastReadMessageId(Long chatRoomId, Long accountId, String messageSk);

    String getLastReadMessageSk(Long chatRoomId, Long accountId);

    long countUnreadMessages(Long chatRoomId) throws InvalidException;

    boolean isMessageRead(Message message, Message currentLastReadMessage);
}

