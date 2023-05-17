package id.ten.springreactorbanking.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ErrorResponseDto {
    private int errorCode;
    private String message;
}
