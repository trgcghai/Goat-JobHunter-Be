package iuh.fit.goat.util.validation;

import iuh.fit.goat.dto.request.auth.RegisterUserRequest;
import iuh.fit.goat.util.annotation.RequireCompanyNameIfRecruiter;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequireCompanyNameIfRecruiterValidator implements ConstraintValidator<RequireCompanyNameIfRecruiter, RegisterUserRequest> {

    public RequireCompanyNameIfRecruiterValidator() {
    }

    @Override
    public boolean isValid(RegisterUserRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        boolean isRecruiter = request.getType().equalsIgnoreCase("recruiter");
        if (!isRecruiter) {
            return true;
        }

        if(request.getCompanyName() == null || request.getCompanyName().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Company name is required for Recruiter when signing up")
                    .addPropertyNode("companyName")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
