package pro.ganyushkin.binary_storage_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ContentTypeServiceTest {

    @Autowired
    private ContentTypeService typeService;

    @Test
    public void shouldFindType() {
        assertEquals("image/png", typeService.findType("image.png"));
        assertEquals("image/jpeg", typeService.findType("image.jpeg"));
        assertEquals("image/jpeg", typeService.findType("image.jpg"));
        assertEquals("text/plain", typeService.findType("file.txt"));
        assertEquals("application/octet-stream", typeService.findType("file.dmg"));
        assertEquals("application/octet-stream", typeService.findType("file-1"));
    }
}
