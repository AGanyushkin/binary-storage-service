package pro.ganyushkin.binary_storage_service.repository;

import pro.ganyushkin.binary_storage_service.exception.InternalStorageException;
import pro.ganyushkin.binary_storage_service.exception.ResourceAlreadyExists;
import pro.ganyushkin.binary_storage_service.exception.UndefinedResourceException;

import java.io.InputStream;
import java.util.List;

/**
 * This interface described asset storage
 */
public interface BinaryStorageRepository {

    /**
     * Check if bucket exists or not
     * @param bucketId - virtual bucket name
     * @return true if bucket exits or false if not exists
     */
    boolean exists(String bucketId);

    /**
     * Check if specified asset in bucket exists
     * @param bucketId - virtual bucket name
     * @param assetId - uniq (in bucket) asset identificator
     * @return true if asset exits or false if not exists
     */
    boolean exists(String bucketId, String assetId);

    /**
     * Create bucket if not exits
     * @param bucketId - virtual bucket name
     * @throws ResourceAlreadyExists if bucket already exists
     * @throws InternalStorageException in other internal exceptions
     */
    void createBucket(String bucketId)
            throws ResourceAlreadyExists, InternalStorageException;

    /**
     * Get bucket list which are exists in storage instance
     * @return list if buckets
     */
    List<String> listBuckets();

    /**
     * Get list of assets which are exists in specified bucket
     * @param bucketId - virtual bucket name
     * @return list of assetId
     * @throws UndefinedResourceException if bucket not exists
     */
    List<String> listAssets(String bucketId)
            throws UndefinedResourceException;

    /**
     * Read and return asset content
     * @param bucketId - virtual bucket name
     * @param assetId - uniq (in bucket) asset identificator
     * @return asset data input stream
     * @throws InternalStorageException - if some errors happened in storage
     * @throws UndefinedResourceException - if bucket or asset is not exits
     */
    InputStream read(String bucketId, String assetId)
            throws InternalStorageException, UndefinedResourceException;

    /**
     * Store asset in bucket
     * @param bucketId - virtual bucket name
     * @param assetId - uniq (in bucket) asset identificator
     * @param data - asset data input stream
     * @throws InternalStorageException - if some errors happened in storage
     * @throws ResourceAlreadyExists if asset already exists
     * @throws UndefinedResourceException - if bucket is not exits
     */
    void store(String bucketId, String assetId, InputStream data)
            throws InternalStorageException, ResourceAlreadyExists, UndefinedResourceException;

    /**
     * Store asset in bucket, overwrite asset if asset already exists
     * @param bucketId - virtual bucket name
     * @param assetId - uniq (in bucket) asset identificator
     * @param data - asset data input stream
     * @throws InternalStorageException - if some errors happened in storage
     * @throws UndefinedResourceException - if bucket is not exits
     */
    void overwrite(String bucketId, String assetId, InputStream data)
            throws InternalStorageException, UndefinedResourceException;
}
