package iuh.fit.goat.util;

import iuh.fit.goat.exception.InvalidException;
import lombok.experimental.UtilityClass;
import org.springframework.web.multipart.MultipartFile;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class FileUploadUtil {
    public static final long MAX_FILE_SIZE = 1024 * 1024 * 2;
    public static final String FILE_PATTERN = "([^\\s]+(\\.(?i)(jpg|jpeg|png|gif|bmp|pdf|doc|docx))$)";
    public static final String DATE_FORMAT = "ddMMyyyyHHmmss";
    public static final String FILE_NAME_FORMAT = "%s_%s";
    public static final String AVATAR = "https://api.dicebear.com/7.x/avataaars/png?seed=";

    public static boolean isAllowedExtension(String fileName, String pattern){
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(fileName);
        return matcher.matches();
    }

    public static void assertAllowed(MultipartFile file, String pattern) throws InvalidException {
        long fileSize = file.getSize();
        if(fileSize > MAX_FILE_SIZE){
            throw new InvalidException("Max file size is 2MB");
        }

        final String fileName = file.getOriginalFilename().replaceAll("\\s+", "_");
        if (!isAllowedExtension(fileName, pattern)) {
            throw new InvalidException("Only jpg, jpeg, png, gif, bmp, pdf, doc, docx files are allowed");
        }
    }

    public static String getFileName(String name) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String date = dateFormat.format(System.currentTimeMillis());
        return String.format(FILE_NAME_FORMAT, name, date);
    }
}
