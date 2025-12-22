package iuh.fit.goat.dto.response.blog;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.entity.embeddable.BlogActivity;
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
    private List<String> images;
    private String content;
    private List<String> tags;
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
        private long accountId;
        private String fullName;
        private String username;
        private String avatar;
        private String bio;
        private String headline;
        private String coverPhoto;
    }
}
