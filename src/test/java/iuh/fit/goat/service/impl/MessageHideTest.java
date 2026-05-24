package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.repository.MessageHiddenRepository;
import iuh.fit.goat.service.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.exception.PermissionException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageHideTest {

    @Mock private MessageHelper messageHelper;
    @Mock private MessageRepository messageRepository;
    @Mock private MessageHiddenRepository messageHiddenRepository;

    @InjectMocks private MessageServiceImpl messageService;

    private Account user;

    @BeforeEach
    void setUp() { user = new User(); user.setAccountId(33L); }

    @Test
    void hideMessageForMe_shouldCallHiddenRepo() throws Exception {
        Message m = new Message(); m.setMessageId("x1"); m.setChatRoomId("4");
        when(messageRepository.findByChatRoomIdAndMessageId("4","x1")).thenReturn(Optional.of(m));
        doNothing().when(messageHelper).validateChatRoom(4L);
        when(messageHelper.isUserInChatRoom(4L, user.getAccountId())).thenReturn(true);

        messageService.hideMessageForMe(4L, "x1", user);

        verify(messageHiddenRepository).hideMessageForUser(eq("x1"), eq(user.getAccountId()), any());
    }

    @Test
    void hideMessageForMe_nullChatRoom_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.hideMessageForMe(null, "m", user)
        );
    }

    @Test
    void hideMessageForMe_blankMessage_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.hideMessageForMe(4L, "", user)
        );
    }

    @Test
    void hideMessageForMe_nullAccount_throwsInvalid() {
        org.junit.jupiter.api.Assertions.assertThrows(InvalidException.class, () ->
                messageService.hideMessageForMe(4L, "x1", null)
        );
    }

    @Test
    void hideMessageForMe_messageNotFound_throwsNotFound() throws Exception {
        when(messageRepository.findByChatRoomIdAndMessageId("4","x2")).thenReturn(Optional.empty());
        when(messageHelper.isUserInChatRoom(4L, user.getAccountId())).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(NotFoundException.class, () ->
                messageService.hideMessageForMe(4L, "x2", user)
        );
    }

    @Test
    void hideMessageForMe_userNotMember_throwsPermission() throws Exception {
        Message m = new Message(); m.setMessageId("x3"); m.setChatRoomId("4");
        when(messageRepository.findByChatRoomIdAndMessageId("4","x3")).thenReturn(Optional.of(m));
        doNothing().when(messageHelper).validateChatRoom(4L);
        when(messageHelper.isUserInChatRoom(4L, user.getAccountId())).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(PermissionException.class, () ->
                messageService.hideMessageForMe(4L, "x3", user)
        );
    }
}
