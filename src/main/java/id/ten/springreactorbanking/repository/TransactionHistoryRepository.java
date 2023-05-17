package id.ten.springreactorbanking.repository;

import id.ten.springreactorbanking.models.TransactionHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionHistoryRepository extends ReactiveCrudRepository<TransactionHistory, String> {
}
