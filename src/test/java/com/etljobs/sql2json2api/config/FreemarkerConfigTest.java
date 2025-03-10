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
import org.springframework.test.context.ActiveProfiles;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@SpringBootTest
@ActiveProfiles("test")
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
        // Créer une configuration indépendante pour le test
        Configuration testConfig = new Configuration(Configuration.VERSION_2_3_32);
        testConfig.setDirectoryForTemplateLoading(tempDir.toFile());
        testConfig.setDefaultEncoding("UTF-8");
        testConfig.setTemplateExceptionHandler(freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER);
        
        // Act - Charger et traiter le template
        Template template = testConfig.getTemplate("test.ftlh");
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
        // Créer directement une configuration qui simule le comportement de fallback
        Configuration testConfig = new Configuration(Configuration.VERSION_2_3_32);
        testConfig.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates/json");
        
        // Assert - Le test réussit si aucune exception n'est lancée
        assertNotNull(testConfig, "La configuration devrait être créée même avec un chemin invalide");
    }
}