package id.ten.springreactorbanking.dto;

import id.ten.springreactorbanking.models.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
@AllArgsConstructor
public class LogDataDto {
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private TransactionType transactionType;
}
