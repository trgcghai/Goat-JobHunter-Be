package iuh.fit.goat.service;

import iuh.fit.goat.dto.request.conversation.ConversationUpdateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.conversation.ConversationPinnedResponse;
import iuh.fit.goat.dto.response.conversation.ConversationResponse;
import iuh.fit.goat.entity.Conversation;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ConversationService {
    ConversationResponse handleCreateConversation() throws InvalidException;

    ConversationResponse handleUpdateConversation(ConversationUpdateRequest request);

    List<ConversationPinnedResponse> handlePinConversation(List<Long> conversationIds);

    List<ConversationPinnedResponse> handleUnpinConversation(List<Long> conversationIds);

    void handleDeleteConversations(List<Long> conversationIds);

    void handleUpdateTitleIfFirstAiMessage(Long conversationId, String aiMessage);

    Conversation handleGetConversationById(Long id);

    ResultPaginationResponse handleGetAllConversations(Specification<Conversation> spec, Pageable pageable);

    ConversationResponse convertConversationResponse (Conversation conversation);

}
