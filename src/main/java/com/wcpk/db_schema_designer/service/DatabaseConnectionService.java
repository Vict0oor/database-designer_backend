package com.wcpk.db_schema_designer.service;

import com.wcpk.db_schema_designer.dto.DatabaseConnectionRequest;
import com.wcpk.db_schema_designer.dto.DatabaseUploadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class DatabaseConnectionService {

    public String uploadSqlCodeToDatabase(DatabaseUploadRequest databaseUploadRequest) {
        try {
            DataSource dataSource = createDataSource(databaseUploadRequest.getDatabaseConnectionRequest());
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            jdbcTemplate.execute(databaseUploadRequest.getSqlCode());

            return "SQL script executed successfully!";
        } catch (DataAccessException dae) {
            Throwable rootCause = dae.getRootCause();
            if (rootCause instanceof SQLException) {
                SQLException sqlEx = (SQLException) rootCause;
                return "Database error: " + sqlEx.getMessage();
            } else {
                return "Database access error: " + dae.getMessage();
            }
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }

    private DataSource createDataSource (DatabaseConnectionRequest dcr)
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://" + dcr.getHost() + ":" + dcr.getPort() + "/" + dcr.getDatabaseName());
        dataSource.setUsername(dcr.getUsername());
        dataSource.setPassword(dcr.getPassword());
        return dataSource;
    }
}
