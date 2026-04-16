package iuh.fit.goat.service.impl;

import iuh.fit.goat.common.MessageEvent;
import iuh.fit.goat.dto.response.message.PinnedMessageResponse;
import iuh.fit.goat.entity.*;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AccountRepository;
import iuh.fit.goat.repository.ChatRoomRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.MessageService;
import iuh.fit.goat.service.PinnedMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class PinnedMessageServiceImpl implements PinnedMessageService {
    private final MessageService messageService;

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public PinnedMessageResponse pinMessage(Long chatRoomId, String messageId, Account currentAccount) throws InvalidException
    {
        ChatRoom chatRoom = this.chatRoomRepository.findByRoomIdAndDeletedAtIsNull(chatRoomId)
                .orElseThrow(() -> new InvalidException("Không tìm thấy phòng chat"));

        isCurrentMemberInChatRoom(chatRoom, currentAccount.getAccountId());

        List<PinnedMessage> pinnedMessages = this.messageRepository.getPinnedMessagesByChatRoom(chatRoomId.toString());
        if(pinnedMessages.size() >= 5) throw new InvalidException("Phòng chat đã đạt giới hạn tin nhắn được ghim (5 tin nhắn)");

        Message message = this.messageRepository.findByChatRoomIdAndMessageId(chatRoomId.toString(), messageId)
                .orElseThrow(() -> new InvalidException("Tin nhắn không tồn tại trong phòng chat"));

        if (this.messageRepository.isPinned(chatRoomId.toString(), messageId)) {
            throw new InvalidException("Tin nhắn đã được ghim trước đó");
        }

        String currentEmail = currentAccount.getEmail();
        Account account = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        PinnedMessage pinnedMessage = this.messageRepository.pinMessage(
                chatRoomId.toString(),
                messageId,
                account instanceof Company company ? company.getName() : ((User) Objects.requireNonNull(account)).getFullName()
        );

        PinnedMessageResponse response = buildPinnedMessageResponse(pinnedMessage, message);

        this.messageService.createAndSendSystemMessage(
                chatRoomId,
                MessageEvent.MESSAGE_PINNED,
                account,
                response.getMessage(),
                response.getMessageId()
        );

        return response;
    }

    @Override
    @Transactional
    public void unpinMessage(Long chatRoomId, String messageId, Account currentAccount) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findByRoomIdAndDeletedAtIsNull(chatRoomId)
                .orElseThrow(() -> new InvalidException("Không tìm thấy phòng chat"));

        isCurrentMemberInChatRoom(chatRoom, currentAccount.getAccountId());

        PinnedMessage pinnedMessage = this.messageRepository.getPinnedMessage(chatRoomId.toString(), messageId);
        if (pinnedMessage == null) throw new InvalidException("Tin nhắn không được ghim hoặc đã bị gỡ ghim");

        this.messageRepository.deletePinnedMessage(chatRoomId.toString(), messageId);

        String currentEmail = currentAccount.getEmail();
        Account account = this.accountRepository.findByEmailAndDeletedAtIsNull(currentEmail).orElse(null);
        Message message = this.messageRepository.findByChatRoomIdAndMessageId(chatRoomId.toString(), messageId)
                .orElseThrow(() -> new InvalidException("Tin nhắn không tồn tại trong phòng chat"));
        PinnedMessageResponse response = buildPinnedMessageResponse(pinnedMessage, message);
        this.messageService.createAndSendSystemMessage(
                chatRoomId,
                MessageEvent.MESSAGE_UNPINNED,
                account,
                response.getMessage(),
                response.getMessageId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PinnedMessageResponse> getPinnedMessages(Long chatRoomId, Account currentAccount) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findByRoomId(chatRoomId)
                .orElseThrow(() -> new InvalidException("Không tìm thấy phòng chat"));

        if (!isUserInChatRoom(chatRoom, currentAccount.getAccountId())) {
            throw new InvalidException("Tài khoản không phải là thành viên của phòng chat");
        }

        List<PinnedMessage> pinnedMessages = this.messageRepository.getPinnedMessagesByChatRoom(chatRoomId.toString());

        return pinnedMessages.stream()
                .map(pm -> {
                    try {
                        Message message = this.messageRepository.findByChatRoomIdAndMessageId(
                                chatRoomId.toString(),
                                pm.getMessageId()
                        ).orElse(null);

                        return buildPinnedMessageResponse(pm, message);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMessagePinned(Long chatRoomId, String messageId) {
        return this.messageRepository.isPinned(chatRoomId.toString(), messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public PinnedMessageResponse getPinnedMessageDetail(Long chatRoomId, String messageId) throws InvalidException {
        PinnedMessage pinnedMessage = this.messageRepository.getPinnedMessage(chatRoomId.toString(), messageId);
        if (pinnedMessage == null) throw new InvalidException("Tin nhắn không được ghim hoặc đã bị gỡ ghim");

        Message message = this.messageRepository.findByChatRoomIdAndMessageId(
                chatRoomId.toString(),
                messageId
        ).orElse(null);

        return buildPinnedMessageResponse(pinnedMessage, message);
    }

    @Override
    @Transactional
    public void clearAllPinnedMessages(Long chatRoomId, Account currentAccount) throws InvalidException {
        ChatRoom chatRoom = this.chatRoomRepository.findByRoomIdAndDeletedAtIsNull(chatRoomId)
                .orElseThrow(() -> new InvalidException("Không tìm thấy phòng chat"));

        isCurrentMemberInChatRoom(chatRoom, currentAccount.getAccountId());

        List<PinnedMessage> pinnedMessages = this.messageRepository.getPinnedMessagesByChatRoom(chatRoomId.toString());
        pinnedMessages.forEach(pm ->
            this.messageRepository.deletePinnedMessage(chatRoomId.toString(), pm.getMessageId())
        );
    }

    private void isCurrentMemberInChatRoom(ChatRoom chatRoom, Long accountId) throws InvalidException {
        chatRoom.getMembers().stream()
                .filter(m -> m.getDeletedAt() == null &&
                        m.getAccount().getAccountId() == accountId)
                .findFirst()
                .orElseThrow(() -> new InvalidException("Tài khoản không phải là thành viên của phòng chat"));
    }

    private boolean isUserInChatRoom(ChatRoom chatRoom, Long accountId) {
        return chatRoom.getMembers().stream()
                .anyMatch(m -> m.getDeletedAt() == null &&
                        m.getAccount().getAccountId() == accountId);
    }

    private PinnedMessageResponse buildPinnedMessageResponse(PinnedMessage pinnedMessage, Message message) {
        PinnedMessageResponse response = PinnedMessageResponse.builder()
                .chatRoomId(pinnedMessage.getChatRoomId())
                .messageId(pinnedMessage.getMessageId())
                .pinnedBy(pinnedMessage.getPinnedBy())
                .pinnedAt(pinnedMessage.getPinnedAt())
                .build();

        if (message != null) response.setMessage(this.messageService.toMessageResponse(message));

        return response;
    }
}

