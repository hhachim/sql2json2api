package com.etljobs.sql2json2api.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * Utility class for file operations.
 */
public class FileUtils {
    
    /**
     * Pattern to extract HTTP method from file name (e.g., "GET_users.sql" -> "GET")
     */
    private static final Pattern HTTP_METHOD_PATTERN = Pattern.compile("^(GET|POST|PUT|DELETE|PATCH)_.*\\.sql$", 
                                                                       Pattern.CASE_INSENSITIVE);
    
    /**
     * Extracts the HTTP method from the file name.
     * 
     * @param fileName The name of the file (e.g., "GET_users.sql")
     * @return The HTTP method (e.g., "GET") or null if not found
     */
    public static String extractHttpMethod(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        Matcher matcher = HTTP_METHOD_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return matcher.group(1).toUpperCase();
        }
        
        return null;
    }
    
    /**
     * Extracts the base name from the file name (without HTTP method prefix and extension).
     * 
     * @param fileName The name of the file (e.g., "GET_users.sql")
     * @return The base name (e.g., "users")
     */
    public static String extractBaseName(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        // Remove extension
        String nameWithoutExtension = StringUtils.stripFilenameExtension(fileName);
        
        // Remove HTTP method prefix if present
        String httpMethod = extractHttpMethod(fileName);
        if (httpMethod != null) {
            return nameWithoutExtension.substring(httpMethod.length() + 1); // +1 for the underscore
        }
        
        return nameWithoutExtension;
    }
    
    /**
     * Gets the corresponding template name for a SQL file.
     * 
     * @param sqlFileName The name of the SQL file (e.g., "GET_users.sql")
     * @return The template name (e.g., "GET_users.ftlh")
     */
    public static String getTemplateNameForSqlFile(String sqlFileName) {
        if (sqlFileName == null) {
            return null;
        }
        
        return sqlFileName.replace(".sql", ".ftlh");
    }
    
    /**
     * Lists all resources matching a pattern.
     * 
     * @param locationPattern The classpath pattern
     * @return An array of resources matching the pattern
     * @throws IOException If an I/O error occurs
     */
    public static Resource[] listResources(String locationPattern) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        return resolver.getResources(locationPattern);
    }
    
    /**
     * Determines if a path is absolute.
     * 
     * @param path The path to check
     * @return true if the path is absolute, false otherwise
     */
    public static boolean isAbsolutePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // Check for Windows absolute path (starts with drive letter followed by colon)
        if (path.length() >= 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
            return true;
        }
        
        // Check for Unix absolute path (starts with forward slash)
        return path.startsWith("/");
    }
    
    /**
     * Lists all SQL files in a directory on the file system.
     * 
     * @param directoryPath The absolute path of the directory
     * @return A list of file paths that have a .sql extension
     * @throws IOException If an I/O error occurs
     */
    public static List<Path> listSqlFilesFromFileSystem(String directoryPath) throws IOException {
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IOException("Directory does not exist or is not a directory: " + directoryPath);
        }
        
        try (Stream<Path> paths = Files.walk(dir, 1)) {
            return paths
                .filter(path -> !Files.isDirectory(path))
                .filter(path -> path.toString().toLowerCase().endsWith(".sql"))
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Reads the content of a file from the file system.
     * 
     * @param filePath The path of the file to read
     * @return The content of the file as a string
     * @throws IOException If an I/O error occurs
     */
    public static String readFileContent(Path filePath) throws IOException {
        return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
    }
}