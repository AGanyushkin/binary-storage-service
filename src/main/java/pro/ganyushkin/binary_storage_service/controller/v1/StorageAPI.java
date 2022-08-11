package pro.ganyushkin.binary_storage_service.controller.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pro.ganyushkin.binary_storage_service.exception.InternalStorageException;
import pro.ganyushkin.binary_storage_service.exception.ResourceAlreadyExists;
import pro.ganyushkin.binary_storage_service.exception.UndefinedResourceException;
import pro.ganyushkin.binary_storage_service.service.ContentTypeService;
import pro.ganyushkin.binary_storage_service.service.StorageService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/v1/storage/")
public class StorageAPI {
    private final StorageService storageService;
    private final ContentTypeService contentTypeService;

    @RequestMapping(method = RequestMethod.GET,
            path = "/bucket/{bucket}/asset/{assetId}")
    public ResponseEntity<InputStreamResource> getAssetByBucketAndId(
            @PathVariable String bucket,
            @PathVariable String assetId,
            @RequestParam(required = false, defaultValue = "false") boolean setContentDisposition) {
        log.info("get asset {}/{}", bucket, assetId);
        try {
            HttpHeaders responseHeaders = new HttpHeaders();
            if (setContentDisposition) {
                responseHeaders.set("Content-Disposition", "attachment; filename=\"" + assetId + "\"");
            }
            responseHeaders.set("Content-Type", contentTypeService.findType(assetId));
            return ResponseEntity.ok()
                    .headers(responseHeaders)
                    .body(storageService.getAsset(bucket, assetId));
        } catch (InternalStorageException e) {
            log.error("Getter exception", e);
            return ResponseEntity.internalServerError().build();
        } catch (UndefinedResourceException e) {
            log.warn("Undefined asset for {}/{}", bucket, assetId);
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(method = { RequestMethod.PUT, RequestMethod.POST },
            path = "/bucket/{bucketId}/asset/{assetId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> storeAsset(
            @PathVariable String bucketId,
            @PathVariable String assetId,
            @RequestParam(required = false, defaultValue = "false") boolean createBucketIfNotExists,
            @RequestPart MultipartFile content,
            HttpServletRequest request) {
        var override = RequestMethod.valueOf(request.getMethod()).equals(RequestMethod.POST);
        log.info("store asset bucket={}; assetId={}; createBucketIfNotExists={}; override={}",
                bucketId, assetId, createBucketIfNotExists, override);
        try {
            storageService.storeAsset(bucketId, assetId, content.getInputStream(),
                    createBucketIfNotExists, override);
            return ResponseEntity.accepted().build();
        } catch (IOException | InternalStorageException e) {
            log.error("Getter exception", e);
            return ResponseEntity.internalServerError().build();
        } catch (UndefinedResourceException e) {
            log.warn("Undefined asset for {}/{}", bucketId, assetId);
            return ResponseEntity.notFound().build();
        } catch (ResourceAlreadyExists e) {
            log.error("Can't save bucketId="+bucketId+"; assetId="+assetId, e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/bucket/{bucketId}")
    public ResponseEntity<String> createBucket(
            @PathVariable String bucketId,
            @RequestParam(required = false, defaultValue = "false") boolean force
    ) {
        log.info("create bucket bucket={}", bucketId);
        try {
            storageService.createBucket(bucketId, force);
            return ResponseEntity.accepted().build();
        } catch (InternalStorageException e) {
            log.error("Getter exception", e);
            return ResponseEntity.internalServerError().build();
        } catch (ResourceAlreadyExists e) {
            log.error("Can't create bucket bucketId=" + bucketId, e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<String>> getBucketList() {
        return ResponseEntity.ok(storageService.getBuckets());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/bucket/{bucketId}/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<String>> getAssetList(@PathVariable String bucketId) {
        try {
            return ResponseEntity.ok(storageService.getBucketList(bucketId));
        } catch (UndefinedResourceException e) {
            log.warn("Undefined bucket bucketId={}", bucketId);
            return ResponseEntity.notFound().build();
        }
    }
}
