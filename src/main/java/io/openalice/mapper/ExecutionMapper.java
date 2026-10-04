package io.openalice.mapper;

import io.openalice.model.Execution;
import io.openalice.model.ExecutionStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExecutionMapper {

    @Insert("""
            INSERT INTO executions(execution_id, status, created_at, updated_at)
            VALUES (
                #{id, typeHandler=io.openalice.mapper.UuidStringTypeHandler},
                #{status},
                #{createdAt, typeHandler=io.openalice.mapper.InstantStringTypeHandler},
                #{updatedAt, typeHandler=io.openalice.mapper.InstantStringTypeHandler}
            )
            """)
    int insert(Execution execution);

    @Select("""
            SELECT execution_id, status, created_at, updated_at
            FROM executions
            WHERE execution_id = #{executionId, typeHandler=io.openalice.mapper.UuidStringTypeHandler}
            """)
    @ConstructorArgs({
        @Arg(column = "execution_id", javaType = UUID.class, typeHandler = UuidStringTypeHandler.class),
        @Arg(column = "status", javaType = ExecutionStatus.class),
        @Arg(column = "created_at", javaType = Instant.class, typeHandler = InstantStringTypeHandler.class),
        @Arg(column = "updated_at", javaType = Instant.class, typeHandler = InstantStringTypeHandler.class)
    })
    Optional<Execution> findById(@Param("executionId") UUID executionId);

    @Update("""
            UPDATE executions
            SET status = #{targetStatus},
                updated_at = #{updatedAt, typeHandler=io.openalice.mapper.InstantStringTypeHandler}
            WHERE execution_id = #{executionId, typeHandler=io.openalice.mapper.UuidStringTypeHandler}
              AND status = 'RUNNING'
            """)
    int transitionFromRunning(
            @Param("executionId") UUID executionId,
            @Param("targetStatus") ExecutionStatus targetStatus,
            @Param("updatedAt") Instant updatedAt);

    @Update("""
            UPDATE executions
            SET status = 'INTERRUPTED',
                updated_at = #{updatedAt, typeHandler=io.openalice.mapper.InstantStringTypeHandler}
            WHERE status = 'RUNNING'
            """)
    int interruptStaleRunningExecutions(@Param("updatedAt") Instant updatedAt);
}
