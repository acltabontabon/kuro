package com.acltabontabon.kuro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KuroApplication {

    public static void main(String[] args) throws IOException {
        // SQLite creates the db file but not its parent directory; the default
        // datasource URL (jdbc:sqlite:data/kuro.db) needs data/ to exist.
        Files.createDirectories(Path.of("data"));
        SpringApplication.run(KuroApplication.class, args);
    }
}
