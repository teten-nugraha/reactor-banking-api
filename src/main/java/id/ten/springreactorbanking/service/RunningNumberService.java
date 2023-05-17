package id.ten.springreactorbanking.service;

import id.ten.springreactorbanking.models.RunningNumber;
import id.ten.springreactorbanking.models.TransactionType;
import id.ten.springreactorbanking.repository.RunningNumberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
@Slf4j
public class RunningNumberService {

    private RunningNumberRepository runningNumberRepository;

    public RunningNumberService(RunningNumberRepository runningNumberRepository) {
        this.runningNumberRepository = runningNumberRepository;
    }

    public Mono<Long> generateNumber(TransactionType transactionType) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        return runningNumberRepository.findByTransactionTypeAndResetPeriod(transactionType, startOfMonth)
                .defaultIfEmpty(createRunningNumber(transactionType, startOfMonth))
                .map(r -> {
                    r.setLastNumber(r.getLastNumber() + 1);
                    return r;
                })
                .flatMap(runningNumberRepository::save)
                .map(r -> r.getLastNumber());
    }

    private RunningNumber createRunningNumber(TransactionType transactionType, LocalDate resetPeriode) {
        RunningNumber runningNumber = new RunningNumber();
        runningNumber.setLastNumber(0L);
        runningNumber.setResetPeriod(resetPeriode);
        runningNumber.setTransactionType(transactionType);
        return runningNumber;
    }

}
