package id.ten.springreactorbanking.repository;

import id.ten.springreactorbanking.models.RunningNumber;
import id.ten.springreactorbanking.models.TransactionType;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface RunningNumberRepository extends ReactiveCrudRepository<RunningNumber, String> {
    Mono<RunningNumber> findByTransactionTypeAndResetPeriod(TransactionType transactionType, LocalDate startOfMonth);
}
