package io.openalice.spike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class CrashRestartSpikeTest {

    @Test
    @Timeout(20)
    void hardKilledJvmIsReconciledFromOpenAliceDurableFactsOnly(@TempDir Path temp)
            throws Exception {
        Path database = temp.resolve("crash-probe.sqlite");
        Path ready = temp.resolve("runtime-active.marker");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path");
        Process child =
                new ProcessBuilder(
                                java,
                                "-cp",
                                classpath,
                                CrashChildMain.class.getName(),
                                database.toString(),
                                ready.toString())
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .start();

        assertTrue(waitForFile(ready, Duration.ofSeconds(8)), "child runtime must become active");
        assertTrue(child.isAlive());
        child.destroyForcibly();
        assertTrue(child.waitFor(5, TimeUnit.SECONDS));
        assertFalse(child.isAlive());

        Row before = read(database);
        assertEquals("persist before runtime", before.userMessage());
        assertEquals("RUNNING", before.status());
        assertNull(before.assistantMessage());

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                Statement statement = connection.createStatement()) {
            assertEquals(
                    1,
                    statement.executeUpdate(
                            "UPDATE spike_execution SET status='INTERRUPTED' WHERE status='RUNNING'"));
        }

        Row after = read(database);
        assertEquals("persist before runtime", after.userMessage());
        assertEquals("INTERRUPTED", after.status());
        assertNull(after.assistantMessage());

        System.out.printf(
                "EVIDENCE C kill=destroyForcibly exit=%d beforeStatus=%s afterStatus=%s "
                        + "userMessageRetained=%s assistantCommitted=%s agentScopeStateRequired=false%n",
                child.exitValue(),
                before.status(),
                after.status(),
                before.userMessage() != null,
                after.assistantMessage() != null);
    }

    private static Row read(Path database) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery(
                                "SELECT user_message,status,assistant_message FROM spike_execution")) {
            assertTrue(result.next());
            return new Row(result.getString(1), result.getString(2), result.getString(3));
        }
    }

    private static boolean waitForFile(Path file, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!Files.exists(file) && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        return Files.exists(file);
    }

    private record Row(String userMessage, String status, String assistantMessage) {}
}
