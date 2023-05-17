package id.ten.springreactorbanking.exceptions;

public class InsufficientBalanceException extends RuntimeException {

    private static final String MESSAGE = "Insufficient Balance";
    private static final int errorCode = 100;
    public InsufficientBalanceException() {
        super(MESSAGE);
    }

    public int getErrorCode() {
        return errorCode;
    }

}
