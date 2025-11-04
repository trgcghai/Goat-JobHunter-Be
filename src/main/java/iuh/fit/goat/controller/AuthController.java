package iuh.fit.goat.controller;

import iuh.fit.goat.common.Role;
import iuh.fit.goat.dto.request.LoginRequest;
import iuh.fit.goat.dto.response.LoginResponse;
import iuh.fit.goat.dto.response.RecruiterResponse;
import iuh.fit.goat.entity.Applicant;
import iuh.fit.goat.entity.Recruiter;
import iuh.fit.goat.entity.User;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.RecruiterService;
import iuh.fit.goat.service.UserService;
import iuh.fit.goat.util.SecurityUtil;
import iuh.fit.goat.util.annotation.ApiMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;

    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        LoginResponse loginResponse = new LoginResponse();
        User currentUser = this.userService.handleGetUserByEmail(loginRequest.getEmail());
        if (currentUser != null) {
            if(currentUser.isEnabled()) {
                LoginResponse.UserLogin userLogin = new LoginResponse.UserLogin(
                        currentUser.getUserId(), currentUser.getContact().getEmail(),
                        currentUser.getFullName(), currentUser.getUsername(), currentUser.getAvatar(),
                        currentUser instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue(),
                        true,
                        currentUser.getRole(), currentUser.getSavedJobs(), currentUser.getFollowedRecruiters(),
                        currentUser.getActorNotifications()
                );
                loginResponse.setUser(userLogin);
            } else {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message", "Account is locked"
                        )
                );
            }
        }
        String accessToken = this.securityUtil.createAccessToken(loginRequest.getEmail(), loginResponse);
        loginResponse.setAccessToken(accessToken);

        String refreshToken = this.securityUtil.createRefreshToken(loginRequest.getEmail(), loginResponse);
        this.userService.handleUpdateRefreshToken(loginRequest.getEmail(), refreshToken);

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtRefreshToken)
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResponse);
    }

    @GetMapping("/auth/account")
    @ApiMessage("Get information account")
    public ResponseEntity<LoginResponse.UserGetAccount> getCurrentAccount(){
        String currentEmail = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        User currentUser = this.userService.handleGetUserByEmail(currentEmail);
        LoginResponse.UserLogin currentUserLogin = new LoginResponse.UserLogin();
        LoginResponse.UserGetAccount userGetAccount = new LoginResponse.UserGetAccount();
        if(currentUser != null) {
            currentUserLogin.setUserId(currentUser.getUserId());
            currentUserLogin.setFullName(currentUser.getFullName());
            currentUserLogin.setUsername(currentUser.getUsername());
            currentUserLogin.setEmail(currentUser.getContact().getEmail());
            currentUserLogin.setAvatar(currentUser.getAvatar());
            currentUserLogin.setType(currentUser instanceof Applicant ? Role.APPLICANT.getValue() : Role.RECRUITER.getValue());
            currentUserLogin.setRole(currentUser.getRole());
            currentUserLogin.setSavedJobs(currentUser.getSavedJobs());
            currentUserLogin.setFollowedRecruiters(currentUser.getFollowedRecruiters());
            currentUserLogin.setActorNotifications(currentUser.getActorNotifications());

            userGetAccount.setUser(currentUserLogin);
        }

        return ResponseEntity.status(HttpStatus.OK).body(userGetAccount);
    }

    @GetMapping("/auth/refresh")
    @ApiMessage("Refresh account")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "refreshToken", defaultValue = "missingValue")
            String refreshToken
    ) throws InvalidException {
        if(refreshToken.equalsIgnoreCase("missingValue")) {
            throw new InvalidException("You don't have a refresh token at cookie");
        }

        Jwt jwt = this.securityUtil.checkValidRefreshToken(refreshToken);
        String email = jwt.getSubject();

        User currentUser = this.userService.handleGetUserByRefreshTokenAndEmail(refreshToken, email);
        if(currentUser == null){
            throw new InvalidException("User not found");
        }
        if(!currentUser.isEnabled()) {
            throw new InvalidException("Account is locked");
        }

        LoginResponse loginResponse = new LoginResponse();

        LoginResponse.UserLogin currentUserLogin = new LoginResponse.UserLogin();
        currentUserLogin.setUserId(currentUser.getUserId());
        currentUserLogin.setFullName(currentUser.getFullName());
        currentUserLogin.setEmail(currentUser.getContact().getEmail());
        currentUserLogin.setRole(currentUser.getRole());
        currentUserLogin.setSavedJobs(currentUser.getSavedJobs());
        currentUserLogin.setFollowedRecruiters(currentUser.getFollowedRecruiters());
        currentUserLogin.setActorNotifications(currentUser.getActorNotifications());

        loginResponse.setUser(currentUserLogin);

        String newAccessToken = this.securityUtil.createAccessToken(email, loginResponse);
        loginResponse.setAccessToken(newAccessToken);

        String newRefreshToken = this.securityUtil.createRefreshToken(email, loginResponse);
        this.userService.handleUpdateRefreshToken(email, newRefreshToken);

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtRefreshToken)
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResponse);
    }

    @PostMapping("/auth/logout")
    @ApiMessage("Logout account")
    public ResponseEntity<Void> logout() throws InvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        if(email.isEmpty()){
            throw new InvalidException("Access token is invalid");
        }

        this.userService.handleUpdateRefreshToken(email, null);

        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", null)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(null);
    }
}
