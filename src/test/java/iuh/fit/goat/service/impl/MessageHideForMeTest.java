package iuh.fit.goat.service.impl;

import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Message;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.exception.NotFoundException;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.repository.MessageHiddenRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.service.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageHideForMeTest {

    @Mock private MessageHelper messageHelper;
    @Mock private MessageRepository messageRepository;
    @Mock private MessageHiddenRepository messageHiddenRepository;

    @InjectMocks private MessageServiceImpl messageService;

    private Account currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setAccountId(33L);
        currentUser.setEmail("user@example.com");
    }

    @Test
    void hideMessageForMe_nullChatRoom_throwsInvalid() {
        assertThrows(InvalidException.class, () -> messageService.hideMessageForMe(null, "m1", currentUser));
        verifyNoInteractions(messageHelper, messageRepository, messageHiddenRepository);
    }

    @Test
    void hideMessageForMe_blankMessageId_throwsInvalid() {
        assertThrows(InvalidException.class, () -> messageService.hideMessageForMe(4L, "", currentUser));
        verifyNoInteractions(messageHelper, messageRepository, messageHiddenRepository);
    }

    @Test
    void hideMessageForMe_blankWhitespaceMessageId_throwsInvalid() {
        assertThrows(InvalidException.class, () -> messageService.hideMessageForMe(4L, "   ", currentUser));
        verifyNoInteractions(messageHelper, messageRepository, messageHiddenRepository);
    }

    @Test
    void hideMessageForMe_nullAccount_throwsInvalid() {
        assertThrows(InvalidException.class, () -> messageService.hideMessageForMe(4L, "m1", null));
        verifyNoInteractions(messageHelper, messageRepository, messageHiddenRepository);
    }

    @Test
    void hideMessageForMe_validateChatRoomFailure_propagates() throws Exception {
        doThrow(new InvalidException("chat room invalid")).when(messageHelper).validateChatRoom(4L);

        assertThrows(InvalidException.class, () -> messageService.hideMessageForMe(4L, "m1", currentUser));
        verify(messageHelper).validateChatRoom(4L);
        verifyNoInteractions(messageRepository, messageHiddenRepository);
    }

    @Test
    void hideMessageForMe_userNotMember_throwsPermission() throws Exception {
        when(messageHelper.isUserInChatRoom(4L, currentUser.getAccountId())).thenReturn(false);

        assertThrows(PermissionException.class, () -> messageService.hideMessageForMe(4L, "m1", currentUser));
        verify(messageHelper).validateChatRoom(4L);
        verify(messageHelper).isUserInChatRoom(4L, currentUser.getAccountId());
        verifyNoInteractions(messageRepository, messageHiddenRepository);
    }

    @Test
    void hideMessageForMe_messageNotFound_throwsNotFound() throws Exception {
        when(messageHelper.isUserInChatRoom(4L, currentUser.getAccountId())).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("4", "m1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> messageService.hideMessageForMe(4L, "m1", currentUser));
        verify(messageHelper).validateChatRoom(4L);
        verify(messageHelper).isUserInChatRoom(4L, currentUser.getAccountId());
        verify(messageRepository).findByChatRoomIdAndMessageId("4", "m1");
        verifyNoInteractions(messageHiddenRepository);
    }

    @Test
    void hideMessageForMe_success_recordsHiddenMessage() throws Exception {
        Message message = new Message();
        message.setChatRoomId("4");
        message.setMessageId("m1");

        when(messageHelper.isUserInChatRoom(4L, currentUser.getAccountId())).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("4", "m1")).thenReturn(Optional.of(message));

        messageService.hideMessageForMe(4L, "m1", currentUser);

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(messageHiddenRepository).hideMessageForUser(eq("m1"), eq(currentUser.getAccountId()), instantCaptor.capture());
        assertThat(instantCaptor.getValue()).isNotNull();
        assertThat(Duration.between(instantCaptor.getValue(), Instant.now()).abs())
            .isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void hideMessageForMe_usesMessageIdFromFoundMessage() throws Exception {
        Message message = new Message();
        message.setChatRoomId("4");
        message.setMessageId("server-m1");

        when(messageHelper.isUserInChatRoom(4L, currentUser.getAccountId())).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("4", "client-m1")).thenReturn(Optional.of(message));

        messageService.hideMessageForMe(4L, "client-m1", currentUser);

        verify(messageHiddenRepository).hideMessageForUser(eq("server-m1"), eq(currentUser.getAccountId()), any(Instant.class));
    }

    @Test
    void hideMessageForMe_repositoryFailure_propagates() throws Exception {
        Message message = new Message();
        message.setChatRoomId("4");
        message.setMessageId("m1");

        when(messageHelper.isUserInChatRoom(4L, currentUser.getAccountId())).thenReturn(true);
        when(messageRepository.findByChatRoomIdAndMessageId("4", "m1")).thenReturn(Optional.of(message));
        doThrow(new RuntimeException("dynamo down")).when(messageHiddenRepository)
                .hideMessageForUser(eq("m1"), eq(currentUser.getAccountId()), any(Instant.class));

        assertThrows(RuntimeException.class, () -> messageService.hideMessageForMe(4L, "m1", currentUser));
        verify(messageRepository).findByChatRoomIdAndMessageId("4", "m1");
    }
}