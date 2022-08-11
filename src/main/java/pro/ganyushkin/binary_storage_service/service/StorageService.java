package pro.ganyushkin.binary_storage_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import pro.ganyushkin.binary_storage_service.exception.InternalStorageException;
import pro.ganyushkin.binary_storage_service.exception.ResourceAlreadyExists;
import pro.ganyushkin.binary_storage_service.exception.UndefinedResourceException;
import pro.ganyushkin.binary_storage_service.repository.BinaryStorageRepository;

import java.io.InputStream;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class StorageService {
    private final BinaryStorageRepository storageRepository;

    public InputStreamResource getAsset(String bucket, String assetId)
        throws UndefinedResourceException, InternalStorageException {
        return new InputStreamResource(
                storageRepository.read(bucket, assetId)
        );
    }

    public void storeAsset(String bucketId, String assetId, InputStream data,
                           boolean createBucketIfNotExists, boolean override)
            throws UndefinedResourceException, InternalStorageException, ResourceAlreadyExists {
        if (createBucketIfNotExists && !storageRepository.exists(bucketId)) {
            createBucket(bucketId, true /* double check */);
        }
        if (override) {
            storageRepository.overwrite(bucketId, assetId, data);
        } else {
            storageRepository.store(bucketId, assetId, data);
        }
    }

    public void createBucket(String bucketId, boolean force)
            throws InternalStorageException, ResourceAlreadyExists {
        try {
            storageRepository.createBucket(bucketId);
        } catch (ResourceAlreadyExists e) {
            if (!force) {
                throw e;
            }
            log.info("ResourceAlreadyExists was skipped for bucketId={}; couse: force={}", bucketId, force);
        }
    }

    public List<String> getBuckets() {
        return storageRepository.listBuckets();
    }

    public List<String> getBucketList(String bucketId) throws UndefinedResourceException {
        return storageRepository.listAssets(bucketId);
    }
}
