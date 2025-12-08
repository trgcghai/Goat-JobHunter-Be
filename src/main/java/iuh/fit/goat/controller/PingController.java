package iuh.fit.goat.controller;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PingController {
    @PermitAll
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Server is running");
    }

    @PermitAll
    @GetMapping("/clear-cookies")
    public ResponseEntity<String> clearCookies(HttpServletRequest request, HttpServletResponse response) {

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                Cookie deleteCookie = new Cookie(cookie.getName(), "");
                deleteCookie.setPath("/");
                deleteCookie.setMaxAge(0);       // XÓA cookie
                deleteCookie.setHttpOnly(true);  // Tùy chọn
                deleteCookie.setSecure(false);   // Để dev thì false
                response.addCookie(deleteCookie);
            }
        }

        return ResponseEntity.ok("All cookies cleared");
    }

    @PermitAll
    @GetMapping("/uuid")
    public ResponseEntity<String> createUuid(@CookieValue(value = "guestId", required = false) String guestId) {
        if (guestId != null) {
            log.info("✅ Guest id is set to {}", guestId);
            return ResponseEntity.ok(guestId);
        }
        return ResponseEntity.ok().body("UUID cookie not found");
    }
}
