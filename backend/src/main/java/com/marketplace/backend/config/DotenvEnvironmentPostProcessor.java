package com.marketplace.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrega um arquivo .env (KEY=VALUE por linha) para dentro do Environment do Spring,
 * para desenvolvimento local. Procura em ./.env e ./backend/.env.
 *
 * Prioridade baixa: variáveis de ambiente reais do sistema continuam ganhando,
 * então em produção (Railway) o .env é ignorado e valem as env vars do host.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final List<String> CANDIDATES = List.of(".env", "backend/.env");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String candidate : CANDIDATES) {
            Path path = Path.of(candidate);
            if (!Files.isRegularFile(path)) {
                continue;
            }
            Map<String, Object> values = parse(path);
            if (!values.isEmpty()) {
                environment.getPropertySources().addLast(new MapPropertySource("dotenv:" + candidate, values));
            }
            return; // usa o primeiro que encontrar
        }
    }

    private Map<String, Object> parse(Path path) {
        Map<String, Object> values = new HashMap<>();
        try {
            for (String raw : Files.readAllLines(path)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if ((value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2)
                        || (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("[dotenv] não consegui ler " + path + ": " + e.getMessage());
        }
        return values;
    }
}
