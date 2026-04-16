package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.ai.BlogContentRequest;
import iuh.fit.goat.dto.request.ai.ChatRequest;
import iuh.fit.goat.dto.request.conversation.ConversationCreateRequest;
import iuh.fit.goat.dto.request.conversation.ConversationPinUpdateRequest;
import iuh.fit.goat.dto.request.conversation.ConversationTitleUpdateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.conversation.ConversationPinnedResponse;
import iuh.fit.goat.dto.response.conversation.ConversationResponse;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AiService;
import iuh.fit.goat.service.ConversationService;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;
    private final ConversationService conversationService;

    @PostMapping("/chat")
    public ResponseEntity<String> chatWithAi(@Valid @RequestBody ChatRequest request) throws InvalidException {
        String aiResponse = this.aiService.chatWithAi(request);
        return ResponseEntity.ok(aiResponse);
    }

    @PostMapping("/generate/blogs/tags")
    public ResponseEntity<List<String>> generateBlogTags(
            @Valid @RequestBody BlogContentRequest request
    ) {
        List<String> tags = this.aiService.generateBlogTags(request.getContent());
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/conversations")
    @ApiMessage("Conversation created")
    public ResponseEntity<ConversationResponse> createConversation(
            @RequestBody(required = false) ConversationCreateRequest request
    ) throws InvalidException {
        ConversationCreateRequest safeRequest = request == null ? new ConversationCreateRequest() : request;
        ConversationResponse response = this.conversationService.createConversation(safeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/conversations/{conversationId}/title")
    @ApiMessage("Conversation title updated")
    public ResponseEntity<ConversationResponse> updateConversationTitle(
            @PathVariable Long conversationId,
            @Valid @RequestBody ConversationTitleUpdateRequest request
    ) throws InvalidException {
        ConversationResponse response = this.conversationService.updateConversationTitle(conversationId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/conversations/{conversationId}/pin")
    @ApiMessage("Conversation pin status updated")
    public ResponseEntity<ConversationPinnedResponse> updateConversationPinned(
            @PathVariable Long conversationId,
            @Valid @RequestBody ConversationPinUpdateRequest request
    ) throws InvalidException {
        ConversationPinnedResponse response = this.conversationService.updateConversationPinned(conversationId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ApiMessage("Conversation deleted")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long conversationId) throws InvalidException {
        this.conversationService.deleteConversation(conversationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/conversations")
    @ApiMessage("Conversation list fetched")
    public ResponseEntity<ResultPaginationResponse> getMyConversations(Pageable pageable) throws InvalidException {
        ResultPaginationResponse response = this.conversationService.getMyConversations(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @ApiMessage("Conversation messages fetched")
    public ResponseEntity<ResultPaginationResponse> getConversationMessages(
            @PathVariable Long conversationId,
            Pageable pageable
    ) throws InvalidException {
        ResultPaginationResponse response = this.conversationService.getConversationMessages(conversationId, pageable);
        return ResponseEntity.ok(response);
    }
}
