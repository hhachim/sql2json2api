package com.etljobs.sql2json2api.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void extractHttpMethod_ShouldReturnCorrectMethod() {
        // Arrange & Act & Assert
        assertEquals("GET", FileUtils.extractHttpMethod("GET_users.sql"));
        assertEquals("POST", FileUtils.extractHttpMethod("POST_order.sql"));
        assertEquals("PUT", FileUtils.extractHttpMethod("PUT_product.sql"));
        assertEquals("DELETE", FileUtils.extractHttpMethod("DELETE_user.sql"));
        assertEquals("PATCH", FileUtils.extractHttpMethod("PATCH_order.sql"));
    }

    @Test
    void extractHttpMethod_ShouldHandleCaseInsensitively() {
        // Arrange & Act & Assert
        assertEquals("GET", FileUtils.extractHttpMethod("get_users.sql"));
        assertEquals("POST", FileUtils.extractHttpMethod("Post_order.sql"));
        assertEquals("PUT", FileUtils.extractHttpMethod("put_product.sql"));
    }

    @Test
    void extractHttpMethod_ShouldReturnNullForInvalidPatterns() {
        // Arrange & Act & Assert
        assertNull(FileUtils.extractHttpMethod("users.sql"));
        assertNull(FileUtils.extractHttpMethod("GET-users.sql")); // Underscore is required
        assertNull(FileUtils.extractHttpMethod("INVALID_users.sql")); // Invalid HTTP method
        assertNull(FileUtils.extractHttpMethod("GET_users.txt")); // Wrong extension
        assertNull(FileUtils.extractHttpMethod(null)); // Null input
    }

    @Test
    void extractBaseName_ShouldReturnCorrectBaseName() {
        // Arrange & Act & Assert
        assertEquals("users", FileUtils.extractBaseName("GET_users.sql"));
        assertEquals("order", FileUtils.extractBaseName("POST_order.sql"));
        assertEquals("product_detail", FileUtils.extractBaseName("PUT_product_detail.sql"));
    }

    @Test
    void extractBaseName_ShouldHandleFilesWithoutHttpMethod() {
        // Arrange & Act & Assert
        assertEquals("users", FileUtils.extractBaseName("users.sql"));
        assertEquals("data", FileUtils.extractBaseName("data.sql"));
    }

    @Test
    void extractBaseName_ShouldReturnNullForNullInput() {
        // Arrange & Act & Assert
        assertNull(FileUtils.extractBaseName(null));
    }

    @Test
    void getTemplateNameForSqlFile_ShouldReplaceExtension() {
        // Arrange & Act & Assert
        assertEquals("GET_users.ftlh", FileUtils.getTemplateNameForSqlFile("GET_users.sql"));
        assertEquals("POST_order.ftlh", FileUtils.getTemplateNameForSqlFile("POST_order.sql"));
        assertEquals("data.ftlh", FileUtils.getTemplateNameForSqlFile("data.sql"));
    }

    @Test
    void getTemplateNameForSqlFile_ShouldReturnNullForNullInput() {
        // Arrange & Act & Assert
        assertNull(FileUtils.getTemplateNameForSqlFile(null));
    }

    @Test
    void isAbsolutePath_ShouldDetectUnixAbsolutePaths() {
        // Arrange & Act & Assert
        assertTrue(FileUtils.isAbsolutePath("/home/user/files"));
        assertTrue(FileUtils.isAbsolutePath("/opt/data"));
        assertTrue(FileUtils.isAbsolutePath("/"));
    }
    
    @Test
    void isAbsolutePath_ShouldDetectWindowsAbsolutePaths() {
        // Arrange & Act & Assert
        assertTrue(FileUtils.isAbsolutePath("C:\\Users\\user\\Documents"));
        assertTrue(FileUtils.isAbsolutePath("D:\\Data"));
        assertTrue(FileUtils.isAbsolutePath("C:"));
    }
    
    @Test
    void isAbsolutePath_ShouldReturnFalseForRelativePaths() {
        // Arrange & Act & Assert
        assertFalse(FileUtils.isAbsolutePath(""));
        assertFalse(FileUtils.isAbsolutePath(null));
        assertFalse(FileUtils.isAbsolutePath("folder/subfolder"));
        assertFalse(FileUtils.isAbsolutePath("file.txt"));
        assertFalse(FileUtils.isAbsolutePath("./resources"));
        assertFalse(FileUtils.isAbsolutePath("../parentfolder"));
    }
    
    @Test
    void listSqlFilesFromFileSystem_ShouldListSqlFilesOnly() throws IOException {
        // Arrange
        Files.writeString(tempDir.resolve("file1.sql"), "SELECT * FROM table1");
        Files.writeString(tempDir.resolve("file2.sql"), "SELECT * FROM table2");
        Files.writeString(tempDir.resolve("notasql.txt"), "This is not SQL");
        Files.createDirectory(tempDir.resolve("subdir"));
        
        // Act
        List<Path> sqlFiles = FileUtils.listSqlFilesFromFileSystem(tempDir.toString());
        
        // Assert
        assertEquals(2, sqlFiles.size(), "Should find only 2 SQL files");
        assertTrue(sqlFiles.stream().anyMatch(p -> p.getFileName().toString().equals("file1.sql")), 
                "Should find file1.sql");
        assertTrue(sqlFiles.stream().anyMatch(p -> p.getFileName().toString().equals("file2.sql")), 
                "Should find file2.sql");
        assertFalse(sqlFiles.stream().anyMatch(p -> p.getFileName().toString().equals("notasql.txt")), 
                "Should not find notasql.txt");
    }
    
    @Test
    void readFileContent_ShouldReadCorrectContent() throws IOException {
        // Arrange
        String expectedContent = "SELECT * FROM users WHERE id = 1";
        Path filePath = tempDir.resolve("test.sql");
        Files.writeString(filePath, expectedContent);
        
        // Act
        String actualContent = FileUtils.readFileContent(filePath);
        
        // Assert
        assertEquals(expectedContent, actualContent, "File content should match");
    }
    
    @Test
    void listResources_ShouldReturnResourcesFromClasspath() throws IOException {
        // This test might be environment-dependent, so we'll just test a known resource pattern
        // Arrange & Act
        Resource[] resources = FileUtils.listResources("classpath:templates/json/*.ftlh");
        
        // Assert
        assertTrue(resources.length > 0, "Should find template resources in classpath");
    }
}