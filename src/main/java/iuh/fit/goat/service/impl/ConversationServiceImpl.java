package iuh.fit.goat.service.impl;

import iuh.fit.goat.dto.request.conversation.ConversationCreateRequest;
import iuh.fit.goat.dto.request.conversation.ConversationPinUpdateRequest;
import iuh.fit.goat.dto.request.conversation.ConversationTitleUpdateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.ai.AIMessageResponse;
import iuh.fit.goat.dto.response.conversation.ConversationPinnedResponse;
import iuh.fit.goat.dto.response.conversation.ConversationResponse;
import iuh.fit.goat.entity.AIMessage;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.entity.Conversation;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.repository.AIMessageRepository;
import iuh.fit.goat.repository.ConversationRepository;
import iuh.fit.goat.service.AccountService;
import iuh.fit.goat.service.ConversationService;
import iuh.fit.goat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private static final String DEFAULT_CONVERSATION_TITLE = "New conversation";

    private final ConversationRepository conversationRepository;
    private final AIMessageRepository aiMessageRepository;
    private final AccountService accountService;

    @Override
    @Transactional
    public ConversationResponse createConversation(ConversationCreateRequest request) throws InvalidException {
        Account currentAccount = this.getCurrentAccount();

        Conversation conversation = new Conversation();
        conversation.setTitle(this.normalizeTitle(request != null ? request.getTitle() : null));
        conversation.setPinned(false);
        conversation.setAccount(currentAccount);

        Conversation saved = this.conversationRepository.save(conversation);
        return this.toConversationResponse(saved);
    }

    @Override
    @Transactional
    public ConversationResponse updateConversationTitle(Long conversationId, ConversationTitleUpdateRequest request)
            throws InvalidException {
        Account currentAccount = this.getCurrentAccount();
        Conversation conversation = this.getOwnedConversation(conversationId, currentAccount);

        String normalizedTitle = this.normalizeRequiredTitle(request.getTitle());
        conversation.setTitle(normalizedTitle);

        Conversation saved = this.conversationRepository.save(conversation);
        return this.toConversationResponse(saved);
    }

    @Override
    @Transactional
    public ConversationPinnedResponse updateConversationPinned(Long conversationId, ConversationPinUpdateRequest request)
            throws InvalidException {
        Account currentAccount = this.getCurrentAccount();
        Conversation conversation = this.getOwnedConversation(conversationId, currentAccount);

        conversation.setPinned(request.getPinned());
        Conversation saved = this.conversationRepository.save(conversation);

        return new ConversationPinnedResponse(saved.getConversationId(), saved.isPinned());
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId) throws InvalidException {
        Account currentAccount = this.getCurrentAccount();
        Conversation conversation = this.getOwnedConversation(conversationId, currentAccount);

        conversation.onDelete();
        this.conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationResponse getMyConversations(Pageable pageable) throws InvalidException {
        Account currentAccount = this.getCurrentAccount();

        Page<Conversation> page = this.conversationRepository
                .findPageActiveByAccountIdOrderByPinnedAndUpdated(currentAccount.getAccountId(), pageable);

        List<ConversationResponse> conversations = page.getContent().stream()
                .map(this::toConversationResponse)
                .toList();

        return this.toPaginationResponse(page, conversations);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationResponse getConversationMessages(Long conversationId, Pageable pageable) throws InvalidException {
        Account currentAccount = this.getCurrentAccount();
        Conversation conversation = this.getOwnedConversation(conversationId, currentAccount);

        Page<AIMessage> page = this.aiMessageRepository.findPageByConversationId(conversation.getConversationId(), pageable);

        List<AIMessageResponse> messages = page.getContent().stream()
                .map(this::toAIMessageResponse)
                .toList();

        return this.toPaginationResponse(page, messages);
    }

    private AIMessageResponse toAIMessageResponse(AIMessage message) {
        return new AIMessageResponse(
                message.getAiMessageId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    private ResultPaginationResponse toPaginationResponse(Page<?> page, Object result) {
        ResultPaginationResponse.Meta meta = new ResultPaginationResponse.Meta();
        meta.setPage(page.getNumber() + 1);
        meta.setPageSize(page.getSize());
        meta.setPages(page.getTotalPages());
        meta.setTotal(page.getTotalElements());

        return new ResultPaginationResponse(meta, result);
    }

    @Override
    public Conversation getOwnedConversation(Long conversationId, Account account) throws InvalidException {
        if (conversationId == null) {
            throw new InvalidException("Conversation ID is required");
        }

        return this.conversationRepository
                .findByConversationIdAndAccount_AccountIdAndDeletedAtIsNull(conversationId, account.getAccountId())
                .orElseThrow(() -> new InvalidException("Conversation not found or access denied"));
    }

    private Account getCurrentAccount() throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new InvalidException("User not authenticated"));

        Account currentAccount = this.accountService.handleGetAccountByEmail(email);
        if (currentAccount == null) {
            throw new InvalidException("User not found");
        }

        return currentAccount;
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getConversationId(),
                conversation.getTitle(),
                conversation.isPinned(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return DEFAULT_CONVERSATION_TITLE;
        }

        return title.trim();
    }

    private String normalizeRequiredTitle(String title) throws InvalidException {
        if (title == null || title.isBlank()) {
            throw new InvalidException("Title must not be blank");
        }

        return title.trim();
    }
}