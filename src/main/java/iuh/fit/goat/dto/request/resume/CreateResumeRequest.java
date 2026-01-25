package iuh.fit.goat.dto.request.resume;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateResumeRequest {
    @NotNull(message = "File URL is required")
    private MultipartFile fileUrl;
}
