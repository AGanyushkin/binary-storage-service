package pro.ganyushkin.binary_storage_service.controller.v1;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import pro.ganyushkin.binary_storage_service.exception.InternalStorageException;
import pro.ganyushkin.binary_storage_service.exception.ResourceAlreadyExists;
import pro.ganyushkin.binary_storage_service.exception.UndefinedResourceException;
import pro.ganyushkin.binary_storage_service.service.ContentTypeService;
import pro.ganyushkin.binary_storage_service.service.StorageService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;


import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class StorageAPITest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @MockBean
    private ContentTypeService contentTypeService;

    @Test
    public void shouldHandleGetAsset() throws Exception {
        final var bucketId = "bucket-1";
        final var assetId = "file1.txt";
        final var DATA = "result data";

        when(storageService.getAsset(bucketId, assetId)).thenReturn(
                new InputStreamResource(
                        new ByteArrayInputStream(DATA.getBytes())
                )
        );
        when(contentTypeService.findType(assetId)).thenReturn(MediaType.TEXT_PLAIN_VALUE);

        mockMvc.perform(get(buildAssetUrl(bucketId, assetId) + "?setContentDisposition=true"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(
                        "Content-Disposition", "attachment; filename=\"file1.txt\""))
                .andExpect(content().string(equalTo(DATA)))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE));
    }

    @Test
    public void shouldThrowNotFoundThenGetAsset() throws Exception {
        final var bucketId = "bucket-1";
        final var assetId = "file1.txt";

        when(storageService.getAsset(bucketId, assetId))
                .thenThrow(new UndefinedResourceException(bucketId, assetId));

        mockMvc.perform(get(buildAssetUrl(bucketId, assetId)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldThrowExceptionThenGetAsset() throws Exception {
        final var bucketId = "bucket-1";
        final var assetId = "file1.txt";

        when(storageService.getAsset(bucketId, assetId))
                .thenThrow(new InternalStorageException("come text"));

        mockMvc.perform(get(buildAssetUrl(bucketId, assetId)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldHandleAssetStore() throws Exception {
        final var bucketId = "bucket-1";
        final var assetId = "file1.txt";
        final var createBucketIfNotExists = "true";
        MockMultipartFile content = new MockMultipartFile("content", null,
                "text/plain", "bla bla bla".getBytes());

        mockMvc.perform(multipart(HttpMethod.POST, buildAssetUrl(bucketId, assetId))
                        .file(content)
                        .param("createBucketIfNotExists", createBucketIfNotExists))
                .andExpect(status().isAccepted());

        verify(storageService, times(1)).storeAsset(
                eq(bucketId),
                eq(assetId),
                any(InputStream.class),
                eq(Boolean.parseBoolean(createBucketIfNotExists)),
                eq(true));
    }

    @Test
    public void shouldHandleAssetUpdate() throws Exception {
        final var bucketId = "bucket-1";
        final var assetId = "file1.txt";
        final var createBucketIfNotExists = "true";
        MockMultipartFile content = new MockMultipartFile("content", null,
                "text/plain", "bla bla bla".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, buildAssetUrl(bucketId, assetId))
                        .file(content)
                        .param("createBucketIfNotExists", createBucketIfNotExists))
                .andExpect(status().isAccepted());

        verify(storageService, times(1)).storeAsset(
                eq(bucketId),
                eq(assetId),
                any(InputStream.class),
                eq(Boolean.parseBoolean(createBucketIfNotExists)),
                eq(false));
    }

    @Test
    public void shouldThrowNotFoundThenAssetStore() throws Exception {
        final var bucketId = "bucket-1";
        final var assetId = "file1.txt";
        final var createBucketIfNotExists = "true";

        doThrow(new UndefinedResourceException(bucketId, assetId)).when(storageService).storeAsset(
                eq(bucketId),
                eq(assetId),
                any(InputStream.class),
                eq(Boolean.parseBoolean(createBucketIfNotExists)),
                eq(true));

        MockMultipartFile content = new MockMultipartFile("content", null,
                "text/plain", "bla bla bla".getBytes());

        mockMvc.perform(multipart(HttpMethod.POST, buildAssetUrl(bucketId, assetId))
                        .file(content)
                        .param("createBucketIfNotExists", createBucketIfNotExists))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldThrowAlreadyExistsThenAssetStore() throws Exception {
        final var bucketId = "bucket-1";
        final var assetId = "file1.txt";
        final var createBucketIfNotExists = "true";

        doThrow(new ResourceAlreadyExists(bucketId, assetId)).when(storageService).storeAsset(
                eq(bucketId),
                eq(assetId),
                any(InputStream.class),
                eq(Boolean.parseBoolean(createBucketIfNotExists)),
                eq(false));

        MockMultipartFile content = new MockMultipartFile("content", null,
                "text/plain", "bla bla bla".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, buildAssetUrl(bucketId, assetId))
                        .file(content)
                        .param("createBucketIfNotExists", createBucketIfNotExists))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldCreateBucket() throws Exception {
        final var bucketId = "bucket-1";

        mockMvc.perform(put(buildBucketUrl(bucketId)))
                .andExpect(status().isAccepted());

        verify(storageService, times(1)).createBucket(
                eq(bucketId),
                eq(false)
        );
    }

    @Test
    public void shouldCreateBucketForced() throws Exception {
        final var bucketId = "bucket-1";

        mockMvc.perform(put(buildBucketUrl(bucketId) + "?force=true"))
                .andExpect(status().isAccepted());

        verify(storageService, times(1)).createBucket(
                eq(bucketId),
                eq(true)
        );
    }

    @Test
    public void shouldThrowAlreadyExistsCreateBucket() throws Exception {
        final var bucketId = "bucket-1";

        doThrow(new ResourceAlreadyExists(bucketId))
                .when(storageService).createBucket(eq(bucketId), eq(false));

        mockMvc.perform(put(buildBucketUrl(bucketId)))
                .andExpect(status().isConflict());
    }

    @Test
    public void shouldThrowExceptionCreateBucket() throws Exception {
        final var bucketId = "bucket-1";

        doThrow(new InternalStorageException("test message"))
                .when(storageService).createBucket(eq(bucketId), eq(false));

        mockMvc.perform(put(buildBucketUrl(bucketId)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void shouldGetListOfBuckets() throws Exception {
        final var buckets = List.of("bucket1", "bucket2", "bucket3");

        when(storageService.getBuckets()).thenReturn(buckets);

        mockMvc.perform(get("/api/v1/storage/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string("[\"bucket1\",\"bucket2\",\"bucket3\"]"));
    }

    @Test
    public void shouldReturnBucketList() throws Exception {
        final var bucketId = "bucket1";
        final var assets = List.of("asset1", "asset2", "asset3");

        when(storageService.getBucketList(eq(bucketId))).thenReturn(assets);

        mockMvc.perform(get("/api/v1/storage/bucket/"+bucketId+"/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string("[\"asset1\",\"asset2\",\"asset3\"]"));
    }

    @Test
    public void shouldThrowBucketList() throws Exception {
        final var bucketId = "bucket-1";

        when(storageService.getBucketList(eq(bucketId)))
                .thenThrow(new UndefinedResourceException(bucketId));

        mockMvc.perform(get("/api/v1/storage/bucket/"+bucketId+"/list"))
                .andExpect(status().isNotFound());
    }

    private String buildBucketUrl(String bucketId) {
        return "/api/v1/storage/bucket/" + bucketId;
    }

    private String buildAssetUrl(String bucketId, String assetId) {
        return "/api/v1/storage/bucket/" + bucketId + "/asset/" + assetId;
    }
}
