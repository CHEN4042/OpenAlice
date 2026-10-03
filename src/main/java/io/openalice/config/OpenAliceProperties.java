package io.openalice.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("openalice")
public class OpenAliceProperties {

    private Path home;
    private final Database database = new Database();
    private final Model model = new Model();

    public Path getHome() {
        return home;
    }

    public void setHome(Path home) {
        this.home = home;
    }

    public Database getDatabase() {
        return database;
    }

    public Model getModel() {
        return model;
    }

    public static class Database {
        private String fileName = "openalice.db";
        private int busyTimeoutMillis = 5_000;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public int getBusyTimeoutMillis() {
            return busyTimeoutMillis;
        }

        public void setBusyTimeoutMillis(int busyTimeoutMillis) {
            this.busyTimeoutMillis = busyTimeoutMillis;
        }
    }

    public static class Model {
        private String alias = "default";
        private String baseUrl = "https://api.openai.com/v1";
        private String modelName = "gpt-4.1-mini";
        private String apiKey;
        private Map<String, String> headers = new LinkedHashMap<>();
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofMinutes(2);

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = new LinkedHashMap<>(headers);
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
