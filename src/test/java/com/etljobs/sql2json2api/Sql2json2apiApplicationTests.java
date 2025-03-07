package com.etljobs.sql2json2api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class Sql2json2apiApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		assertNotNull(jdbcTemplate);
	}
	
	@Test
	void testDatabaseConnection() {
		Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
		assertNotNull(result);
	}
}