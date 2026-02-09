package iuh.fit.goat.entity.embeddable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SenderInfo {

    private Long accountId;
    private String fullName;
    private String username;
    private String email;
    private String avatar;

    @DynamoDbAttribute("accountId")
    public Long getAccountId() {
        return accountId;
    }

    @DynamoDbAttribute("fullName")
    public String getFullName() {
        return fullName;
    }

    @DynamoDbAttribute("username")
    public String getUsername() {
        return username;
    }

    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    @DynamoDbAttribute("avatar")
    public String getAvatar() {
        return avatar;
    }
}