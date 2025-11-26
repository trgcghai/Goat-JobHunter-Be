package iuh.fit.goat.util.validation;

import iuh.fit.goat.dto.request.application.ApplicationIdsRequest;
import iuh.fit.goat.util.annotation.ValidApplicationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ApplicationRequestValidator implements ConstraintValidator<ValidApplicationRequest, ApplicationIdsRequest> {

    @Override
    public boolean isValid(ApplicationIdsRequest req, ConstraintValidatorContext context) {

        if (req == null) return false;

        boolean valid = true;

        context.disableDefaultConstraintViolation();

        if (req.getApplicationIds() == null || req.getApplicationIds().isEmpty()) {
            context.buildConstraintViolationWithTemplate("applicationIds is required")
                    .addPropertyNode("applicationIds").addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}