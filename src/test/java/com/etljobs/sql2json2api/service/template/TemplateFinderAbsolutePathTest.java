package com.etljobs.sql2json2api.service.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.model.SqlFile;

@SpringBootTest
class TemplateFinderAbsolutePathTest {

    @Autowired
    private TemplateFinder templateFinder;
    
    @TempDir
    static Path tempDir;
    
    @BeforeAll
    static void setup() throws IOException {
        // Créer les templates de test
        Files.writeString(tempDir.resolve("GET_users.ftlh"), 
                "<#-- @api-route: /api/users -->\n{\"test\": true}");
        Files.writeString(tempDir.resolve("POST_order.ftlh"), 
                "<#-- @api-route: /api/orders -->\n{\"test\": true}");
        Files.writeString(tempDir.resolve("users.ftlh"), 
                "<#-- Alternative template -->\n{\"test\": true}");
        Files.writeString(tempDir.resolve("GET_generic.ftlh"), 
                "<#-- Generic GET template -->\n{\"test\": true}");
        Files.writeString(tempDir.resolve("default.ftlh"), 
                "<#-- Default template -->\n{\"test\": true}");
    }
    
    @Test
    void templateExists_ShouldDetectTemplateInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateFinder, "templateDirectory", tempDir.toString());
        
        // Act & Assert
        assertTrue(templateFinder.templateExists("GET_users.ftlh"), 
                "Devrait trouver GET_users.ftlh");
        assertTrue(templateFinder.templateExists("POST_order.ftlh"), 
                "Devrait trouver POST_order.ftlh");
        assertFalse(templateFinder.templateExists("NONEXISTENT.ftlh"), 
                "Ne devrait pas trouver un template inexistant");
    }
    
    @Test
    void findTemplateForSqlFile_ShouldFindMatchingTemplateInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateFinder, "templateDirectory", tempDir.toString());
        
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        // Act
        Optional<String> templateName = templateFinder.findTemplateForSqlFile(sqlFile);
        
        // Assert
        assertTrue(templateName.isPresent(), "Devrait trouver un template");
        assertEquals("GET_users.ftlh", templateName.get(), "Le nom du template devrait correspondre");
    }
    
    @Test
    void findAlternativeTemplate_ShouldFindBaseNameTemplateInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateFinder, "templateDirectory", tempDir.toString());
        
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_nonexistent.sql")
                .httpMethod("GET")
                .baseName("users") // Correspond à users.ftlh
                .build();
        
        // Act
        Optional<String> templateName = templateFinder.findAlternativeTemplate(sqlFile);
        
        // Assert
        assertTrue(templateName.isPresent(), "Devrait trouver un template alternatif");
        assertEquals("users.ftlh", templateName.get(), "Devrait trouver le template basé sur le nom de base");
    }
    
    @Test
    void findAlternativeTemplate_ShouldFindGenericTemplateInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateFinder, "templateDirectory", tempDir.toString());
        
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_something.sql")
                .httpMethod("GET")
                .baseName("something") // Ne correspond à aucun template spécifique
                .build();
        
        // Act
        Optional<String> templateName = templateFinder.findAlternativeTemplate(sqlFile);
        
        // Assert
        assertTrue(templateName.isPresent(), "Devrait trouver un template alternatif");
        assertEquals("GET_generic.ftlh", templateName.get(), "Devrait trouver le template générique GET");
    }
    
    @Test
    void findAlternativeTemplate_ShouldFindDefaultTemplateInFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateFinder, "templateDirectory", tempDir.toString());
        
        SqlFile sqlFile = SqlFile.builder()
                .fileName("DELETE_something.sql")
                .httpMethod("DELETE")
                .baseName("something") // Ne correspond à aucun template spécifique ou générique
                .build();
        
        // Act
        Optional<String> templateName = templateFinder.findAlternativeTemplate(sqlFile);
        
        // Assert
        assertTrue(templateName.isPresent(), "Devrait trouver un template alternatif");
        assertEquals("default.ftlh", templateName.get(), "Devrait trouver le template par défaut");
    }
    
    @Test
    void findBestMatchingTemplate_ShouldTryConventionsThenAlternatives() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateFinder, "templateDirectory", tempDir.toString());
        
        SqlFile existingFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        SqlFile nonExistingFile = SqlFile.builder()
                .fileName("DELETE_something.sql")
                .httpMethod("DELETE")
                .baseName("something")
                .build();
        
        // Act & Assert
        Optional<String> result1 = templateFinder.findBestMatchingTemplate(existingFile);
        assertTrue(result1.isPresent(), "Devrait trouver un template");
        assertEquals("GET_users.ftlh", result1.get(), "Devrait trouver le template exact");
        
        Optional<String> result2 = templateFinder.findBestMatchingTemplate(nonExistingFile);
        assertTrue(result2.isPresent(), "Devrait trouver un template alternatif");
        assertEquals("default.ftlh", result2.get(), "Devrait trouver le template par défaut");
    }
    
    @Test
    void listAvailableTemplates_ShouldListTemplatesFromFileSystem() {
        // Arrange - Injecter le chemin absolu
        ReflectionTestUtils.setField(templateFinder, "templateDirectory", tempDir.toString());
        
        // Act
        List<String> templates = templateFinder.listAvailableTemplates();
        
        // Assert
        assertNotNull(templates, "La liste ne devrait pas être nulle");
        assertEquals(5, templates.size(), "Devrait trouver 5 templates");
        assertTrue(templates.contains("GET_users.ftlh"), "Devrait contenir GET_users.ftlh");
        assertTrue(templates.contains("POST_order.ftlh"), "Devrait contenir POST_order.ftlh");
        assertTrue(templates.contains("users.ftlh"), "Devrait contenir users.ftlh");
        assertTrue(templates.contains("GET_generic.ftlh"), "Devrait contenir GET_generic.ftlh");
        assertTrue(templates.contains("default.ftlh"), "Devrait contenir default.ftlh");
    }
}