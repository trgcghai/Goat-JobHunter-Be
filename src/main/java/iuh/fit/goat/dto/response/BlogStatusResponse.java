package iuh.fit.goat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogStatusResponse {
    private Long blogId;
    private boolean enabled;
}
