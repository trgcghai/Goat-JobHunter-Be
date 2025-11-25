package iuh.fit.goat.dto.request.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface DateRange {
    String message() default "endDate must be equal or after startDate";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String startField() default "startDate";
    String endField() default "endDate";
}
