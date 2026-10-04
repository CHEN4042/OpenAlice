package io.openalice.service;

import io.openalice.common.log.OpenAliceLog;
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
        int interrupted = executionMapper.interruptStaleRunningExecutions(clock.instant());
        if (interrupted > 0) {
            OpenAliceLog.event("execution.interrupted")
                    .message("Stale running executions were interrupted during startup")
                    .field("count", interrupted)
                    .warn();
        }
        return interrupted;
    }
}
