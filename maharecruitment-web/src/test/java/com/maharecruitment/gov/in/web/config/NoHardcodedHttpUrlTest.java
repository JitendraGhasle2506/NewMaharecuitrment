package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class NoHardcodedHttpUrlTest {

    @Test
    void templatesJavascriptAndApplicationConfigDoNotContainHardcodedHttpUrls() throws Exception {
        List<String> violations = new ArrayList<>();

        for (Path file : filesToScan()) {
            List<String> lines = Files.readAllLines(file);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                if (line.contains("http://") && !isAllowedNamespaceReference(line)) {
                    violations.add(resourcesDirectory().relativize(file) + ":" + (index + 1) + ": " + line.trim());
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    private boolean isAllowedNamespaceReference(String line) {
        return line.contains("http://www.thymeleaf.org");
    }

    private List<Path> filesToScan() throws IOException {
        Path resources = resourcesDirectory();
        List<Path> files = new ArrayList<>();
        collectFiles(resources.resolve("templates"), files, ".html");
        collectFiles(resources.resolve("static/js"), files, ".js");
        try (Stream<Path> applicationProperties = Files.list(resources)) {
            applicationProperties
                    .filter(path -> path.getFileName().toString().startsWith("application"))
                    .filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .forEach(files::add);
        }
        return files;
    }

    private void collectFiles(Path directory, List<Path> files, String suffix) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .forEach(files::add);
        }
    }

    private Path resourcesDirectory() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.isDirectory(userDir.resolve("src/main/resources"))) {
            return userDir.resolve("src/main/resources");
        }
        return userDir.resolve("maharecruitment-web/src/main/resources");
    }
}
