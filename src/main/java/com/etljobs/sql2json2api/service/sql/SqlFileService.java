package com.etljobs.sql2json2api.service.sql;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.etljobs.sql2json2api.config.SqlConfig;
import com.etljobs.sql2json2api.exception.SqlFileException;
import com.etljobs.sql2json2api.model.SqlFile;
import com.etljobs.sql2json2api.util.FileUtils;
import com.etljobs.sql2json2api.util.PathResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for SQL file operations.
 */
@Service
@Slf4j
public class SqlFileService {

    @Value("${app.sql.directory}")
    private String sqlDirectory;
    
    @Value("${app.sql.external-directory:}")
    private String externalSqlDirectory;

    @Autowired
    private SqlConfig sqlConfig;
    
    @Autowired
    private PathResolver pathResolver;

    /**
     * Lists all available SQL files in the configured directory.
     *
     * @return A list of SqlFile objects
     */
    public List<SqlFile> listSqlFiles() {
        List<SqlFile> sqlFiles = new ArrayList<>();

        try {
            // Résoudre le chemin externe s'il est spécifié
            String resolvedExternalDir = pathResolver.resolvePath(externalSqlDirectory);
            
            // First try to load from external directory if configured
            if (resolvedExternalDir != null && !resolvedExternalDir.isEmpty()) {
                File directory = new File(resolvedExternalDir);
                if (directory.exists() && directory.isDirectory()) {
                    log.info("Loading SQL files from external directory: {}", resolvedExternalDir);
                    File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".sql"));
                    if (files != null) {
                        for (File file : files) {
                            String fileName = file.getName();
                            String content = Files.readString(file.toPath());
                            String httpMethod = FileUtils.extractHttpMethod(fileName);
                            String baseName = FileUtils.extractBaseName(fileName);
                            String templateName = FileUtils.getTemplateNameForSqlFile(fileName);

                            SqlFile sqlFile = SqlFile.builder()
                                    .fileName(fileName)
                                    .content(content)
                                    .httpMethod(httpMethod)
                                    .baseName(baseName)
                                    .templateName(templateName)
                                    .build();

                            sqlFiles.add(sqlFile);
                            log.debug("Found SQL file in external directory: {}", fileName);
                        }
                    }
                    return sqlFiles; // Return early if external files were loaded
                } else {
                    log.warn("External SQL directory does not exist or is not a directory: {}", resolvedExternalDir);
                }
            }
            
            // Fall back to classpath loading if external loading failed or not configured
            log.info("Loading SQL files from classpath directory: {}", sqlDirectory);
            Resource[] resources = FileUtils.listResources("classpath:" + sqlDirectory + "/*.sql");

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName != null) {
                    String content = readResourceContent(resource);
                    String httpMethod = FileUtils.extractHttpMethod(fileName);
                    String baseName = FileUtils.extractBaseName(fileName);
                    String templateName = FileUtils.getTemplateNameForSqlFile(fileName);

                    SqlFile sqlFile = SqlFile.builder()
                            .fileName(fileName)
                            .content(content)
                            .httpMethod(httpMethod)
                            .baseName(baseName)
                            .templateName(templateName)
                            .build();

                    sqlFiles.add(sqlFile);
                    log.debug("Found SQL file in classpath: {}", fileName);
                }
            }
        } catch (IOException e) {
            throw new SqlFileException("Failed to list SQL files", e);
        }

        return sqlFiles;
    }

    /**
     * Reads the content of a SQL file by its name.
     *
     * @param fileName The name of the SQL file (e.g., "GET_users.sql")
     * @return The SqlFile object with content and metadata
     */
    public SqlFile readSqlFile(String fileName) {
        // Résoudre le chemin externe s'il est spécifié
        String resolvedExternalDir = pathResolver.resolvePath(externalSqlDirectory);
        
        // First try external directory if configured
        if (resolvedExternalDir != null && !resolvedExternalDir.isEmpty()) {
            File file = new File(resolvedExternalDir, fileName);
            if (file.exists() && file.isFile()) {
                try {
                    log.debug("Reading SQL file from external directory: {}", fileName);
                    String content = Files.readString(file.toPath());
                    String httpMethod = FileUtils.extractHttpMethod(fileName);
                    String baseName = FileUtils.extractBaseName(fileName);
                    String templateName = FileUtils.getTemplateNameForSqlFile(fileName);

                    return SqlFile.builder()
                            .fileName(fileName)
                            .content(content)
                            .httpMethod(httpMethod)
                            .baseName(baseName)
                            .templateName(templateName)
                            .build();
                } catch (IOException e) {
                    log.warn("Failed to read external SQL file: {}", fileName, e);
                    // Fall through to try classpath instead
                }
            }
        }
        
        // Fall back to classpath
        try {
            log.debug("Reading SQL file from classpath: {}", fileName);
            Resource resource = FileUtils.listResources("classpath:" + sqlDirectory + "/" + fileName)[0];

            String content = readResourceContent(resource);
            String httpMethod = FileUtils.extractHttpMethod(fileName);
            String baseName = FileUtils.extractBaseName(fileName);
            String templateName = FileUtils.getTemplateNameForSqlFile(fileName);

            return SqlFile.builder()
                    .fileName(fileName)
                    .content(content)
                    .httpMethod(httpMethod)
                    .baseName(baseName)
                    .templateName(templateName)
                    .build();

        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            throw new SqlFileException("Failed to read SQL file: " + fileName, e);
        }
    }

    /**
     * Helper method to read the content of a resource.
     *
     * @param resource The Spring Resource object
     * @return The content as a String
     * @throws IOException If an I/O error occurs
     */
    private String readResourceContent(Resource resource) throws IOException {
        byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Récupère la liste des fichiers SQL à traiter selon l'ordre configuré. Si
     * l'ordre configuré est vide, retourne tous les fichiers disponibles.
     *
     * @return Liste ordonnée des fichiers SQL à traiter
     */
    public List<SqlFile> getSqlFilesInConfiguredOrder() {
        // Si aucun ordre n'est configuré, retourner tous les fichiers
        if (sqlConfig.getExecutionOrder() == null || sqlConfig.getExecutionOrder().isEmpty()) {
            log.info("No execution order configured, processing all SQL files");
            return listSqlFiles();
        }

        log.info("Using configured execution order: {}", sqlConfig.getExecutionOrder());
        List<SqlFile> orderedFiles = new ArrayList<>();

        // Ne traiter que les fichiers spécifiés dans l'ordre configuré
        for (String filename : sqlConfig.getExecutionOrder()) {
            SqlFile sqlFile = null;
            try {
                sqlFile = readSqlFile(filename);
                orderedFiles.add(sqlFile);
                log.debug("Added SQL file to execution queue: {}", filename);
            } catch (Exception e) {
                log.warn("SQL file specified in execution order not found: {}", filename);
            }
        }

        return orderedFiles;
    }
}