package iuh.fit.goat.util;

import iuh.fit.goat.exception.InvalidException;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public class FileUploadUtil {
    public static final long MAX_FILE_SIZE = 1024 * 1024 * 2L;
    public static final String FILE_PATTERN = "([^\\s]+(\\.(?i)(jpg|jpeg|png|pdf|doc|docx))$)";
    public static final String DATE_FORMAT = "ddMMyyyyHHmmss";
    public static final String FILE_NAME_FORMAT = "%s_%s";
    public static final String AVATAR = "https://api.dicebear.com/7.x/avataaars/png?seed=";
    public static final String AVATAR_RECRUITER
            = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=http://";

    public static boolean isAllowedExtension(String fileName){
        Matcher matcher = Pattern.compile(FILE_PATTERN, Pattern.CASE_INSENSITIVE).matcher(fileName);
        return matcher.matches();
    }

    public static void assertAllowed(MultipartFile file) throws InvalidException {
        long fileSize = file.getSize();
        if(fileSize > MAX_FILE_SIZE){
            throw new InvalidException("Max file size is 2MB");
        }

        final String fileName = Objects.requireNonNull(file.getOriginalFilename()).replaceAll("\\s+", "_");
        if (!isAllowedExtension(fileName)) {
            throw new InvalidException("Only jpg, jpeg, png, pdf, doc, docx files are allowed");
        }
    }

    public static String getFileName(String name) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String date = dateFormat.format(System.currentTimeMillis());
        return String.format(FILE_NAME_FORMAT, name, date);
    }

    public static String getAvatarRecruiter(String username) {
        return AVATAR_RECRUITER + username + ".com&size=128";
    }

    public static List<String> getEmailApplicants() throws IOException {
        ClassPathResource resource = new ClassPathResource("text/email.txt");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return br.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
        }

    }

    public static List<String> getUsernameRecruiters() throws IOException {
        ClassPathResource resource = new ClassPathResource("text/recruiter.txt");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return br.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .toList();
        }

    }
}
