package id.ten.springreactorbanking.service.impl;

import id.ten.springreactorbanking.dto.LogDataDto;
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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service
@Slf4j
@AllArgsConstructor
public class BankServiceImpl implements BankService {

    private AccountRepository accountRepository;
    private TransactionHistoryRepository transactionHistoryRepository;
    private RunningNumberService runningNumberService;
    private TransactionLogService transactionLogService;

    @Transactional
    @Override
    public Mono<Void> transfer(String sourceAccountNumber, String destinationAccountNumber, BigDecimal amount){
        Mono<Void> startLog = transactionLogService.log(TransactionType.TRANSFER, ActivityStatus.START,
                transferRemarks(sourceAccountNumber, destinationAccountNumber, amount)).log();

        Mono<String> reference = runningNumberService.generateNumber(TransactionType.TRANSFER)
                .map(number -> TransactionType.TRANSFER.name() + "-" + String.format("%05d",number));

        // https://stackoverflow.com/a/53596358 : how to validate
        Mono<Account> sourceAccount = checkAccountExist(sourceAccountNumber)
                .flatMap(validateAccount(amount));

        Mono<Account> destinationAccount = checkAccountExist(destinationAccountNumber)
                .flatMap(validateAccount(amount));

        Mono<Void> processTransfer = Mono.usingWhen(
                reference,
                transfer(sourceAccount, destinationAccount, amount),
                successLog(new LogDataDto(sourceAccountNumber, destinationAccountNumber, amount, TransactionType.TRANSFER)),
                errorLog(new LogDataDto(sourceAccountNumber, destinationAccountNumber, amount, TransactionType.TRANSFER)),
                x -> Mono.error(new TransactionErrorException("Transfer cancelled")));

        return startLog.then(processTransfer);
    }

    @Override
    public Mono<Void> withdraw(String source, BigDecimal amount) {
        return null;
    }

    @Override
    public Mono<Void> deposit(String ownerAccountNumber, BigDecimal amount) {

        Mono<Void> startLog = transactionLogService.log(
                TransactionType.DEPOSIT,
                ActivityStatus.START,
                depositRemarks(ownerAccountNumber, amount)
        );

        Mono<String> reference = runningNumberService.generateNumber(TransactionType.DEPOSIT)
                .map(number -> TransactionType.DEPOSIT.name() + "-" + String.format("%05d",number));

        Mono<Account> ownerAccount = checkAccountExist(ownerAccountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()))
                .flatMap(validateAccount(amount));

        Mono<Void> depositTransaction = Mono.usingWhen(
                reference,
                deposit(ownerAccount, amount),
                successLog(new LogDataDto(ownerAccountNumber, null, amount, TransactionType.DEPOSIT)),
                errorLog(new LogDataDto(ownerAccountNumber, null, amount, TransactionType.DEPOSIT)),
                x -> Mono.error(new TransactionErrorException("Deposit failed")));

        return startLog.then(depositTransaction);
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

    private String depositRemarks(String ower, BigDecimal amount) {
        return "Deposit " +ower+ " ["+amount+"]";
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
                                            saveTransactionHistory(src, remarks, amount.negate(), transactionReference, TransactionType.TRANSFER),
                                            saveTransactionHistory(dst, remarks, amount, transactionReference, TransactionType.TRANSFER))
                            );
                })
                .flatMap(transactionHistoryRepository::save).then();
    }

    private Mono<TransactionHistory> saveTransactionHistory(Account account, String remarks, BigDecimal amount, String reference, TransactionType transactionType) {
        TransactionHistory transactionHistory = new TransactionHistory();
        transactionHistory.setAccount(account);
        transactionHistory.setIdAccount(account.getId());
        transactionHistory.setTransactionType(transactionType);
        transactionHistory.setRemarks(remarks);
        transactionHistory.setAmount(amount);
        transactionHistory.setReference(reference+"-"+ account.getAccountNumber());
        return transactionHistoryRepository.save(transactionHistory);
    }

    private Function<String, Mono<Void>> successLog(LogDataDto logDataDto) {
        return ref -> transactionLogService.log(logDataDto.getTransactionType(), ActivityStatus.SUCCESS,
                transferRemarks(logDataDto.getSourceAccountNumber(), logDataDto.getDestinationAccountNumber(), logDataDto.getAmount()) + " - ["+ref+"]");
    }

    private BiFunction<String, Throwable, Mono<Void>> errorLog(LogDataDto logDataDto) {
        return (d,e) -> transactionLogService.log(
                logDataDto.getTransactionType(),
                ActivityStatus.FAILED,
                transferRemarks(
                        logDataDto.getSourceAccountNumber(),
                        logDataDto.getDestinationAccountNumber(),
                        logDataDto.getAmount()
                ) + " - [" + e.getMessage() + "]"
        );
    }

    private Mono<Account>checkAccountExist(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()));
    }

    private Function<String, Mono<Void>> deposit(Mono<Account> ownerAccount, BigDecimal amount) {
        return transactionReference -> ownerAccount
                .flatMap(account -> {
                    Account owner = account;
                    String remarks = depositRemarks(owner.getAccountNumber(), amount);

                    owner.setBalance(owner.getBalance().add(amount));

                    log.debug("Transfer running on thread {}", Thread.currentThread().getName());

                    return accountRepository.save(owner)
                            .then(saveTransactionHistory(owner, remarks, amount, transactionReference, TransactionType.DEPOSIT));
                })
                .flatMap(transactionHistoryRepository::save)
                .then();

    }
}
