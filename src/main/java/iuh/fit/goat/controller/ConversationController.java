package iuh.fit.goat.controller;

import com.turkraft.springfilter.boot.Filter;
import iuh.fit.goat.dto.request.conversation.ConversationIdsRequest;
import iuh.fit.goat.dto.request.conversation.ConversationUpdateRequest;
import iuh.fit.goat.dto.response.ResultPaginationResponse;
import iuh.fit.goat.dto.response.conversation.ConversationPinnedResponse;
import iuh.fit.goat.dto.response.conversation.ConversationResponse;
import iuh.fit.goat.entity.Conversation;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation() throws InvalidException {
        ConversationResponse response = this.conversationService.handleCreateConversation();
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ConversationResponse> updateConversation(@Valid @RequestBody ConversationUpdateRequest request)
            throws InvalidException
    {
        Conversation conversation = this.conversationService.handleGetConversationById(request.getConversationId());
        if(conversation == null) throw new InvalidException("Conversation does not exist");

        ConversationResponse response = this.conversationService.handleUpdateConversation(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/pin")
    public ResponseEntity<List<ConversationPinnedResponse>> pinConversations(
            @Valid @RequestBody ConversationIdsRequest request
    ) {
        List<ConversationPinnedResponse> result = this.conversationService.handlePinConversation(request.getConversationIds());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PatchMapping("/unpin")
    public ResponseEntity<List<ConversationPinnedResponse>> unpinConversations(
            @Valid @RequestBody ConversationIdsRequest request
    ) {
        List<ConversationPinnedResponse> result = this.conversationService.handleUnpinConversation(request.getConversationIds());
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteConversations(@Valid @RequestBody ConversationIdsRequest request) {
        this.conversationService.handleDeleteConversations(request.getConversationIds());
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversationById(@PathVariable("id") Long id)
            throws InvalidException
    {
        Conversation conversation = this.conversationService.handleGetConversationById(id);
        if(conversation == null) throw new InvalidException("Conversation does not exist");

        return ResponseEntity.ok(this.conversationService.convertConversationResponse(conversation));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ResultPaginationResponse> getMessagesByConversation(
            @PathVariable("id") Long id,
            Pageable pageable) throws InvalidException
    {
        Conversation conversation = this.conversationService.handleGetConversationById(id);
        if (conversation == null) throw new InvalidException("Conversation does not exist");

        ResultPaginationResponse result =
                this.conversationService.handleGetMessagesByConversation(id, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<ResultPaginationResponse> getAllConversations(
            @Filter Specification<Conversation> spec, Pageable pageable
    ) {
        ResultPaginationResponse response = this.conversationService.handleGetAllConversations(spec, pageable);
        return ResponseEntity.ok(response);
    }
}
