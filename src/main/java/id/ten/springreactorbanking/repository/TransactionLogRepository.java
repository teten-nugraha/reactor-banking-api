package id.ten.springreactorbanking.repository;

import id.ten.springreactorbanking.models.TransactionLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLogRepository extends ReactiveCrudRepository<TransactionLog, String> {
}
