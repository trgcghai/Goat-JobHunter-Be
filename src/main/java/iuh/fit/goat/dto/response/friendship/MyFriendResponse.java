package iuh.fit.goat.dto.response.friendship;

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
public class MyFriendResponse {
    private Long relationshipId;
    private Instant friendsSince;
    private FriendUserSnippetResponse friend;
}
