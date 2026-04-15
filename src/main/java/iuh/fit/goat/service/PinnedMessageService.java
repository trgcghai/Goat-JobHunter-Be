package iuh.fit.goat.service;

import iuh.fit.goat.dto.response.message.PinnedMessageResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.PinnedMessage;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.exception.PermissionException;

import java.util.List;

public interface PinnedMessageService {

    PinnedMessageResponse pinMessage(Long chatRoomId, String messageId, Account currentAccount) throws InvalidException;

    void unpinMessage(Long chatRoomId, String messageId, Account currentAccount) throws InvalidException;

    List<PinnedMessageResponse> getPinnedMessages(Long chatRoomId, Account currentAccount) throws InvalidException;

    boolean isMessagePinned(Long chatRoomId, String messageId);

    PinnedMessageResponse getPinnedMessageDetail(Long chatRoomId, String messageId) throws InvalidException;

    void clearAllPinnedMessages(Long chatRoomId, Account currentAccount) throws InvalidException;
}

