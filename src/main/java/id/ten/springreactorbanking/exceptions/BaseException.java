package id.ten.springreactorbanking.exceptions;

import id.ten.springreactorbanking.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class BaseException {

    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<ErrorResponseDto> inactiveAccountException(InactiveAccountException ex) {
        ErrorResponseDto response = new ErrorResponseDto();
        response.setErrorCode(HttpStatus.BAD_REQUEST.value());
        response.setMessage(ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponseDto> insufficientBalanceException(InsufficientBalanceException ex) {
        ErrorResponseDto response = new ErrorResponseDto();
        response.setErrorCode(HttpStatus.BAD_REQUEST.value());
        response.setMessage(ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(TransactionErrorException.class)
    public ResponseEntity<ErrorResponseDto> transactionErrorException(TransactionErrorException ex) {
        ErrorResponseDto response = new ErrorResponseDto();
        response.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMessage(ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> accountNotFoundException(AccountNotFoundException ex) {
        ErrorResponseDto response = new ErrorResponseDto();
        response.setErrorCode(HttpStatus.NOT_FOUND.value());
        response.setMessage(ex.getMessage());

        return ResponseEntity.badRequest().body(response);
    }

}