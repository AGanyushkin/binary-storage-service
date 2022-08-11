package pro.ganyushkin.binary_storage_service.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ContentTypeService {

    public String findType(String assetId) {
        try {
            var type = Files.probeContentType(Path.of(assetId));
            if (type != null) {
                return type;
            }
        } catch (IOException ignored) {}
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
