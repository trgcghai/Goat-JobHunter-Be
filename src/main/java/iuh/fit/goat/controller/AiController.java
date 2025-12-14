package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.ai.BlogContentRequest;
import iuh.fit.goat.dto.request.ai.ChatRequest;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
//    private final AiService aiService;
//    private final ConversationService conversationService;
//
//    @PostMapping("/chat")
//    public ResponseEntity<String> chatWithAi(@Valid @RequestBody ChatRequest request) throws InvalidException {
//        if(request.getConversationId() != null) {
//            Conversation conversation = this.conversationService.handleGetConversationById(request.getConversationId());
//            if(conversation == null) throw new InvalidException("Conversation does not exist");
//        }
//
//        String aiResponse = this.aiService.chatWithAi(request);
//        return ResponseEntity.ok(aiResponse);
//    }
//
//    @PostMapping("/generate/blogs/description")
//    public ResponseEntity<String> generateBlogDescription(
//            @Valid @RequestBody BlogContentRequest request
//    ) {
//        String description = this.aiService.generateBlogDescription(request.getContent());
//        return ResponseEntity.ok(description);
//    }
//
//    @PostMapping("/generate/blogs/tags")
//    public ResponseEntity<List<String>> generateBlogTags(
//            @Valid @RequestBody BlogContentRequest request
//    ) {
//        List<String> tags = this.aiService.generateBlogTags(request.getContent());
//        return ResponseEntity.ok(tags);
//    }
}
