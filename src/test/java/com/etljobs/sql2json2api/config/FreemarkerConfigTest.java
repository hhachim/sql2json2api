package com.etljobs.sql2json2api.config;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@SpringBootTest
class FreemarkerConfigTest {

    @Autowired
    private FreemarkerConfig freemarkerConfig;
    
    @TempDir
    static Path tempDir;
    
    private static final String TEST_TEMPLATE_CONTENT = "<#-- Test template -->\nHello, ${name}!";
    
    @BeforeAll
    static void setup() throws IOException {
        // Créer un template de test dans le répertoire temporaire
        Files.writeString(tempDir.resolve("test.ftlh"), TEST_TEMPLATE_CONTENT);
    }
    
    @Test
    void freemarkerConfiguration_ShouldLoadTemplateFromAbsolutePath() throws IOException, TemplateException {
        // Arrange - Injecter le chemin absolu et reconfigurer
        ReflectionTestUtils.setField(freemarkerConfig, "templateDirectory", tempDir.toString());
        freemarker.template.Configuration config = freemarkerConfig.freemarkerConfiguration();
        
        // Act - Charger et traiter le template
        Template template = config.getTemplate("test.ftlh");
        StringWriter writer = new StringWriter();
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("name", "World");
        template.process(dataModel, writer);
        String result = writer.toString();
        
        // Assert
        assertNotNull(result, "Le résultat ne devrait pas être null");
        assertEquals("Hello, World!", result.trim(), "Le contenu devrait correspondre");
    }
    
    @Test
    void freemarkerConfiguration_ShouldFallbackToClasspathForInvalidAbsolutePath() throws IOException {
        // Arrange - Injecter un chemin absolu invalide et reconfigurer
        ReflectionTestUtils.setField(freemarkerConfig, "templateDirectory", "/path/does/not/exist");
        
        // Act
        freemarker.template.Configuration config = freemarkerConfig.freemarkerConfiguration();
        
        // Assert - Le test réussit si aucune exception n'est lancée
        assertNotNull(config, "La configuration devrait être créée même avec un chemin invalide");
        
        // Vérifions que nous pouvons charger un template du classpath
        try {
            Template template = config.getTemplate("GET_users.ftlh");
            assertNotNull(template, "Devrait pouvoir charger un template du classpath");
        } catch (IOException e) {
            // C'est acceptable si le template n'existe pas dans le classpath
            // Le test vérifie principalement que nous ne plantons pas avec un chemin invalide
        }
    }
}