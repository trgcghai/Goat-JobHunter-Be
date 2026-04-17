package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.ChatMember;
import iuh.fit.goat.entity.ChatRoom;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.exception.InvalidException;
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

    private final AccountService accountService;

    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
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
    public long countUnreadMessages(Long chatRoomId) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findByRoomId(chatRoomId)
                .orElseThrow(() -> new InvalidException("Phòng trò chuyện không tồn tại"));

        String email = SecurityUtil.getCurrentUserEmail();
        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if(currentAccount == null) throw new InvalidException("Tài khoản không tồn tại");

        ChatMember member = this.chatMemberRepository.findByRoomRoomIdAndDeletedAtIsNull(chatRoom.getRoomId()).stream()
                    .filter(m -> m.getAccount().getAccountId() == currentAccount.getAccountId())
                    .findFirst()
                    .orElseThrow(() -> new InvalidException("Không phải là thành viên của phòng trò chuyện này"));


        return this.messageRepository.countUnreadMessages(String.valueOf(chatRoom.getRoomId()), member.getLastReadMessageSk());
    }

    @Override
    public boolean isMessageRead(Message message, Message currentLastReadMessage) {
        if (message == null || currentLastReadMessage == null) {
            return false;
        }
        return message.getCreatedAt().isBefore(currentLastReadMessage.getCreatedAt());
    }
}

