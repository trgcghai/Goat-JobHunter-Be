package iuh.fit.goat.service;

import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.Message;

public interface ChatMemberService {

    void updateLastReadMessageId(Long chatRoomId, Long accountId, String messageSk);

    String getLastReadMessageSk(Long chatRoomId, Long accountId);

    long countUnreadMessages(Long chatRoomId, ChatMember member);

    boolean isMessageRead(Message message, Message currentLastReadMessage);
}

