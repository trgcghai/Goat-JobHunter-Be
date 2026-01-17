package iuh.fit.goat.util.validation;

import iuh.fit.goat.entity.Recruiter;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import iuh.fit.goat.util.annotation.RequireAddressIfRecruiter;

public class RequireAddressIfRecruiterValidator implements ConstraintValidator<RequireAddressIfRecruiter, Object> {

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext constraintValidatorContext) {
        if(o instanceof Recruiter recruiter && (recruiter.getAddresses() == null || recruiter.getAddresses().isEmpty())){
            constraintValidatorContext.disableDefaultConstraintViolation();
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate(
                            "Recruiter must provide an address or info about address is not empty")
                    .addPropertyNode("address")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}

