package com.picklebook.controller.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-empty")
class EmptyDatabasePublicPagesSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void removeSeedRows() throws SQLException {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try (var connection = jdbcTemplate.getDataSource().getConnection()) {
            List<String> tableNames = new ArrayList<>();
            try (var tables = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    if (!"PUBLIC".equalsIgnoreCase(tables.getString("TABLE_SCHEM"))) {
                        continue;
                    }
                    String tableName = tables.getString("TABLE_NAME");
                    if (!"flyway_schema_history".equalsIgnoreCase(tableName)) {
                        tableNames.add(tableName);
                    }
                }
            }
            for (String tableName : tableNames) {
                jdbcTemplate.execute("DELETE FROM " + tableName.toLowerCase(Locale.ROOT));
            }
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    @Test
    void publicPagesHandleNoSeedRows() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/san")).andExpect(status().isOk());
        mockMvc.perform(get("/san/san-trung-tam")).andExpect(status().isNotFound());
        mockMvc.perform(get("/gioi-thieu")).andExpect(status().isOk());
        mockMvc.perform(get("/chinh-sach")).andExpect(status().isOk());
        mockMvc.perform(get("/dat-san").param("courtId", "1"))
                .andExpect(status().is3xxRedirection());
    }
}
