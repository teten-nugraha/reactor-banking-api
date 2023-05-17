package id.ten.springreactorbanking.exceptions;

public class AccountNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Account Not Found";
    private static final int errorCode = 100;

    public AccountNotFoundException() {
        super(MESSAGE);
    }

    public int getErrorCode() {
        return errorCode;
    }

}
