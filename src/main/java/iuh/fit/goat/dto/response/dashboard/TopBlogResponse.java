package iuh.fit.goat.dto.response.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopBlogResponse {
    private Long id;
    private String title;
    private long totalLikes;
    private long totalComments;
    private long totalReads;
}
