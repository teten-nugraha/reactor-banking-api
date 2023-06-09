package id.ten.springreactorbanking.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

@Data
public class Account {
    @Id private String id;
    private String accountNumber;
    private String name;
    private BigDecimal balance;
    private Boolean active;
}
