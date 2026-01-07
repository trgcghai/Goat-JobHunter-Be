package iuh.fit.goat.util.annotation;

import iuh.fit.goat.util.validation.RequireCompanyNameIfRecruiterValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = RequireCompanyNameIfRecruiterValidator.class)
@Documented
public @interface RequireCompanyNameIfRecruiter {
    String message() default "Recruiter must provide a company name";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
