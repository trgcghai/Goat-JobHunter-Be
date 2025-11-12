package iuh.fit.goat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.entity.embeddable.BlogActivity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlogResponse {
    private long blogId;
    private String title;
    private String banner;
    private String description;
    private List<String> content;
    private List<String> tags;
    private boolean draft;
    private boolean enabled;
    private BlogActivity activity = new BlogActivity();
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private BlogAuthor author;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlogAuthor {
        private long userId;
        private String fullName;
    }
}
