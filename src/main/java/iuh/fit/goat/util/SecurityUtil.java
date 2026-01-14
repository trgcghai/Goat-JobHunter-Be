package iuh.fit.goat.util;

import com.nimbusds.jose.util.Base64;
import iuh.fit.goat.dto.response.auth.LoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SecurityUtil {

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;
    @Value("${minhdat.jwt.base64-secret}")
    private String jwtKey;
    @Value("${minhdat.jwt.access-token-validity-in-seconds}")
    private long jwtAccessToken;
    @Value("${minhdat.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshToken;

    private static final SecureRandom secureRandom = new SecureRandom();
    private final JwtEncoder jwtEncoder;

    public SecurityUtil(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String createAccessToken(String email, LoginResponse loginResponse) {
        Instant now = Instant.now();
        Instant validity = now.plus(this.jwtAccessToken, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("accountId", loginResponse.getAccountId())
                .claim("email", loginResponse.getEmail())
                .claim("fullName", loginResponse.getFullName())
                .claim("type", loginResponse.getType())
                .claim("role", loginResponse.getRole().getName())
                .build();


        JwsHeader header = JwsHeader.with(SecurityUtil.JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String createRefreshToken(String email, LoginResponse loginResponse){
        Instant now = Instant.now();
        Instant validity = now.plus(this.jwtRefreshToken, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(email)
                .claim("accountId", loginResponse.getAccountId())
                .build();

        JwsHeader header = JwsHeader.with(SecurityUtil.JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, JWT_ALGORITHM.getName());
    }

    public Jwt checkValidToken(String refreshToken) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(getSecretKey())
                .macAlgorithm(JWT_ALGORITHM)
                .build();

        try{
            return decoder.decode(refreshToken);
        } catch (Exception e) {
            log.error(">>> JWT error: " + e.getMessage());
            throw e;
        }
    }

    public long getRemainingTime(String token) {
        Jwt jwt = checkValidToken(token);

        Instant expiration = jwt.getExpiresAt();
        Instant now = Instant.now();
        if (expiration == null) return 0;
        long seconds = Duration.between(now, expiration).toSeconds();

        return Math.max(seconds, 0);
    }

    public static String getCurrentUserEmail() {
        return getCurrentUserLogin().isPresent() ? getCurrentUserLogin().get() : "";
    }


    public static Optional<String> getCurrentUserLogin(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(extractPrincipal(authentication));
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private static String extractPrincipal(Authentication authentication){
        if(authentication == null){
            return null;
        } else if(authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if(authentication.getPrincipal() instanceof Jwt jwt){
            return jwt.getSubject();
        } else if(authentication.getPrincipal() instanceof String s){
            return s;
        }
        return null;
    }

    public static String generateVerificationCode() {
        int code = secureRandom.nextInt(900_000) + 100_000;
        return String.valueOf(code);
    }

    public static boolean checkValidNumber(String str) {
        Pattern pattern = Pattern.compile("^\\d+$");
        return pattern.matcher(str).matches();
    }

}
