package id.ten.springreactorbanking.controller;

import id.ten.springreactorbanking.models.Account;
import id.ten.springreactorbanking.service.BankService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class BankController {

    private BankService bankService;

    @PostMapping("/transfer")
    public Mono<Void> transfer(
            @RequestParam("src") String src,
            @RequestParam("dst") String dst,
            @RequestParam("amt") BigDecimal amt
    ) {
        return bankService.transfer(src, dst, amt);
    }

    @PostMapping("/deposit")
    public Mono<Void> deposit(
            @RequestParam("src") String src,
            @RequestParam("amt") BigDecimal amt
    ) {
        return bankService.deposit(src, amt);
    }

    @PostMapping("/withdraw")
    public Mono<Void> withdraw(
            @RequestParam("src") String src,
            @RequestParam("amt") BigDecimal amt
    ) {
        return bankService.withdraw(src, amt);
    }

    @GetMapping("/check-balance")
    public Mono<Account> withdraw(
            @RequestParam("src") String src
    ) {
        return bankService.check(src);
    }


}
