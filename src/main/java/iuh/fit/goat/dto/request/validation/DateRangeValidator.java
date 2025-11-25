package iuh.fit.goat.dto.request.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {
    private String startField;
    private String endField;
    private String message;

    @Override
    public void initialize(DateRange constraintAnnotation) {
        this.startField = constraintAnnotation.startField();
        this.endField = constraintAnnotation.endField();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            Field sField = value.getClass().getDeclaredField(this.startField);
            Field eField = value.getClass().getDeclaredField(this.endField);
            sField.setAccessible(true);
            eField.setAccessible(true);

            Object sObj = sField.get(value);
            Object eObj = eField.get(value);

            if (sObj == null || eObj == null) {
                return true; // other annotations handle null checks
            }

            if (!(sObj instanceof LocalDate) || !(eObj instanceof LocalDate)) {
                return true;
            }

            LocalDate start = (LocalDate) sObj;
            LocalDate end = (LocalDate) eObj;

            boolean valid = !end.isBefore(start);
            if (!valid) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(this.message)
                        .addPropertyNode(this.endField).addConstraintViolation();
            }
            return valid;
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            // if fields not present, skip validation
            return true;
        }
    }
}