package iuh.fit.goat.dto.request.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupInfoRequest {
    private String name;
    private MultipartFile avatar;
}