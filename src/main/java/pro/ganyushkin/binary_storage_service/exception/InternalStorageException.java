package pro.ganyushkin.binary_storage_service.exception;

import java.io.FileNotFoundException;

public class InternalStorageException extends Exception {
    public InternalStorageException(String message) {
        super(message);
    }

    public InternalStorageException(String message, FileNotFoundException e) {
        super(message, e);
    }

    public InternalStorageException(String message, Exception e) {
        super(message, e);
    }
}
