package com.etljobs.sql2json2api.service.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;

@SpringBootTest
@ActiveProfiles("test")
class TemplateLoaderAbsolutePathTest {

    @Autowired
    private TemplateLoader templateLoader;
    
    @TempDir
    static Path tempDir;
    
    private static final String TEST_TEMPLATE_CONTENT = "<#-- @api-route: /api/test @api-method: GET -->\n" +
            "{\n" +
            "  \"test\": \"${result.value}\"\n" +
            "}";
    
    @BeforeAll
    static void setup() throws IOException {
        // Créer un template de test dans le répertoire temporaire
        Files.writeString(tempDir.resolve("TEST_template.ftlh"), TEST_TEMPLATE_CONTENT);
        
        // Créer un sous-répertoire pour tester la structure des dossiers
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Files.writeString(subDir.resolve("SUBDIR_template.ftlh"), 
                "<#-- Subdir template -->\n{\"sub\":\"${result.value}\"}");
    }
    
    @Test
    void templateExists_ShouldReturnTrue_WhenTemplateExistsInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateLoader, "templateDirectory", tempDir.toString());
        
        // Act & Assert
        assertTrue(templateLoader.templateExists("TEST_template.ftlh"), 
                "Le template devrait exister dans le système de fichiers");
    }
    
    @Test
    void templateExists_ShouldReturnFalse_WhenTemplateDoesNotExistInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateLoader, "templateDirectory", tempDir.toString());
        
        // Act & Assert
        assertFalse(templateLoader.templateExists("NONEXISTENT_template.ftlh"), 
                "Le template ne devrait pas exister");
    }
    
    @Test
    void loadTemplateContent_ShouldReturnContent_WhenTemplateExistsInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateLoader, "templateDirectory", tempDir.toString());
        
        // Act
        String content = templateLoader.loadTemplateContent("TEST_template.ftlh");
        
        // Assert
        assertNotNull(content, "Le contenu ne devrait pas être null");
        assertEquals(TEST_TEMPLATE_CONTENT, content, "Le contenu devrait correspondre");
    }
    
    @Test
    void loadTemplateContent_ShouldThrowException_WhenTemplateDoesNotExistInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateLoader, "templateDirectory", tempDir.toString());
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            templateLoader.loadTemplateContent("NONEXISTENT_template.ftlh");
        }, "Devrait lancer une exception pour un template inexistant");
    }
    
    @Test
    void loadTemplateContent_ShouldWorkWithSubdirectories() throws IOException {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateLoader, "templateDirectory", tempDir.toString());
        
        // Act
        boolean exists = templateLoader.templateExists("subdir/SUBDIR_template.ftlh");
        
        // Assert
        assertTrue(exists, "Le template dans un sous-répertoire devrait être trouvé");
        
        // Charger le contenu
        String content = templateLoader.loadTemplateContent("subdir/SUBDIR_template.ftlh");
        assertNotNull(content, "Le contenu ne devrait pas être null");
        assertTrue(content.contains("Subdir template"), "Le contenu devrait correspondre");
    }
    
    @Test
    void buildTemplatePath_ShouldCombineDirectoryAndTemplateName() {
        // Arrange - Injecter le chemin absolu
        String absolutePath = "/test/templates";
        ReflectionTestUtils.setField(templateLoader, "templateDirectory", absolutePath);
        
        // Act
        String path = templateLoader.buildTemplatePath("test.ftlh");
        
        // Assert
        assertEquals("/test/templates/test.ftlh", path);
    }
}