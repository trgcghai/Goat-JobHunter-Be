package iuh.fit.goat.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlogContentRequest {
    @NotBlank(message = "Content is required")
    private String content;
}