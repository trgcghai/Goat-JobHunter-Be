package iuh.fit.goat.util.annotation;

import iuh.fit.goat.util.validation.RequireMeetingLinkIfTypeOnlineValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {RequireMeetingLinkIfTypeOnlineValidator.class})
public @interface RequireMeetingLinkIfTypeOnline {
    String message() default "Meeting link is required for online interviews";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
