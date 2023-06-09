package id.ten.springreactorbanking.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDate;

@Data
public class RunningNumber {
    @Id private String id;
    private TransactionType transactionType;
    private LocalDate resetPeriod = LocalDate.now();
    private Long lastNumber;
}
