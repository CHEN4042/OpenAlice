package io.openalice.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.sqlite.SQLiteConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfiguration {

    @Bean(destroyMethod = "close")
    DataSource dataSource(OpenAliceProperties properties) throws IOException {
        Path dataDirectory = properties.getHome().resolve("data").toAbsolutePath().normalize();
        Files.createDirectories(dataDirectory);

        Path databasePath = dataDirectory.resolve(properties.getDatabase().getFileName()).normalize();
        if (!databasePath.startsWith(dataDirectory)) {
            throw new IllegalArgumentException("Database file must remain inside OPENALICE_HOME/data");
        }

        SQLiteConfig sqlite = new SQLiteConfig();
        sqlite.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqlite.enforceForeignKeys(true);
        sqlite.setBusyTimeout(properties.getDatabase().getBusyTimeoutMillis());

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("openalice-sqlite");
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + databasePath);
        hikari.setDataSourceProperties(sqlite.toProperties());
        hikari.setMaximumPoolSize(4);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTimeout(5_000);
        return new HikariDataSource(hikari);
    }
}
