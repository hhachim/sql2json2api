package com.etljobs.sql2json2api.service.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.exception.TemplateProcessingException;

class TemplateLoaderTest {

    private TemplateLoader templateLoader;
    
    @BeforeEach
    void setUp() {
        templateLoader = new TemplateLoader();
        ReflectionTestUtils.setField(templateLoader, "templateDirectory", "templates/json");
    }
    
    @Test
    void buildTemplatePath_ShouldCombineDirectoryAndTemplateName() {
        // Act
        String path = templateLoader.buildTemplatePath("test.ftlh");
        
        // Assert
        assertEquals("templates/json/test.ftlh", path);
    }
    
    @Test
    void loadTemplateContent_ShouldThrowException_WhenTemplateDoesNotExist() {
        // Cette méthode utilise PowerMockito pour simuler ClassPathResource
        // Dans un test simple, on vérifie simplement que l'exception est bien lancée
        // lorsque le template n'existe pas réellement
        
        assertThrows(TemplateProcessingException.class, () -> {
            templateLoader.loadTemplateContent("nonexistent.ftlh");
        });
    }
}