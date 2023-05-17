package id.ten.springreactorbanking.service.impl;

import id.ten.springreactorbanking.exceptions.AccountNotFoundException;
import id.ten.springreactorbanking.exceptions.InactiveAccountException;
import id.ten.springreactorbanking.exceptions.InsufficientBalanceException;
import id.ten.springreactorbanking.exceptions.TransactionErrorException;
import id.ten.springreactorbanking.models.Account;
import id.ten.springreactorbanking.models.ActivityStatus;
import id.ten.springreactorbanking.models.TransactionHistory;
import id.ten.springreactorbanking.models.TransactionType;
import id.ten.springreactorbanking.repository.AccountRepository;
import id.ten.springreactorbanking.repository.TransactionHistoryRepository;
import id.ten.springreactorbanking.service.BankService;
import id.ten.springreactorbanking.service.RunningNumberService;
import id.ten.springreactorbanking.service.TransactionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service
@Slf4j
public class BankServiceImpl implements BankService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionHistoryRepository transactionHistoryRepository;
    @Autowired private RunningNumberService runningNumberService;
    @Autowired private TransactionLogService transactionLogService;

    @Transactional
    @Override
    public Mono<Void> transfer(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount){
        Mono<Void> startLog = transactionLogService.log(TransactionType.TRANSFER, ActivityStatus.START,
                transferRemarks(sourceAccountNumber, destinationAccountNumber, amount)).log();

        Mono<String> reference = runningNumberService.generateNumber(TransactionType.TRANSFER)
                .map(number -> TransactionType.TRANSFER.name() + "-" + String.format("%05d",number));

        // https://stackoverflow.com/a/53596358 : how to validate
        Mono<Account> sourceAccount = accountRepository
                .findByAccountNumber(sourceAccountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()))
                .flatMap(validateAccount(amount));

        Mono<Account> destinationAccount = accountRepository
                .findByAccountNumber(destinationAccountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()))
                .flatMap(validateAccount(amount));

        Mono<Void> processTransfer = Mono.usingWhen(
                reference,
                transfer(sourceAccount, destinationAccount, amount),
                successLog(sourceAccountNumber, destinationAccountNumber, amount),
                errorLog(sourceAccountNumber, destinationAccountNumber, amount),
                x -> Mono.error(new TransactionErrorException("Transfer cancelled")));

        return startLog.then(processTransfer);
    }

    @Override
    public Mono<Void> withdraw(String source, BigDecimal amount) {
        return null;
    }

    @Override
    public Mono<Void> deposit(String source, BigDecimal amount) {
        return null;
    }

    @Override
    public Mono<Void> check(String source) {
        return null;
    }

    @Override
    public Mono<Void> payment(String source, String item, BigDecimal amount) {
        return null;
    }

    private String transferRemarks(String source, String destination, BigDecimal amount) {
        return "Transfer "+source+" -> "+destination+ " ["+amount+"]";
    }

    private Function<Account, Mono<Account>> validateAccount(BigDecimal amount) {
        return r -> {
            if(insufficientBalance(r, amount)) {
                return Mono.error(new InsufficientBalanceException());
            } else if(!r.getActive()) {
                return Mono.error(new InactiveAccountException());
            }
            return Mono.just(r);
        };
    }

    private boolean insufficientBalance(Account source, BigDecimal nilai) {
        return nilai.compareTo(source.getBalance()) > 0;
    }

    private Function<String, Mono<Void>> transfer(Mono<Account> sourceAccount, Mono<Account> destinationAccount, BigDecimal amount) {
        return transactionReference -> Mono.zip(sourceAccount, destinationAccount)
                .flatMapMany(tuple2 -> {
                    Account src = tuple2.getT1();
                    Account dst = tuple2.getT2();
                    String remarks = transferRemarks(src.getAccountNumber(), dst.getAccountNumber(), amount);

                    src.setBalance(src.getBalance().subtract(amount));
                    dst.setBalance(dst.getBalance().add(amount));
                    log.debug("Transfer running on thread {}", Thread.currentThread().getName());

                    return accountRepository.save(src)
                            .then(accountRepository.save(dst))
                            .thenMany(
                                    Flux.concat(
                                            saveTransactionHistory(src, remarks, amount.negate(), transactionReference),
                                            saveTransactionHistory(dst, remarks, amount, transactionReference))
                            );
                })
                .flatMap(transactionHistoryRepository::save).then();
    }

    private Mono<TransactionHistory> saveTransactionHistory(Account account, String remarks, BigDecimal amount, String reference) {
        TransactionHistory transactionHistory = new TransactionHistory();
        transactionHistory.setAccount(account);
        transactionHistory.setIdAccount(account.getId());
        transactionHistory.setTransactionType(TransactionType.TRANSFER);
        transactionHistory.setRemarks(remarks);
        transactionHistory.setAmount(amount);
        transactionHistory.setReference(reference+"-"+ account.getAccountNumber());
        return transactionHistoryRepository.save(transactionHistory);
    }

    private Function<String, Mono<Void>> successLog(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount) {
        return ref -> transactionLogService.log(TransactionType.TRANSFER, ActivityStatus.SUCCESS,
                transferRemarks(sourceAccountNumber, destinationAccountNumber, amount) + " - ["+ref+"]");
    }

    private BiFunction<String, Throwable, Mono<Void>> errorLog(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount) {
        return (d,e) -> transactionLogService.log(TransactionType.TRANSFER, ActivityStatus.FAILED,
                transferRemarks(sourceAccountNumber, destinationAccountNumber, amount) + " - [" + e.getMessage() + "]");
    }
}
