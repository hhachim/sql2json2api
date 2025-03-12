package com.etljobs.sql2json2api.service.sql;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.etljobs.sql2json2api.exception.SqlExecutionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for executing SQL queries and retrieving results.
 */
@Service
@Slf4j
public class SqlExecutionService {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired
    public SqlExecutionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Executes a SQL query and returns results as a list of maps.
     * Each map represents a row where keys are column names and values are column values.
     * 
     * @param sql The SQL query to execute
     * @return List of maps representing the query results
     * @throws SqlExecutionException if the query execution fails
     */
    public List<Map<String, Object>> executeQuery(String sql) {
        try {
            log.debug("Executing SQL query: {}", sql);
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            log.debug("Query executed successfully. Retrieved {} rows.", results.size());
            return results;
        } catch (DataAccessException e) {
            log.error("Failed to execute SQL query: {}", sql, e);
            throw new SqlExecutionException("Failed to execute SQL query", e);
        }
    }
    
    /**
     * For testing purposes - executes a count query that should return a single numeric value
     * 
     * @param sql The SQL count query to execute
     * @return The count as an integer
     * @throws SqlExecutionException if the query execution fails
     */
    public int executeCountQuery(String sql) {
        try {
            log.debug("Executing count query: {}", sql);
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            log.debug("Count query executed successfully. Result: {}", count);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            log.error("Failed to execute count query: {}", sql, e);
            throw new SqlExecutionException("Failed to execute count query", e);
        }
    }
}