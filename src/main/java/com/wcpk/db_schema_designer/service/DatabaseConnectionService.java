package com.wcpk.db_schema_designer.service;

import com.wcpk.db_schema_designer.dto.DatabaseConnectionRequest;
import com.wcpk.db_schema_designer.dto.DatabaseUploadRequest;
import com.wcpk.db_schema_designer.model.Column;
import com.wcpk.db_schema_designer.model.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public List<Table> getTablesData(DatabaseConnectionRequest dcr)
    {
        List<Table> tablesList = new ArrayList<>();
        String url = "jdbc:postgresql://" + dcr.getHost() +":" + dcr.getPort() + "/" + dcr.getDatabaseName();
        try {
            Connection connection = DriverManager.getConnection(url,dcr.getUsername(),dcr.getPassword());
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null,null,"%",new String[]{"TABLE"});

            while (tables.next())
            {
                String tableName = tables.getString("TABLE_NAME");
                Table table = new Table(tableName);
                ResultSet columns = metaData.getColumns(null, null, tableName, "%");

                while (columns.next())
                {
                    String columnName = columns.getString("COLUMN_NAME");
                    String dataType = columns.getString("TYPE_NAME");
                    Column column = new Column(columnName, dataType);
                    table.addColumn(column);
                }

                columns.close();
                tablesList.add(table);
            }
            tables.close();
            return tablesList;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
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
