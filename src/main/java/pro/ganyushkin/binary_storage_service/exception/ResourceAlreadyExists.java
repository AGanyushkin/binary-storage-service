package pro.ganyushkin.binary_storage_service.exception;

public class ResourceAlreadyExists extends Exception {
    public ResourceAlreadyExists(String bucketId) {
        super("Exists; bucketId=" + bucketId);
    }

    public ResourceAlreadyExists(String bucketId, String assetId) {
        super("Exists; bucketId=" + bucketId + "; assetId=" + assetId);
    }
}
