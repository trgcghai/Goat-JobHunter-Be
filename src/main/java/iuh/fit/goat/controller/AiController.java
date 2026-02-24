package iuh.fit.goat.controller;

import iuh.fit.goat.dto.request.ai.BlogContentRequest;
import iuh.fit.goat.dto.request.ai.ChatRequest;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;

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
}
