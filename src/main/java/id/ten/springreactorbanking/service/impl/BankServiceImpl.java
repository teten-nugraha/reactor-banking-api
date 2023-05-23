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
import id.ten.springreactorbanking.util.RemarksUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.function.BiFunction;
import java.util.function.Function;

import static id.ten.springreactorbanking.util.RemarksUtil.depositRemarks;
import static id.ten.springreactorbanking.util.RemarksUtil.transferRemarks;
import static id.ten.springreactorbanking.util.RemarksUtil.withdrawRemarks;

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
                RemarksUtil.transferRemarks(sourceAccountNumber, destinationAccountNumber, amount)).log();

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
                successLog(
                        new LogDataDto(sourceAccountNumber, destinationAccountNumber, amount, TransactionType.TRANSFER),
                        transferRemarks(sourceAccountNumber,destinationAccountNumber,amount)
                ),
                errorLog(
                        new LogDataDto(sourceAccountNumber, destinationAccountNumber, amount, TransactionType.TRANSFER),
                        transferRemarks(sourceAccountNumber,destinationAccountNumber,amount)
                ),
                x -> Mono.error(new TransactionErrorException("Transfer cancelled")));

        return startLog.then(processTransfer);
    }

    @Override
    public Mono<Void> withdraw(String sourceAccountNumber, BigDecimal amount) {

        Mono<Void> startLog = transactionLogService.log(
                TransactionType.WITHDRAW,
                ActivityStatus.START,
                RemarksUtil.withdrawRemarks(sourceAccountNumber,amount)
        );

        Mono<String> reference = runningNumberService.generateNumber(TransactionType.WITHDRAW)
                .map(number -> TransactionType.WITHDRAW.name() + "-" + String.format("%05d",number));

        Mono<Account> ownerAccount = checkAccountExist(sourceAccountNumber)
                .flatMap(validateAccount(amount));

        Mono<Void> withdrawTransaction = Mono.usingWhen(
                reference,
                withdraw(ownerAccount, amount),
                successLog(
                        new LogDataDto(sourceAccountNumber, null, amount, TransactionType.WITHDRAW),
                        withdrawRemarks(sourceAccountNumber ,amount)
                ),
                errorLog(
                        new LogDataDto(sourceAccountNumber, null, amount, TransactionType.WITHDRAW),
                        withdrawRemarks(sourceAccountNumber, amount)
                ),
                x -> Mono.error(new TransactionErrorException("Deposit failed")));


        return startLog.then(withdrawTransaction);
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
                .flatMap(validateAccount(amount));

        Mono<Void> depositTransaction = Mono.usingWhen(
                reference,
                deposit(ownerAccount, amount),
                successLog(
                        new LogDataDto(ownerAccountNumber, null, amount, TransactionType.DEPOSIT),
                        depositRemarks(ownerAccountNumber, amount)
                ),
                errorLog(
                        new LogDataDto(ownerAccountNumber, null, amount, TransactionType.DEPOSIT),
                        depositRemarks(ownerAccountNumber, amount)
                ),
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

    private Function<Account, Mono<Account>> validateAccount(BigDecimal amount) {
        return account -> {
            if(insufficientBalance(account, amount)) {
                return Mono.error(new InsufficientBalanceException());
            } else if(!account.getActive()) {
                return Mono.error(new InactiveAccountException());
            }
            return Mono.just(account);
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
                    String remarks = RemarksUtil.transferRemarks(src.getAccountNumber(), dst.getAccountNumber(), amount);

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

    private Function<String, Mono<Void>> successLog(LogDataDto logDataDto, String remarks) {
        return ref -> {
            return transactionLogService.log(logDataDto.getTransactionType(), ActivityStatus.SUCCESS,
                    remarks + " - ["+ref+"]");
        };
    }

    private BiFunction<String, Throwable, Mono<Void>> errorLog(LogDataDto logDataDto, String remarks) {
        return (d,e) -> transactionLogService.log(
                logDataDto.getTransactionType(),
                ActivityStatus.FAILED,
                remarks + " - [" + e.getMessage() + "]"
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

    private Function<String, Mono<Void>> withdraw(Mono<Account> ownerAccount, BigDecimal amount) {
        return transactionReference -> ownerAccount
                .flatMap(account -> {
                    String remarks = withdrawRemarks(account.getAccountNumber(), amount);

                    account.setBalance(account.getBalance().subtract(amount));

                    log.debug("Transfer running on thread {}", Thread.currentThread().getName());

                    return accountRepository.save(account)
                            .then(saveTransactionHistory(account, remarks, amount.negate(), transactionReference, TransactionType.WITHDRAW));

                })
                .flatMap(transactionHistoryRepository::save)
                .then();
    }
}
