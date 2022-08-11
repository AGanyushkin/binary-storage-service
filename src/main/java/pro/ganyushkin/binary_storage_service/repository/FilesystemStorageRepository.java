package pro.ganyushkin.binary_storage_service.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pro.ganyushkin.binary_storage_service.exception.InternalStorageException;
import pro.ganyushkin.binary_storage_service.exception.ResourceAlreadyExists;
import pro.ganyushkin.binary_storage_service.exception.UndefinedResourceException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Profile("FS-STORAGE")
@Slf4j
@Component
public class FilesystemStorageRepository implements BinaryStorageRepository {

    @Value("${storage.fs.root-directory}")
    private String rootDir;

    private Path rootPath;

    @PostConstruct
    private void init() throws InternalStorageException {
        rootPath = Paths.get(rootDir);
        var rootF = rootPath.toFile();
        if (!rootF.exists() || !rootF.isDirectory()) {
            if (!rootF.mkdirs()) {
                throw new InternalStorageException("Can't create storage root " + rootPath.toString());
            }
            log.info("fs root storage directory was created; {}", rootPath);
        }
    }

    @Override
    public boolean exists(String bucketId) {
        var bucketF = buildBucketFile(bucketId);
        return bucketF.exists() && bucketF.isDirectory();
    }

    @Override
    public boolean exists(String bucketId, String assetId) {
        if (!exists(bucketId)) {
            return false;
        }
        var assetF = buildAssetFile(bucketId, assetId);
        return assetF.exists() && assetF.isFile();
    }

    @Override
    public void createBucket(String bucketId) throws ResourceAlreadyExists, InternalStorageException {
        var bucketF = buildBucketFile(bucketId);
        if (bucketF.exists() && bucketF.isDirectory()) {
            throw new ResourceAlreadyExists(bucketId);
        }
        if (!bucketF.mkdir()) {
            throw new InternalStorageException("Can't create bucket " + bucketF);
        }
        log.info("bucket was created for bucketId={}", bucketId);
    }

    @Override
    public List<String> listBuckets() {
        return Arrays.stream(Objects.requireNonNull(rootPath.toFile().list()))
                .filter(name -> rootPath.resolve(name).toFile().isDirectory())
                .toList();
    }

    @Override
    public List<String> listAssets(String bucketId) throws UndefinedResourceException {
        throwIfBucketIsNotExists(bucketId);
        var assetPath = buildBucketPath(bucketId);
        return Arrays.stream(Objects.requireNonNull(assetPath.toFile().list()))
                .filter(name -> assetPath.resolve(name).toFile().isFile())
                .toList();
    }

    @Override
    public InputStream read(String bucketId, String assetId)
            throws InternalStorageException, UndefinedResourceException {
        if (!exists(bucketId, assetId)) {
            throw new UndefinedResourceException(bucketId, assetId);
        }
        try {
            return new FileInputStream(buildAssetFile(bucketId, assetId));
        } catch (FileNotFoundException e) {
            throw new InternalStorageException("Can't read asset, bucketId=" + bucketId +
                    "; assetId=" + assetId, e);
        }
    }

    @Override
    public void store(String bucketId, String assetId, InputStream data)
            throws InternalStorageException, ResourceAlreadyExists, UndefinedResourceException {
        throwIfBucketIsNotExists(bucketId);
        if (exists(bucketId, assetId)) {
            throw new ResourceAlreadyExists(bucketId, assetId);
        }
        try {
            Files.copy(data, buildAssetPath(bucketId, assetId));
            log.info("Stored asset; {}/{}", bucketId, assetId);
        } catch (IOException e) {
            throw new InternalStorageException("Can't write data", e);
        }
    }

    @Override
    public void overwrite(String bucketId, String assetId, InputStream data)
            throws InternalStorageException, UndefinedResourceException {
        throwIfBucketIsNotExists(bucketId);
        if (exists(bucketId, assetId)) {
            log.info("Remove existing asset; {}/{}", bucketId, assetId);
            if (!buildAssetFile(bucketId, assetId).delete()) {
                log.error("Can't remove asset; {}/{}", bucketId, assetId);
                throw new InternalStorageException("Can't override asset; bucketId=" + bucketId +
                        "; assetId=" + assetId);
            }
        }
        try {
            store(bucketId, assetId, data);
        } catch (ResourceAlreadyExists e) {
            log.error("Override fail; {}/{}", bucketId, assetId);
            throw new InternalStorageException("Can't override asset", e);
        }
    }

    private void throwIfBucketIsNotExists(String bucketId) throws UndefinedResourceException {
        if (!exists(bucketId)) {
            throw new UndefinedResourceException(bucketId);
        }
    }

    private Path buildBucketPath(String bucketId) {
        return rootPath.resolve(bucketId);
    }

    private File buildBucketFile(String bucketId) {
        return buildBucketPath(bucketId).toFile();
    }

    private Path buildAssetPath(String bucketId, String assetId) {
        return buildBucketPath(bucketId).resolve(assetId);
    }

    private File buildAssetFile(String bucketId, String assetId) {
        return buildAssetPath(bucketId, assetId).toFile();
    }
}
