package id.ten.springreactorbanking.controller;

import id.ten.springreactorbanking.service.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class BankController {

    @Autowired
    private BankService bankService;

    @PostMapping("/transfer")
    public Mono<Void> transfer(
            @RequestParam("src") String src,
            @RequestParam("dst") String dst,
            @RequestParam("amt") BigDecimal amt
    ) {
        return bankService.transfer(src, dst, amt);
    }


}
