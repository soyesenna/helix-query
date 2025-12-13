package com.soyesenna.helixquery;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class LibraryResourceRegistrationTest {

    @Test
    void registersAnnotationProcessorViaServiceLoaderResource() {
        String content = readResource("META-INF/services/javax.annotation.processing.Processor");
        assertTrue(content.contains("com.soyesenna.helixquery.processor.HelixQueryProcessor"));
    }

    @Test
    void registersAutoConfigurationViaImportsResource() {
        String content = readResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        assertTrue(content.contains("com.soyesenna.helixquery.autoconfigure.HelixQueryAutoConfiguration"));
    }

    private static String readResource(String path) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        assertNotNull(in, "Missing classpath resource: " + path);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new AssertionError("Failed to read resource: " + path, e);
        }
    }
}

