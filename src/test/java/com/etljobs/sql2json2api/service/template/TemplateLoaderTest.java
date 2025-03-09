package com.etljobs.sql2json2api.service.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.config.PathsConfig;
import com.etljobs.sql2json2api.exception.TemplateProcessingException;

class TemplateLoaderTest {

    private TemplateLoader templateLoader;
    private PathsConfig mockPathsConfig;
    private PathMatchingResourcePatternResolver mockResolver;
    
    @BeforeEach
    void setUp() {
        mockPathsConfig = mock(PathsConfig.class);
        when(mockPathsConfig.getTemplateDirectory()).thenReturn("templates/json");
        when(mockPathsConfig.resolvedTemplateDirectory()).thenReturn("classpath:templates/json");
        
        mockResolver = mock(PathMatchingResourcePatternResolver.class);
        templateLoader = new TemplateLoader(mockPathsConfig);
        
        // Injecter le mock resolver via reflection
        ReflectionTestUtils.setField(templateLoader, "resolver", mockResolver);
    }
    
    @Test
    void buildTemplatePath_ShouldCombineDirectoryAndTemplateName() {
        // Arrange
        when(mockPathsConfig.resolvedTemplateDirectory()).thenReturn("classpath:templates/json");
        
        // Act
        String path = templateLoader.buildTemplatePath("test.ftlh");
        
        // Assert
        assertEquals("classpath:templates/json/test.ftlh", path);
    }
    
    @Test
    void buildTemplatePath_ShouldHandleAbsolutePaths() {
        // Arrange
        when(mockPathsConfig.resolvedTemplateDirectory()).thenReturn("/absolute/path/to/templates");
        
        // Act
        String path = templateLoader.buildTemplatePath("test.ftlh");
        
        // Assert
        assertEquals("/absolute/path/to/templates/test.ftlh", path);
    }
    
    @Test
    void loadTemplateContent_ShouldThrowException_WhenTemplateDoesNotExist() {
        // Arrange
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(false);
        when(mockResolver.getResource(anyString())).thenReturn(mockResource);
        
        // Act & Assert
        assertThrows(TemplateProcessingException.class, () -> {
            templateLoader.loadTemplateContent("nonexistent.ftlh");
        });
    }
    
    @Test
    void templateExists_ShouldReturnTrue_WhenTemplateExists() {
        // Arrange
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        when(mockResolver.getResource(anyString())).thenReturn(mockResource);
        
        // Act & Assert
        assertEquals(true, templateLoader.templateExists("existing.ftlh"));
    }
    
    @Test
    void templateExists_ShouldReturnFalse_WhenTemplateDoesNotExist() {
        // Arrange
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(false);
        when(mockResolver.getResource(anyString())).thenReturn(mockResource);
        
        // Act & Assert
        assertEquals(false, templateLoader.templateExists("nonexistent.ftlh"));
    }
    
    @Test
    void templateExists_ShouldReturnFalse_WhenExceptionOccurs() {
        // Arrange
        when(mockResolver.getResource(anyString())).thenThrow(new RuntimeException("Test exception"));
        
        // Act & Assert
        assertEquals(false, templateLoader.templateExists("exception.ftlh"));
    }
}