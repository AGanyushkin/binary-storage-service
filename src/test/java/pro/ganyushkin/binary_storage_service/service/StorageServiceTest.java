package pro.ganyushkin.binary_storage_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import pro.ganyushkin.binary_storage_service.exception.InternalStorageException;
import pro.ganyushkin.binary_storage_service.exception.ResourceAlreadyExists;
import pro.ganyushkin.binary_storage_service.exception.UndefinedResourceException;
import pro.ganyushkin.binary_storage_service.repository.BinaryStorageRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class StorageServiceTest {
    final String bucketId = "bucket-1";
    final String assetId = "asset.ext";
    final String DATA = "bla bla bla";

    @MockBean
    private BinaryStorageRepository repository;

    @Autowired
    private StorageService storageService;

    @Test
    public void getAssetTest() throws InternalStorageException, UndefinedResourceException, IOException {
        when(repository.read(bucketId, assetId)).thenReturn(new ByteArrayInputStream(DATA.getBytes()));
        var res = storageService.getAsset(bucketId, assetId);
        assertEquals(DATA, new String(res.getInputStream().readAllBytes()));
    }

    @Test
    public void storeAssetTest() throws InternalStorageException, UndefinedResourceException, ResourceAlreadyExists {
        final var is = new ByteArrayInputStream(DATA.getBytes());
        when(repository.exists(bucketId)).thenReturn(true);
        storageService.storeAsset(bucketId, assetId, is, false, false);

        verify(repository, times(0)).exists(bucketId);

        verify(repository, times(0)).createBucket(bucketId);
        verify(repository, times(1)).store(bucketId, assetId, is);
        verify(repository, times(0)).overwrite(bucketId, assetId, is);
    }

    @Test
    public void storeAssetTestCreateBucket() throws InternalStorageException, UndefinedResourceException, ResourceAlreadyExists {
        final var is = new ByteArrayInputStream(DATA.getBytes());
        when(repository.exists(bucketId)).thenReturn(false);
        storageService.storeAsset(bucketId, assetId, is, true, false);

        verify(repository, times(1)).exists(bucketId);

        verify(repository, times(1)).createBucket(bucketId);
        verify(repository, times(1)).store(bucketId, assetId, is);
        verify(repository, times(0)).overwrite(bucketId, assetId, is);
    }

    @Test
    public void storeAssetTestCheckForcedBucketCreation() throws InternalStorageException,
            UndefinedResourceException, ResourceAlreadyExists {
        final var is = new ByteArrayInputStream(DATA.getBytes());

        when(repository.exists(bucketId)).thenReturn(false);
        doThrow(new ResourceAlreadyExists(bucketId)).when(repository).createBucket(bucketId);

        storageService.storeAsset(bucketId, assetId, is, true, false);

        verify(repository, times(1)).exists(bucketId);

        verify(repository, times(1)).createBucket(bucketId);
        verify(repository, times(1)).store(bucketId, assetId, is);
        verify(repository, times(0)).overwrite(bucketId, assetId, is);
    }

    @Test
    public void storeAssetTestWithoutBucket() throws InternalStorageException, UndefinedResourceException, ResourceAlreadyExists {
        final var is = new ByteArrayInputStream(DATA.getBytes());
        when(repository.exists(bucketId)).thenReturn(false);
        storageService.storeAsset(bucketId, assetId, is, false, false);
        verify(repository, times(0)).createBucket(bucketId);
        verify(repository, times(1)).store(bucketId, assetId, is);
        verify(repository, times(0)).overwrite(bucketId, assetId, is);
    }

    @Test
    public void storeAssetTestOverride() throws InternalStorageException, UndefinedResourceException, ResourceAlreadyExists {
        final var is = new ByteArrayInputStream(DATA.getBytes());
        when(repository.exists(bucketId)).thenReturn(true);
        storageService.storeAsset(bucketId, assetId, is, false, true);
        verify(repository, times(0)).createBucket(bucketId);
        verify(repository, times(0)).store(bucketId, assetId, is);
        verify(repository, times(1)).overwrite(bucketId, assetId, is);
    }

    @Test
    public void createBucketTest() throws InternalStorageException, ResourceAlreadyExists {
        storageService.createBucket(bucketId, false);
        verify(repository, times(1)).createBucket(bucketId);
    }

    @Test
    public void createBucketTestThrow() throws InternalStorageException, ResourceAlreadyExists {
        doThrow(new ResourceAlreadyExists(bucketId)).when(repository).createBucket(bucketId);
        assertThrows(ResourceAlreadyExists.class, () -> storageService.createBucket(bucketId, false));
    }

    @Test
    public void createBucketTestForced() throws InternalStorageException, ResourceAlreadyExists {
        doThrow(new ResourceAlreadyExists(bucketId)).when(repository).createBucket(bucketId);
        storageService.createBucket(bucketId, true);
    }

    @Test
    public void getBucketsTest() {
        final var buckets = List.of("bucket1", "bucket2", "bucket3");
        when(repository.listBuckets()).thenReturn(buckets);
        assertEquals(buckets, storageService.getBuckets());
    }

    @Test
    public void getBucketListTest() throws UndefinedResourceException {
        final var assets = List.of("file1.ext", "file2.ext", "file3.ext");
        when(repository.listAssets(bucketId)).thenReturn(assets);
        assertEquals(assets, storageService.getBucketList(bucketId));
    }
}
