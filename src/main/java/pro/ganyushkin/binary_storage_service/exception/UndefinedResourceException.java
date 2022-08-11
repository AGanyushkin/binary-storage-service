package pro.ganyushkin.binary_storage_service.exception;

public class UndefinedResourceException extends Exception {

    public UndefinedResourceException(String bucketId) {
        super("Undefined resource; bucketId=" + bucketId);
    }

    public UndefinedResourceException(String bucketId, String assetId) {
        super("Undefined resource; bucketId=" + bucketId + "; assetId=" + assetId);
    }
}
