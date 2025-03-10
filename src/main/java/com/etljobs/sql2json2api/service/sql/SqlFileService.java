package com.etljobs.sql2json2api.service.sql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import lombok.extern.slf4j.Slf4j;

/**
 * Service for SQL file operations.
 */
@Service
@Slf4j
public class SqlFileService {

    @Value("${app.sql.directory}")
    private String sqlDirectory;

    @Autowired
    private SqlConfig sqlConfig;

    /**
     * Lists all available SQL files in the configured directory.
     *
     * @return A list of SqlFile objects
     */
    public List<SqlFile> listSqlFiles() {
        List<SqlFile> sqlFiles = new ArrayList<>();

        try {
            if (FileUtils.isAbsolutePath(sqlDirectory)) {
                // Handle absolute path using file system operations
                log.debug("Using file system to read SQL files from absolute path: {}", sqlDirectory);
                List<Path> sqlFilePaths = FileUtils.listSqlFilesFromFileSystem(sqlDirectory);
                
                for (Path filePath : sqlFilePaths) {
                    String fileName = filePath.getFileName().toString();
                    String content = FileUtils.readFileContent(filePath);
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
                    log.debug("Found SQL file: {}", fileName);
                }
            } else {
                // Original behavior: handle classpath resources
                log.debug("Using classpath to read SQL files from: {}", sqlDirectory);
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
                        log.debug("Found SQL file: {}", fileName);
                    }
                }
            }
        } catch (IOException e) {
            throw new SqlFileException("Failed to list SQL files from directory: " + sqlDirectory, e);
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
        try {
            String content;
            
            if (FileUtils.isAbsolutePath(sqlDirectory)) {
                // Handle absolute path using file system operations
                Path filePath = Paths.get(sqlDirectory, fileName);
                if (!Files.exists(filePath)) {
                    throw new SqlFileException("SQL file not found: " + filePath);
                }
                content = FileUtils.readFileContent(filePath);
            } else {
                // Original behavior: handle classpath resources
                Resource resource = FileUtils.listResources("classpath:" + sqlDirectory + "/" + fileName)[0];
                content = readResourceContent(resource);
            }
            
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
