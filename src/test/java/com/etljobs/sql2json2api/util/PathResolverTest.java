package com.etljobs.sql2json2api.util;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class PathResolverTest {

    @Mock
    private Environment environment;
    
    private PathResolver pathResolver;
    
    @BeforeEach
    void setUp() {
        // Par défaut, configurer environment pour retourner null pour spring.config.location
        when(environment.getProperty("spring.config.location")).thenReturn(null);
    }
    
    @Test
    void resolvePath_ShouldReturnNull_WhenPathIsNull() {
        // Arrange
        pathResolver = new PathResolver(environment);
        
        // Act
        String result = pathResolver.resolvePath(null);
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void resolvePath_ShouldReturnEmpty_WhenPathIsEmpty() {
        // Arrange
        pathResolver = new PathResolver(environment);
        
        // Act
        String result = pathResolver.resolvePath("");
        
        // Assert
        assertEquals("", result);
    }
    
    @Test
    void resolvePath_ShouldReturnSamePath_WhenPathIsAbsolute() {
        // Arrange
        pathResolver = new PathResolver(environment);
        String absolutePath = getAbsolutePath("/some/absolute/path");
        
        // Act
        String result = pathResolver.resolvePath(absolutePath);
        
        // Assert
        assertEquals(absolutePath, result);
    }
    
    @Test
    void resolvePath_ShouldReturnSamePath_WhenConfigLocationIsNull() {
        // Arrange
        pathResolver = new PathResolver(environment);
        
        // Act
        String result = pathResolver.resolvePath("relative/path");
        
        // Assert
        assertEquals("relative/path", result);
    }
    
    @Test
    void resolvePath_ShouldResolveRelativePath_WhenConfigLocationIsDirectory() {
        // Arrange
        String configLocation = getAbsolutePath("/config/location/");
        when(environment.getProperty("spring.config.location")).thenReturn(configLocation);
        pathResolver = new PathResolver(environment);
        
        // Act
        String result = pathResolver.resolvePath("relative/path");
        
        // Assert
        String expected = new File(configLocation, "relative/path").getPath();
        assertEquals(expected, result);
    }
    
    @Test
    void resolvePath_ShouldResolveRelativePath_WhenConfigLocationIsFile() {
        // Arrange
        String configLocation = getAbsolutePath("/config/location/application.yml");
        when(environment.getProperty("spring.config.location")).thenReturn(configLocation);
        pathResolver = new PathResolver(environment);
        
        // Act
        String result = pathResolver.resolvePath("relative/path");
        
        // Assert
        File parentDir = new File(configLocation).getParentFile();
        String expected = new File(parentDir, "relative/path").getPath();
        assertEquals(expected, result);
    }
    
    @Test
    void resolvePath_ShouldRemoveFilePrefix_WhenConfigLocationHasFilePrefix() {
        // Arrange
        String configLocation = "file:" + getAbsolutePath("/config/location/");
        when(environment.getProperty("spring.config.location")).thenReturn(configLocation);
        pathResolver = new PathResolver(environment);
        
        // Act
        String result = pathResolver.resolvePath("relative/path");
        
        // Assert
        String baseDirWithoutPrefix = configLocation.substring(5);
        String expected = new File(baseDirWithoutPrefix, "relative/path").getPath();
        assertEquals(expected, result);
    }
    
    /**
     * Helper method to ensure paths are absolute and OS-specific.
     */
    private String getAbsolutePath(String path) {
        // Convertir le chemin en format spécifique à l'OS
        if (File.separatorChar == '\\') {
            // Windows: transformer /path/to/file en C:\path\to\file
            path = path.replace('/', '\\');
            if (path.startsWith("\\")) {
                path = "C:" + path;
            }
        }
        return new File(path).getAbsolutePath();
    }
}