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
    public static final String AVATAR_MALE = "male.png";
    public static final String AVATAR_FEMALE = "female.png";

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
}
