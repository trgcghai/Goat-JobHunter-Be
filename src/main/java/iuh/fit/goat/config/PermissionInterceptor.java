package iuh.fit.goat.config;

import iuh.fit.goat.entity.Permission;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.PermissionException;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;

@Slf4j
@Transactional
public class PermissionInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String bestPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String requestUri = request.getRequestURI();
        String path = (bestPattern != null && !"/**".equals(bestPattern)) ? bestPattern : requestUri;

        String method = request.getMethod();

        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        if(!email.isEmpty()) {
            User user = this.userService.handleGetUserByEmail(email);
            if(user != null) {
                Role role = user.getRole();

                log.info("Check role of user: {}", role);

                // Check permission
                if(role != null) {

                    // Allow all if user is admin
                    if (iuh.fit.goat.common.Role.ADMIN.getValue().equalsIgnoreCase(role.getName())) {
                        return true;
                    }

                    List<Permission> permissions = user.getRole().getPermissions();
                    boolean hasPermission = permissions.stream().anyMatch(
                            p -> p.getApiPath().equals(path) && p.getMethod().equals(method)
                    );
                    if(!hasPermission) {
                        throw new PermissionException("You don't have permission to access");
                    }
                } else {
                    throw new PermissionException("You don't have permission to access");
                }
            }
        }

        return true;
    }
}
