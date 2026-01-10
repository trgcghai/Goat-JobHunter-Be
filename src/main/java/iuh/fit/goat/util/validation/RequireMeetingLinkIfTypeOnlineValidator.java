package iuh.fit.goat.util.validation;

import iuh.fit.goat.dto.request.interview.CreateInterviewRequest;
import iuh.fit.goat.enumeration.InterviewType;
import iuh.fit.goat.util.annotation.RequireMeetingLinkIfTypeOnline;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequireMeetingLinkIfTypeOnlineValidator implements ConstraintValidator<RequireMeetingLinkIfTypeOnline, CreateInterviewRequest> {

    public RequireMeetingLinkIfTypeOnlineValidator() {}

    @Override
    public boolean isValid(CreateInterviewRequest createInterviewRequest, ConstraintValidatorContext constraintValidatorContext) {
        if (createInterviewRequest == null || createInterviewRequest.getType() == null) return true;

        boolean isOnline = createInterviewRequest.getType() == InterviewType.VIDEO
                || createInterviewRequest.getType() == InterviewType.ONLINE_TEST;

        if (isOnline) {
            String meetingLink = createInterviewRequest.getMeetingLink();
            if (meetingLink == null || meetingLink.trim().isEmpty()) {
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext.buildConstraintViolationWithTemplate("Meeting link is required for ONLINE interviews")
                        .addPropertyNode("meetingLink")
                        .addConstraintViolation();
                return false;
            }
            return true;
        }

        return true;
    }
}
