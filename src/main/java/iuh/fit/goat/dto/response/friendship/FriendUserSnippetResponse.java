package iuh.fit.goat.dto.response.friendship;

import com.fasterxml.jackson.annotation.JsonInclude;
import iuh.fit.goat.enumeration.Visibility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FriendUserSnippetResponse {
    private Long accountId;
    private String fullName;
    private String username;
    private String avatar;
    private String headline;
    private String bio;
    private String coverPhoto;
    private Visibility visibility;
}
