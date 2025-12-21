package iuh.fit.goat.dto.request.blog;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogCreateRequest {
    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private MultipartFile[] files;
}