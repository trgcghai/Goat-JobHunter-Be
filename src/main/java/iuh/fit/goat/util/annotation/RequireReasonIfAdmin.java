package iuh.fit.goat.util.annotation;

import iuh.fit.goat.util.validation.RequireReasonIfAdminValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequireReasonIfAdminValidator.class)
public @interface RequireReasonIfAdmin {
    String message() default "reason is required for ADMIN";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
