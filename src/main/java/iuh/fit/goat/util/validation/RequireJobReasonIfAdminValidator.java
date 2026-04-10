package iuh.fit.goat.util.validation;

import iuh.fit.goat.common.ActionType;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.job.JobIdsActionRequest;
import iuh.fit.goat.entity.Account;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import iuh.fit.goat.util.annotation.RequireReasonIfAdmin;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequireJobReasonIfAdminValidator implements ConstraintValidator<RequireReasonIfAdmin, JobIdsActionRequest> {
    private final UserService userService;

    public RequireJobReasonIfAdminValidator(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isValid(JobIdsActionRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (currentEmail == null) return true;

        Account currentAccount = this.userService.handleGetAccountByEmail(currentEmail);
        if (currentAccount == null) return true;

        boolean isAdmin = currentAccount.getRole().getName().equalsIgnoreCase(Role.ADMIN.getValue());
        ActionType mode = request.getMode();

        if (!isAdmin) {
            return true;
        }

        boolean requireReason = (mode == ActionType.DELETE || mode == ActionType.REJECT);

        if (requireReason && (request.getReason() == null || request.getReason().trim().isEmpty())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("reason is required for ADMIN when mode is DELETE or REJECT")
                    .addPropertyNode("reason")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

}
