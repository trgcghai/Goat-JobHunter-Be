package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.repository.ChatMemberRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.AccountService;
import iuh.fit.goat.service.ChatMemberService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMemberServiceImpl implements ChatMemberService {

    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public void updateLastReadMessageId(Long chatRoomId, Long accountId, String messageSk) {
        this.chatMemberRepository.updateLastReadMessageId(chatRoomId, accountId, messageSk);
    }

    @Override
    @Transactional(readOnly = true)
    public String getLastReadMessageSk(Long chatRoomId, Long accountId) {
        return this.chatMemberRepository
                .findLastReadMessageSk(chatRoomId, accountId)
                .orElse(null);
    }

    @Override
    public long countUnreadMessages(Long chatRoomId, ChatMember member) {
        return this.messageRepository.countUnreadMessages(
                String.valueOf(chatRoomId), member.getLastReadMessageSk(), String.valueOf(member.getAccount().getAccountId())
        );
    }

    @Override
    public boolean isMessageRead(Message message, Message currentLastReadMessage) {
        if (message == null || currentLastReadMessage == null) {
            return false;
        }

        if(message.getSender().getEmail().equalsIgnoreCase(SecurityUtil.getCurrentUserEmail())) {
            return true;
        }

        return message.getCreatedAt().isBefore(currentLastReadMessage.getCreatedAt());
    }
}

