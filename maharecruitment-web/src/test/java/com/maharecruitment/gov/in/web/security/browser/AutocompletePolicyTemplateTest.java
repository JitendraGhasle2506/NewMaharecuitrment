package com.maharecruitment.gov.in.web.security.browser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class AutocompletePolicyTemplateTest {

    private static final Pattern FORM_TAG = Pattern.compile("<form\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTROL_TAG =
            Pattern.compile("<(?:input|textarea|select)\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUTOCOMPLETE_ATTRIBUTE =
            Pattern.compile("\\bautocomplete\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);

    @Test
    void everyServerRenderedFormDisablesAutocomplete() throws Exception {
        for (Path template : templates()) {
            String content = Files.readString(template);
            Matcher forms = FORM_TAG.matcher(content);

            while (forms.find()) {
                assertThat(forms.group())
                        .as("form autocomplete policy in %s", template)
                        .containsIgnoringCase("autocomplete=\"off\"");
            }
        }
    }

    @Test
    void everyServerRenderedControlDisablesAutocomplete() throws Exception {
        for (Path template : templates()) {
            Matcher controls = CONTROL_TAG.matcher(Files.readString(template));

            while (controls.find()) {
                Matcher attribute = AUTOCOMPLETE_ATTRIBUTE.matcher(controls.group());
                assertThat(attribute.find())
                        .as("control autocomplete policy in %s", template)
                        .isTrue();
                assertThat(attribute.group(1).toLowerCase(Locale.ROOT))
                        .as("control autocomplete value in %s", template)
                        .isEqualTo("off");
            }
        }
    }

    @Test
    void sharedHeaderLoadsDynamicControlPolicy() throws Exception {
        Path resourceRoot = resourcesDirectory();
        String header = Files.readString(resourceRoot.resolve("templates/header/header.html"));
        String policy = Files.readString(resourceRoot.resolve("static/js/autocomplete-policy.js"));

        assertThat(header).contains("/js/autocomplete-policy.js");
        assertThat(policy)
                .contains("form, input, textarea, select")
                .contains("MutationObserver")
                .contains("setAttribute('autocomplete', 'off')");
    }

    private Iterable<Path> templates() throws IOException {
        try (Stream<Path> paths = Files.walk(resourcesDirectory().resolve("templates"))) {
            return paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".html"))
                    .toList();
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
