package io.openalice.spike;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import reactor.core.publisher.Flux;

/** Separate process used only by {@link CrashRestartSpikeTest}. */
public final class CrashChildMain {
    private CrashChildMain() {}

    public static void main(String[] args) throws Exception {
        Path database = Path.of(args[0]);
        Path ready = Path.of(args[1]);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TABLE spike_execution ("
                                + "turn_id TEXT PRIMARY KEY, user_message TEXT NOT NULL, "
                                + "status TEXT NOT NULL, assistant_message TEXT)");
            }
            try (PreparedStatement statement =
                    connection.prepareStatement(
                            "INSERT INTO spike_execution"
                                    + "(turn_id,user_message,status,assistant_message) VALUES(?,?,?,?)")) {
                statement.setString(1, "turn-crash");
                statement.setString(2, "persist before runtime");
                statement.setString(3, "RUNNING");
                statement.setString(4, null);
                statement.executeUpdate();
            }
        }

        Model neverCompletingModel =
                new Model() {
                    @Override
                    public Flux<ChatResponse> stream(
                            List<Msg> messages,
                            List<ToolSchema> tools,
                            GenerateOptions options) {
                        try {
                            Files.writeString(ready, "runtime-active");
                        } catch (Exception exception) {
                            return Flux.error(exception);
                        }
                        return Flux.never();
                    }

                    @Override
                    public String getModelName() {
                        return "crash-probe-never-model";
                    }
                };

        ReActAgent agent =
                ReActAgent.builder().name("crash-child").model(neverCompletingModel).build();
        agent.call(
                        List.of(SpikeSupport.user("persist before runtime")),
                        RuntimeContext.builder()
                                .userId("user")
                                .sessionId("runtime-only-session")
                                .build())
                .block();
    }
}
