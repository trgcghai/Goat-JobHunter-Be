package iuh.fit.goat.entity.embeddable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BlogActivity {
    private long totalLikes = 0L;
    private long totalComments = 0L;
    private long totalReads = 0L;
    private long totalParentComments = 0L;
}
