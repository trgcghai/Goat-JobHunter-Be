package iuh.fit.goat.dto.response.user;

import iuh.fit.goat.enumeration.Visibility;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserVisibilityResponse {
    private long accountId;
    private Visibility visibility;
}
