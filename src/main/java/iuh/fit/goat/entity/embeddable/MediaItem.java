package iuh.fit.goat.entity.embeddable;

import iuh.fit.goat.enumeration.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaItem {

    private String url;
    private MediaType mediaType;
    private String mimeType;
    private Long sizeBytes;
    private Integer displayOrder;

    @DynamoDbAttribute("url")
    public String getUrl() {
        return url;
    }

    @DynamoDbAttribute("mediaType")
    public MediaType getMediaType() {
        return mediaType;
    }

    @DynamoDbAttribute("mimeType")
    public String getMimeType() {
        return mimeType;
    }

    @DynamoDbAttribute("sizeBytes")
    public Long getSizeBytes() {
        return sizeBytes;
    }

    @DynamoDbAttribute("displayOrder")
    public Integer getDisplayOrder() {
        return displayOrder;
    }
}