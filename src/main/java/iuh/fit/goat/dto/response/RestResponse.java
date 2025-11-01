package iuh.fit.goat.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestResponse<T> {
    private int statusCode;
    private Object message;
    private T data;
    private String error;
}
