package iuh.fit.goat.dto.response.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {
    private long commentId;
    private String comment;
    private boolean reply;
    private BlogComment blog;
    private UserCommented commentedBy;
    private ParentComment parent;
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlogComment {
        private long blogId;
        private String title;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCommented {
        private long accountId;
        private String fullName;
        private String username;
        private String avatar;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentComment {
        private long commentId;
        private String comment;
        private UserCommented commentedBy;
    }
}
