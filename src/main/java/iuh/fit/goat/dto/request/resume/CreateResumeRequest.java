package iuh.fit.goat.dto.request.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateResumeRequest {
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Resume URL is required")
    private String fileUrl;
    @NotBlank(message = "Resume name is required")
    private String fileName;
    @NotNull(message = "Resume size is required")
    private Long fileSize;
    @NotBlank(message = "Resume summary is required")
    private String summary;
}
