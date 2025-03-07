package com.etljobs.sql2json2api.util;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
}