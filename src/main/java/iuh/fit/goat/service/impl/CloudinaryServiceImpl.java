package iuh.fit.goat.service.impl;

import com.cloudinary.Cloudinary;
import iuh.fit.goat.dto.response.CloudinaryResponse;
import iuh.fit.goat.exception.InvalidException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryServiceImpl implements iuh.fit.goat.service.CloudinaryService {
    private final Cloudinary cloudinary;

    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Transactional
    @Override
    public CloudinaryResponse handleUploadFile(MultipartFile file, String folder, String fileName)
            throws InvalidException {
        try {
            Map<String, Object> result = this.cloudinary.uploader()
                    .upload(file.getBytes(),
                            Map.of("public_id","jobhunter/" + folder + "/" + fileName,
                                    "resource_type", "auto"
                            )
                    );
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            return CloudinaryResponse.builder()
                    .publicId(publicId).url(url)
                    .build();

        } catch (Exception e) {
            throw new InvalidException("Failed to upload file");
        }
    }

    @Override
    public void handleDeleteFile(String publicId) throws InvalidException {
        try {
            this.cloudinary.uploader().destroy(publicId, Map.of());
        } catch (Exception e){
            throw new InvalidException("Failed to delete file");
        }
    }
}