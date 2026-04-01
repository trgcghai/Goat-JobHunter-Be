package iuh.fit.goat.util;

import iuh.fit.goat.dto.response.StorageResponse;
import iuh.fit.goat.exception.InvalidException;
import iuh.fit.goat.service.StorageService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public class BasicUtil {
    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateVerificationCode() {
        int code = secureRandom.nextInt(900_000) + 100_000;
        return String.valueOf(code);
    }

    public static boolean checkValidNumber(String str) {
        Pattern pattern = Pattern.compile("^\\d+$");
        return pattern.matcher(str).matches();
    }

    public static String uploadImage(MultipartFile file, String folder, StorageService storageService) throws InvalidException {
        StorageResponse response = storageService.handleUploadFile(file, folder);
        if (response == null || response.getUrl() == null) {
            throw new InvalidException("Failed to upload file");
        }
        return response.getUrl();
    }

    public static MultipartFile convertToMultipartFile(String image) throws IOException {
        ClassPathResource resource = new ClassPathResource("images/" + image);
        return new MockMultipartFile(
                "file",
                image,
                "image/png",
                resource.getInputStream()
        );
    }

    public static String detectMimeType(String url) throws InvalidException {
        String lower = url.toLowerCase();

        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }

        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        if (lower.endsWith(".doc")) {
            return "application/msword";
        }

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (lower.endsWith(".png")) {
            return "image/png";
        }

        throw new InvalidException("Unsupported file type");
    }
}
