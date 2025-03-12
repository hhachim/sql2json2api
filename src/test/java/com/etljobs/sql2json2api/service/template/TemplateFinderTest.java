package com.etljobs.sql2json2api.service.template;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.model.SqlFile;

class TemplateFinderTest {

    private TemplateFinder templateFinder;
    private PathMatchingResourcePatternResolver mockResolver;
    
    @BeforeEach
    void setUp() {
        // Créer un mock du résolveur
        mockResolver = mock(PathMatchingResourcePatternResolver.class);
        
        // Créer l'instance réelle (pas un spy)
        templateFinder = new TemplateFinder();
        
        // Définir les champs via reflection
        ReflectionTestUtils.setField(templateFinder, "templateDirectory", "templates/json");
        ReflectionTestUtils.setField(templateFinder, "resolver", mockResolver);
    }
    
    @Test
    void determineTemplateName_ShouldReplaceExtension() {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        // Act
        String templateName = templateFinder.determineTemplateName(sqlFile);
        
        // Assert
        assertEquals("GET_users.ftlh", templateName);
    }
    
    @Test
    void findTemplateForSqlFile_ShouldReturnEmpty_WhenSqlFileIsNull() {
        // Act
        Optional<String> result = templateFinder.findTemplateForSqlFile(null);
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    void findTemplateForSqlFile_ShouldReturnTemplateName_WhenTemplateExists() throws IOException {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        // Mocker le comportement de templateExists
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        
        Resource[] resources = new Resource[] { mockResource };
        when(mockResolver.getResources(anyString())).thenReturn(resources);
        
        // Act
        Optional<String> result = templateFinder.findTemplateForSqlFile(sqlFile);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("GET_users.ftlh", result.get());
    }
    
    @Test
    void findTemplateForSqlFile_ShouldReturnEmpty_WhenTemplateDoesNotExist() throws IOException {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_nonexistent.sql")
                .httpMethod("GET")
                .baseName("nonexistent")
                .build();
        
        // Mocker le comportement de templateExists pour renvoyer false
        when(mockResolver.getResources(anyString())).thenReturn(new Resource[0]);
        
        // Act
        Optional<String> result = templateFinder.findTemplateForSqlFile(sqlFile);
        
        // Assert
        assertFalse(result.isPresent());
    }
    
    @Test
    void findAlternativeTemplate_ShouldFindBaseNameTemplate() throws IOException {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        // Simuler que le template baseName existe
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        Resource[] resources = new Resource[] { mockResource };
        
        // Configurer le comportement du resolver pour "users.ftlh"
        when(mockResolver.getResources(eq("classpath:templates/json/users.ftlh"))).thenReturn(resources);
        
        // Configurer pour que les autres templates n'existent pas
        when(mockResolver.getResources(eq("classpath:templates/json/GET_generic.ftlh"))).thenReturn(new Resource[0]);
        when(mockResolver.getResources(eq("classpath:templates/json/default.ftlh"))).thenReturn(new Resource[0]);
        
        // Act
        Optional<String> result = templateFinder.findAlternativeTemplate(sqlFile);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("users.ftlh", result.get());
    }
    
    @Test
    void findAlternativeTemplate_ShouldFindGenericTemplate() throws IOException {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        // Configurer le comportement du resolver pour que seul le template générique existe
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        Resource[] resources = new Resource[] { mockResource };
        
        // Le template baseName n'existe pas
        when(mockResolver.getResources(eq("classpath:templates/json/users.ftlh"))).thenReturn(new Resource[0]);
        
        // Le template générique existe
        when(mockResolver.getResources(eq("classpath:templates/json/GET_generic.ftlh"))).thenReturn(resources);
        
        // Le template par défaut n'existe pas
        when(mockResolver.getResources(eq("classpath:templates/json/default.ftlh"))).thenReturn(new Resource[0]);
        
        // Act
        Optional<String> result = templateFinder.findAlternativeTemplate(sqlFile);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("GET_generic.ftlh", result.get());
    }
    
    @Test
    void findAlternativeTemplate_ShouldFindDefaultTemplate() throws IOException {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        // Configurer le comportement du resolver pour que seul le template par défaut existe
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        Resource[] resources = new Resource[] { mockResource };
        
        // Les autres templates n'existent pas
        when(mockResolver.getResources(eq("classpath:templates/json/users.ftlh"))).thenReturn(new Resource[0]);
        when(mockResolver.getResources(eq("classpath:templates/json/GET_generic.ftlh"))).thenReturn(new Resource[0]);
        
        // Le template par défaut existe
        when(mockResolver.getResources(eq("classpath:templates/json/default.ftlh"))).thenReturn(resources);
        
        // Act
        Optional<String> result = templateFinder.findAlternativeTemplate(sqlFile);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("default.ftlh", result.get());
    }
    
    @Test
    void findBestMatchingTemplate_ShouldReturnStandardTemplate_WhenExists() throws IOException {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        // Simuler que le template standard existe
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        Resource[] resources = new Resource[] { mockResource };
        
        when(mockResolver.getResources(eq("classpath:templates/json/GET_users.ftlh"))).thenReturn(resources);
        
        // Act
        Optional<String> result = templateFinder.findBestMatchingTemplate(sqlFile);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("GET_users.ftlh", result.get());
    }
    
    @Test
    void findBestMatchingTemplate_ShouldReturnAlternative_WhenStandardDoesNotExist() throws IOException {
        // Arrange
        SqlFile sqlFile = SqlFile.builder()
                .fileName("GET_users.sql")
                .httpMethod("GET")
                .baseName("users")
                .build();
        
        // Simuler que le template standard n'existe pas
        when(mockResolver.getResources(eq("classpath:templates/json/GET_users.ftlh"))).thenReturn(new Resource[0]);
        
        // Mais que le template baseName existe
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        Resource[] resources = new Resource[] { mockResource };
        
        when(mockResolver.getResources(eq("classpath:templates/json/users.ftlh"))).thenReturn(resources);
        
        // Et que les autres n'existent pas
        when(mockResolver.getResources(eq("classpath:templates/json/GET_generic.ftlh"))).thenReturn(new Resource[0]);
        when(mockResolver.getResources(eq("classpath:templates/json/default.ftlh"))).thenReturn(new Resource[0]);
        
        // Act
        Optional<String> result = templateFinder.findBestMatchingTemplate(sqlFile);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals("users.ftlh", result.get());
    }
}