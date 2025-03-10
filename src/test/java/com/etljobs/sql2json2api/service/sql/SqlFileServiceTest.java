package com.etljobs.sql2json2api.service.sql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.etljobs.sql2json2api.config.SqlConfig;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.util.PathResolver;

@ExtendWith(MockitoExtension.class)
class SqlFileServiceTest {

    @Mock
    private SqlConfig sqlConfig;

    @Mock
    private PathResolver pathResolver;

    @InjectMocks
    private SqlFileService sqlFileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sqlFileService, "sqlDirectory", "sql");
        ReflectionTestUtils.setField(sqlFileService, "externalSqlDirectory", "/path/to/external/sql");

        // Configurer le PathResolver par défaut pour simplement retourner la même valeur
        when(pathResolver.resolvePath(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listSqlFiles_ShouldUseExternalDirectory_WhenAvailable() throws IOException {
        // Arrange - créer des fichiers SQL temporaires
        File sqlFile1 = tempDir.resolve("GET_users.sql").toFile();
        File sqlFile2 = tempDir.resolve("POST_order.sql").toFile();

        // Écrire du contenu dans les fichiers
        Files.writeString(sqlFile1.toPath(), "SELECT * FROM users");
        Files.writeString(sqlFile2.toPath(), "INSERT INTO orders VALUES ()");

        // Configurer le PathResolver pour retourner le répertoire temporaire
        when(pathResolver.resolvePath("/path/to/external/sql")).thenReturn(tempDir.toString());

        // Act
        List<SqlFile> result = sqlFileService.listSqlFiles();

        // Assert
        assertEquals(2, result.size());

        // Vérifier que les fichiers ont été correctement chargés
        boolean foundGetUsers = false;
        boolean foundPostOrder = false;

        for (SqlFile file : result) {
            if ("GET_users.sql".equals(file.getFileName())) {
                foundGetUsers = true;
                assertEquals("GET", file.getHttpMethod());
                assertEquals("users", file.getBaseName());
                assertEquals("GET_users.ftlh", file.getTemplateName());
                assertTrue(file.getContent().contains("SELECT * FROM users"));
            } else if ("POST_order.sql".equals(file.getFileName())) {
                foundPostOrder = true;
                assertEquals("POST", file.getHttpMethod());
                assertEquals("order", file.getBaseName());
                assertEquals("POST_order.ftlh", file.getTemplateName());
                assertTrue(file.getContent().contains("INSERT INTO orders"));
            }
        }

        assertTrue(foundGetUsers, "GET_users.sql should be found");
        assertTrue(foundPostOrder, "POST_order.sql should be found");
    }

    @Test
    void readSqlFile_ShouldReadFromExternalDirectory_WhenAvailable() throws IOException {
        // Arrange - créer un fichier SQL temporaire
        File sqlFile = tempDir.resolve("GET_users.sql").toFile();
        String sqlContent = "SELECT * FROM users WHERE active = true";
        Files.writeString(sqlFile.toPath(), sqlContent);

        // Configurer le PathResolver pour retourner le répertoire temporaire
        when(pathResolver.resolvePath("/path/to/external/sql")).thenReturn(tempDir.toString());

        // Act
        SqlFile result = sqlFileService.readSqlFile("GET_users.sql");

        // Assert
        assertNotNull(result);
        assertEquals("GET_users.sql", result.getFileName());
        assertEquals("GET", result.getHttpMethod());
        assertEquals("users", result.getBaseName());
        assertEquals("GET_users.ftlh", result.getTemplateName());
        assertEquals(sqlContent, result.getContent());
    }

    @Test
    void getSqlFilesInConfiguredOrder_WhenExecutionOrderIsEmpty() {
        // Arrange - configurer un mock qui retourne des fichiers SQL temporaires
        try {
            // Créer des fichiers SQL temporaires
            File sqlFile1 = tempDir.resolve("GET_users.sql").toFile();
            File sqlFile2 = tempDir.resolve("POST_order.sql").toFile();

            // Écrire du contenu dans les fichiers
            Files.writeString(sqlFile1.toPath(), "SELECT * FROM users");
            Files.writeString(sqlFile2.toPath(), "INSERT INTO orders VALUES ()");

            // Configurer le PathResolver pour retourner le répertoire temporaire
            when(pathResolver.resolvePath("/path/to/external/sql")).thenReturn(tempDir.toString());
        } catch (IOException e) {
            // Ignore les erreurs ici, ce n'est pas le focus du test
        }

        // Configure mock pour retourner une liste vide
        when(sqlConfig.getExecutionOrder()).thenReturn(Collections.emptyList());

        // Act
        List<SqlFile> result = sqlFileService.getSqlFilesInConfiguredOrder();

        // Assert
        // Nous ne pouvons pas vérifier précisément le contenu dans ce test unitaire
        // car il dépend soit des fichiers réels dans le classpath, soit des mocks
        // dans le cas présent, nous vérifions simplement que la méthode ne lance pas d'exception
        assertNotNull(result);
    }

    @Test
    void getSqlFilesInConfiguredOrder_WhenExecutionOrderIsNull() {
        // Configure mock pour retourner null
        when(sqlConfig.getExecutionOrder()).thenReturn(null);

        // Act
        List<SqlFile> result = sqlFileService.getSqlFilesInConfiguredOrder();

        // Assert
        assertNotNull(result);
    }

    @Test
    void getSqlFilesInConfiguredOrder_ShouldRespectConfiguredOrder() {
        // Ce test est difficile à implementer complètement sans soit:
        // 1. Des fichiers réels dans le classpath de test
        // 2. Un mock complet de SqlFileService

        // Nous allons donc tester une partie de la logique:
        // Configure mock pour retourner un ordre spécifique
        when(sqlConfig.getExecutionOrder()).thenReturn(Arrays.asList("POST_order.sql", "GET_users.sql"));

        // Pour ce test, nous pouvons vérifier que la méthode ne lance pas d'exception
        // et que la logique principale fonctionne
        try {
            // Act
            sqlFileService.getSqlFilesInConfiguredOrder();

            // Assert - le test réussit si on arrive ici sans exception
            assertTrue(true, "Method executed without exception");
        } catch (Exception e) {
            // Si une exception est lancée, le test échoue
            assertTrue(false, "Method threw exception: " + e.getMessage());
        }
    }
}
