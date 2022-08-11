package pro.ganyushkin.binary_storage_service.repository;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pro.ganyushkin.binary_storage_service.exception.InternalStorageException;
import pro.ganyushkin.binary_storage_service.exception.ResourceAlreadyExists;
import pro.ganyushkin.binary_storage_service.exception.UndefinedResourceException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("FS-STORAGE")
@SpringBootTest
class FilesystemStorageRepositoryTest {
    @Autowired
    private FilesystemStorageRepository repository;

    @Value("${storage.fs.root-directory}")
    private String rootDir;

    @AfterEach
    public void cleanup() throws IOException {
        final var rootStorage = new File(rootDir);
        FileUtils.deleteDirectory(rootStorage);
        rootStorage.mkdir();
    }

    @AfterAll
    public static void cleanupFinal() throws IOException {
        final var rootStorage = new File("./build/test");
        FileUtils.deleteDirectory(rootStorage);
    }

    @Test
    @Order(0)
    public void shouldCreateRootDirectory() {
        assertTrue(Paths.get(rootDir).toFile().isDirectory());
    }

    @Test
    public void shouldCheckExistBucket() throws IOException {
        final var bucketId = "bucket-1";
        assertFalse(repository.exists(bucketId));

        final var bucketF = Paths.get(rootDir).resolve(bucketId).toFile();
        bucketF.mkdir();
        assertTrue(repository.exists(bucketId));
    }

    @Test
    public void shouldCheckExistAsset() throws IOException {
        final var bucketId = "bucket-2";
        final var assetId = "asset-1";
        assertFalse(repository.exists(bucketId));

        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        final var bucketF = bucketP.toFile();
        bucketF.mkdir();
        final var assetP = bucketP.resolve(assetId);
        final var assetF = assetP.toFile();
        assetF.createNewFile();
        assertTrue(repository.exists(bucketId, assetId));
    }

    @Test
    public void shouldCreateBucket() throws InternalStorageException, ResourceAlreadyExists {
        final var bucketId = "bucket-3";
        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        assertFalse(bucketP.toFile().exists());

        repository.createBucket(bucketId);
        assertTrue(bucketP.toFile().exists());
    }

    @Test
    public void shouldThrowAlreadyExistsForCreateBucket() throws IOException {
        final var bucketId = "bucket-1";
        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        FileUtils.forceMkdir(bucketP.toFile());
        assertThrows(ResourceAlreadyExists.class, () ->
                repository.createBucket(bucketId));
    }

    @Test
    public void shouldListBuckets() throws IOException {
        final var bucketId1 = "bucket-list-1";
        final var bucketId2 = "bucket-list-2";

        assertEquals(List.of(), repository.listBuckets());

        FileUtils.forceMkdir(Paths.get(rootDir).resolve(bucketId1).toFile());
        FileUtils.forceMkdir(Paths.get(rootDir).resolve(bucketId2).toFile());

        assertEquals(List.of(bucketId1, bucketId2), repository.listBuckets().stream().sorted().toList());
    }

    @Test
    public void shouldListAssets() throws IOException, UndefinedResourceException {
        final var bucketId = "bucket-1";
        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        FileUtils.forceMkdir(bucketP.toFile());
        final var fileName1 = "file1.ext";
        final var fileName2 = "file2.ext";

        assertEquals(List.of(), repository.listAssets(bucketId));

        bucketP.resolve(fileName1).toFile().createNewFile();
        bucketP.resolve(fileName2).toFile().createNewFile();

        assertEquals(List.of(fileName1, fileName2),
                repository.listAssets(bucketId).stream().sorted().toList());
    }

    @Test
    public void shouldThrowNoBucketForListAssets() {
        final var bucketId = "bucket-1";
        assertThrows(UndefinedResourceException.class, () ->
                repository.listAssets(bucketId));
    }

    @Test
    public void shouldReadAsset() throws IOException, InternalStorageException, UndefinedResourceException {
        final var bucketId = "bucket-1";
        final var assetId = "file.txt";
        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        final var assetP = bucketP.resolve(assetId);
        final var TEST_CONTENT = "example text\n in file\n";
        FileUtils.forceMkdir(bucketP.toFile());
        try(var file = new FileWriter(assetP.toFile())) {
            file.write(TEST_CONTENT);
        }

        var stream = repository.read(bucketId, assetId);
        var content = new String(stream.readAllBytes());
        stream.close();
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void shouldThrowNoBucketForReadIfNoBucket() {
        final var bucketId = "bucket-1";
        final var assetId = "file.txt";

        assertThrows(UndefinedResourceException.class, () ->
                repository.read(bucketId, assetId));
    }

    @Test
    public void shouldThrowNoBucketForReadIfNoAsset() throws IOException {
        final var bucketId = "bucket-1";
        final var assetId = "file.txt";
        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        FileUtils.forceMkdir(bucketP.toFile());

        assertThrows(UndefinedResourceException.class, () ->
                repository.read(bucketId, assetId));
    }

    @Test
    public void shouldStoreAsset() throws IOException, InternalStorageException, UndefinedResourceException,
            ResourceAlreadyExists {
        final var bucketId = "bucket-1";
        final var assetId = "example-file.txt";
        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        final var assetP = bucketP.resolve(assetId);
        final var TEST_CONTENT = "example text\n in file\n";
        FileUtils.forceMkdir(bucketP.toFile());

        repository.store(bucketId, assetId, new ByteArrayInputStream(TEST_CONTENT.getBytes()));

        assertEquals(TEST_CONTENT, Files.readString(assetP));
    }

    @Test
    public void shouldThrowAlreadyExists() throws IOException {
        final var bucketId = "bucket-1";
        final var assetId = "file.txt";
        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        final var assetP = bucketP.resolve(assetId);
        final var TEST_CONTENT = "example text\n in file\n";
        FileUtils.forceMkdir(bucketP.toFile());
        try(var file = new FileWriter(assetP.toFile())) {
            file.write(TEST_CONTENT + TEST_CONTENT);
        }

        assertThrows(ResourceAlreadyExists.class, () ->
                repository.store(bucketId, assetId, new ByteArrayInputStream(TEST_CONTENT.getBytes())));
    }

    @Test
    public void shouldThrowNoBucketForStore() {
        final var bucketId = "bucket-1";
        final var assetId = "file.txt";
        final var TEST_CONTENT = "example text\n in file\n";

        assertThrows(UndefinedResourceException.class, () ->
                repository.store(bucketId, assetId, new ByteArrayInputStream(TEST_CONTENT.getBytes())));
    }

    @Test
    public void shouldOverrideAsset() throws IOException, InternalStorageException, UndefinedResourceException {
        final var bucketId = "bucket-1";
        final var assetId = "file.txt";
        final var bucketP = Paths.get(rootDir).resolve(bucketId);
        final var assetP = bucketP.resolve(assetId);
        final var TEST_CONTENT = "example text\n in file\n";
        FileUtils.forceMkdir(bucketP.toFile());
        try(var file = new FileWriter(assetP.toFile())) {
            file.write(TEST_CONTENT + TEST_CONTENT);
        }

        repository.overwrite(bucketId, assetId, new ByteArrayInputStream(TEST_CONTENT.getBytes()));

        assertEquals(TEST_CONTENT, Files.readString(assetP));
    }
}
