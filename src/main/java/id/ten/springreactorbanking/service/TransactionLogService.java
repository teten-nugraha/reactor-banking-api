package id.ten.springreactorbanking.service;

import id.ten.springreactorbanking.models.ActivityStatus;
import id.ten.springreactorbanking.models.TransactionType;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionLogService {

    private static final String SQL_INSERT = "insert into transaction_log" +
            "(transaction_type, activity_status, activity_time, remarks) " +
            "values (:type, :status, :time, :remarks)";

    private final ConnectionFactory connectionFactory;
    private DatabaseClient databaseClient;
    private ReactiveTransactionManager reactiveTransactionManager;

    public TransactionLogService(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void init() {
        reactiveTransactionManager = new R2dbcTransactionManager(connectionFactory);
        databaseClient = DatabaseClient.create(connectionFactory);
    }

    public Mono<Void> log(TransactionType transactionType, ActivityStatus activityStatus, String remarks) {
        DefaultTransactionDefinition tdf = new DefaultTransactionDefinition();
        tdf.setName(this.getClass().getName());
        tdf.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        TransactionalOperator rxtx = TransactionalOperator.create(reactiveTransactionManager, tdf);
        log.debug("Transaction log running on thread {}", Thread.currentThread().getName());
        return databaseClient.sql(SQL_INSERT)
                .bind("type", transactionType.name())
                .bind("status", activityStatus.name())
                .bind("time", LocalDateTime.now())
                .bind("remarks", remarks)
                .then()
                .as(rxtx::transactional);
    }
}
