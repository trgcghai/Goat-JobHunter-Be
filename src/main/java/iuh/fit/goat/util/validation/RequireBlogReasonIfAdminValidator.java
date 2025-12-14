package iuh.fit.goat.util.validation;

import iuh.fit.goat.common.ActionType;
import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.blog.BlogIdsRequest;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import iuh.fit.goat.util.annotation.RequireReasonIfAdmin;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequireBlogReasonIfAdminValidator implements ConstraintValidator<RequireReasonIfAdmin, BlogIdsRequest> {
    private final UserService userService;

    public RequireBlogReasonIfAdminValidator(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isValid(BlogIdsRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        String currentEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (currentEmail == null) return true;

        User currentUser = this.userService.handleGetUserByEmail(currentEmail);
        if (currentUser == null) return true;

        boolean isAdmin = currentUser.getRole().getName().equalsIgnoreCase(Role.ADMIN.getValue());
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

