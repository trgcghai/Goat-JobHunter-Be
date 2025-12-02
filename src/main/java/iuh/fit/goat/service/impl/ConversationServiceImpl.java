package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.conversation.ConversationUpdateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.conversation.ConversationPinnedResponse;
import iuh.fit.goat.dto.response.conversation.ConversationResponse;
import iuh.fit.goat.entity.Conversation;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.ConversationRepository;
import iuh.fit.goat.repository.MessageRepository;
import iuh.fit.goat.repository.UserRepository;
import iuh.fit.goat.service.ConversationService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    public ConversationResponse handleCreateConversation() throws InvalidException {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        User currentUser = this.userRepository.findByContact_Email(currentEmail);
        if(currentUser == null){
            throw new InvalidException("User doesn't exist");
        }

        Conversation conversation = new Conversation();
        conversation.setUser(currentUser);

        Conversation saved = this.conversationRepository.save(conversation);

        return this.convertConversationResponse(saved);
    }

    @Override
    public ConversationResponse handleUpdateConversation(ConversationUpdateRequest request) {
            Conversation conversation = this.handleGetConversationById(request.getConversationId());

            if(request.getTitle() != null && !request.getTitle().isEmpty()){
                conversation.setTitle(request.getTitle());
            }

            Conversation updated = this.conversationRepository.save(conversation);

            return this.convertConversationResponse(updated);
    }

    @Override
    @Transactional
    public List<ConversationPinnedResponse> handlePinConversation(List<Long> conversationIds) {
        return this.handleSetPinnedForConversations(conversationIds, true);
    }

    @Override
    @Transactional
    public List<ConversationPinnedResponse> handleUnpinConversation(List<Long> conversationIds) {
        return this.handleSetPinnedForConversations(conversationIds, false);
    }

    @Override
    @Transactional
    public void handleDeleteConversations(List<Long> conversationIds) {
        List<Conversation> conversations = this.conversationRepository.findAllById(conversationIds);
        if(conversations.isEmpty()) return;

        conversations.forEach(conversation -> conversation.setDeleted(true));
        this.conversationRepository.saveAll(conversations);
    }

    @Override
    public void handleUpdateTitleIfFirstAiMessage(Long conversationId, String aiMessage) {
        Conversation conversation = this.handleGetConversationById(conversationId);
        long totalMessages = this.messageRepository.countByConversation_ConversationId(conversation.getConversationId());

        if (totalMessages == 2) {
            String title = aiMessage.length() > 60 ? aiMessage.substring(0, 60) : aiMessage;
            conversation.setTitle(title);

            this.conversationRepository.save(conversation);
        }

    }

    @Override
    public Conversation handleGetConversationById(Long id) {
        return this.conversationRepository.findById(id).orElse(null);
    }

    @Override
    public ResultPaginationResponse handleGetAllConversations(Specification<Conversation> spec, Pageable pageable) {
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        Specification<Conversation> specification = (root, query, cb) ->
        {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("deleted")));
            predicates.add(cb.equal(root.get("user").get("contact").get("email"), currentEmail));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        if(spec != null){
            specification = specification.and(spec);
        }

        Page<Conversation> page = this.conversationRepository.findAll(specification, pageable);

        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        List<ConversationResponse> conversations = page.getContent().stream()
                .map(this::convertConversationResponse)
                .toList();

        return new ResultPaginationResponse(meta, conversations);
    }

    @Override
    public ConversationResponse convertConversationResponse (Conversation conversation) {
        ConversationResponse conversationResponse = new ConversationResponse();
        conversationResponse.setConversationId(conversation.getConversationId());
        conversationResponse.setTitle(conversation.getTitle());
        conversationResponse.setPinned(conversation.isPinned());
        conversationResponse.setUpdatedAt(conversation.getUpdatedAt());

        return conversationResponse;
    }

    private List<ConversationPinnedResponse> handleSetPinnedForConversations(List<Long> conversationIds, boolean pinned)
    {
        if(conversationIds == null || conversationIds.isEmpty()){
            return Collections.emptyList();
        }

        List<Conversation> conversations = this.conversationRepository.findAllById(conversationIds);
        conversations.forEach(conversation -> conversation.setPinned(pinned));
        this.conversationRepository.saveAll(conversations);

        return conversations.stream().map(
                conversation -> new ConversationPinnedResponse(
                        conversation.getConversationId(), conversation.isPinned()
                )
        ).collect(Collectors.toList());
    }
}
