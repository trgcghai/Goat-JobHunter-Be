package iuh.fit.goat.util.validation;

import iuh.fit.goat.common.Status;
import iuh.fit.goat.dto.request.ApplicationIdsRequest;
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

        if (req.getStatus() == null || req.getStatus().isBlank()) {
            context.buildConstraintViolationWithTemplate("status is required")
                    .addPropertyNode("status").addConstraintViolation();
            valid = false;
            return valid;
        }

        if (Status.ACCEPTED.getValue().equalsIgnoreCase(req.getStatus())) {
            if (req.getInterviewDate() == null) {
                context.buildConstraintViolationWithTemplate("interviewDate is required when status is accepted")
                        .addPropertyNode("interviewDate").addConstraintViolation();
                valid = false;
            }
            if (req.getInterviewType() == null || req.getInterviewType().isBlank()) {
                context.buildConstraintViolationWithTemplate("interviewType is required when status is accepted")
                        .addPropertyNode("interviewType").addConstraintViolation();
                valid = false;
            }
            if (req.getLocation() == null || req.getLocation().isBlank()) {
                context.buildConstraintViolationWithTemplate("location is required when status is accepted")
                        .addPropertyNode("location").addConstraintViolation();
                valid = false;
            }
        }

        if (Status.REJECTED.getValue().equalsIgnoreCase(req.getStatus())) {
            if (req.getReason() == null || req.getReason().isBlank()) {
                context.buildConstraintViolationWithTemplate("reason is required when status is rejected")
                        .addPropertyNode("reason").addConstraintViolation();
                valid = false;
            }
        }

        return valid;
    }
}