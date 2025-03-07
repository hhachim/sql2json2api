package com.etljobs.sql2json2api.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
@Getter
@Setter
public class DatabaseConfig {
    
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    
    /**
     * Configure the main data source
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }
    
    /**
     * Configure JdbcTemplate with our data source
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}