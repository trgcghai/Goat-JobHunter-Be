package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.conversation.ConversationCreateRequest;
import iuh.fit.goat.dto.request.conversation.ConversationPinUpdateRequest;
import iuh.fit.goat.dto.request.conversation.ConversationTitleUpdateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.conversation.ConversationPinnedResponse;
import iuh.fit.goat.dto.response.conversation.ConversationResponse;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Conversation;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;

public interface ConversationService {
    ConversationResponse createConversation(ConversationCreateRequest request) throws InvalidException;

    ConversationResponse updateConversationTitle(Long conversationId, ConversationTitleUpdateRequest request)
            throws InvalidException;

    ConversationPinnedResponse updateConversationPinned(Long conversationId, ConversationPinUpdateRequest request)
            throws InvalidException;

    void deleteConversation(Long conversationId) throws InvalidException;

    ResultPaginationResponse getMyConversations(Pageable pageable) throws InvalidException;

    ResultPaginationResponse getConversationMessages(Long conversationId, Pageable pageable) throws InvalidException;

    Conversation getOwnedConversation(Long conversationId, Account account) throws InvalidException;
}