package io.openalice.service;

import io.openalice.mapper.ExecutionMapper;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ExecutionStartupReconciler implements ApplicationRunner {

    private final ExecutionMapper executionMapper;
    private final Clock clock;

    @Autowired
    public ExecutionStartupReconciler(ExecutionMapper executionMapper) {
        this(executionMapper, Clock.systemUTC());
    }

    ExecutionStartupReconciler(ExecutionMapper executionMapper, Clock clock) {
        this.executionMapper = executionMapper;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        reconcile();
    }

    public int reconcile() {
        return executionMapper.interruptStaleRunningExecutions(clock.instant());
    }
}
