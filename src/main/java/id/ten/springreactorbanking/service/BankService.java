package id.ten.springreactorbanking.service;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface BankService {
    Mono<Void> transfer(String source, String destination, BigDecimal amount);
    Mono<Void> withdraw(String source, BigDecimal amount);
    Mono<Void> deposit(String source, BigDecimal amount);
    Mono<Void> check(String source);
    Mono<Void> payment(String source, String item, BigDecimal amount);

}
