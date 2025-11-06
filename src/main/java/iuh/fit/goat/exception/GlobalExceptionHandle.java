package iuh.fit.goat.exception;

import iuh.fit.goat.dto.response.RestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandle {

    @ExceptionHandler(Exception.class)
    ResponseEntity<RestResponse<Object>> handleAllExceptions(Exception e){
        RestResponse<Object> response = new RestResponse<Object>();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage(e.getMessage());
        response.setError("Internal Server Error");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(value = {
            InvalidException.class,
            BadCredentialsException.class,
            UsernameNotFoundException.class
    })
    ResponseEntity<RestResponse<Object>> handleAllSpecialExceptions(Exception e){
        RestResponse<Object> response = new RestResponse<Object>();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setMessage(e.getMessage());
        response.setError("Exception Occurred");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<RestResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        final List<FieldError> list = result.getFieldErrors();
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());

        if (list.isEmpty()) {
            res.setError("Validation failed");
            res.setMessage("No valid fields found");
            return ResponseEntity.badRequest().body(res);
        }

        List<String> messages = list.stream().map(l -> l.getDefaultMessage())
                .collect(Collectors.toList());
        res.setError(e.getBody().getDetail());
        res.setMessage(messages.size() > 1 ? messages : messages.getFirst());

        return ResponseEntity.badRequest().body(res);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<RestResponse<Object>> handleResourceException(Exception e){
        RestResponse<Object> response = new RestResponse<Object>();
        response.setStatusCode(HttpStatus.NOT_FOUND.value());
        response.setMessage(e.getMessage());
        response.setError("404 Not found. URL may not exist");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<RestResponse<Object>> handleUploadFileException(StorageException e){
        RestResponse<Object> response = new RestResponse<Object>();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setMessage(e.getMessage());
        response.setError("Upload file exception");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(PermissionException.class)
    ResponseEntity<RestResponse<Object>> handlePermissionException(PermissionException e){
        RestResponse<Object> response = new RestResponse<Object>();
        response.setStatusCode(HttpStatus.FORBIDDEN.value());
        response.setMessage(e.getMessage());
        response.setError("Forbidden");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
}
