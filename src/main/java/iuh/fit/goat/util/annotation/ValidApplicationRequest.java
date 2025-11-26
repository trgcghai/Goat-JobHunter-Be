package iuh.fit.goat.util.annotation;

import iuh.fit.goat.util.validation.ApplicationRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ApplicationRequestValidator.class)
public @interface ValidApplicationRequest {
    String message() default "Invalid application request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
