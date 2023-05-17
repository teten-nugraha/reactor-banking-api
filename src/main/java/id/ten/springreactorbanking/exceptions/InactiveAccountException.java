package id.ten.springreactorbanking.exceptions;

public class InactiveAccountException extends RuntimeException {

    private static final String MESSAGE = "Inactive Account";
    private static final int errorCode = 100;

    public InactiveAccountException() {
        super(MESSAGE);
    }

    public int getErrorCode() {
        return errorCode;
    }

}