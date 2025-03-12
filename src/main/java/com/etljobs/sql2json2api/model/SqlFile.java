package com.etljobs.sql2json2api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a SQL file with its content and metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlFile {
    
    /**
     * The name of the SQL file (e.g., "GET_users.sql")
     */
    private String fileName;
    
    /**
     * The content of the SQL file (the SQL query)
     */
    private String content;
    
    /**
     * The HTTP method extracted from the file name prefix
     */
    private String httpMethod;
    
    /**
     * The base name without the HTTP method prefix and extension
     * (e.g., "users" for "GET_users.sql")
     */
    private String baseName;
    
    /**
     * The associated template name (e.g., "GET_users.ftlh")
     */
    private String templateName;
}