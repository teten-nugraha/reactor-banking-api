package id.ten.springreactorbanking.exceptions;

public class TransactionErrorException extends RuntimeException {

    private static final int errorCode = 100;

    public TransactionErrorException(String message) {
        super(message);
    }

    public int getErrorCode() {
        return errorCode;
    }

}