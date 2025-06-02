package com.wcpk.db_schema_designer.service;

import com.wcpk.db_schema_designer.dto.DatabaseConnectionRequest;
import com.wcpk.db_schema_designer.dto.DatabaseUploadRequest;
import com.wcpk.db_schema_designer.dto.ExecuteCodeRequest;
import com.wcpk.db_schema_designer.dto.ExecuteCodeResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        DatabaseConnectionRequest connReq = request.getDatabaseConnectionRequest();
        String sqlCode = request.getSqlCode();
        String codeType = request.getCodeType();

        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://" + connReq.getHost() + ":" + connReq.getPort() + "/" + connReq.getDatabaseName(),
                connReq.getUsername(),
                connReq.getPassword()
        )) {
            conn.setAutoCommit(true);

            try (Statement stmt = conn.createStatement()) {

                if (codeType.toUpperCase().startsWith("QUERY_SELECT")) {
                    try (ResultSet rs = stmt.executeQuery(sqlCode)) {
                        List<Map<String, Object>> rows = new ArrayList<>();
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();

                        while (rs.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.put(meta.getColumnLabel(i), rs.getObject(i));
                            }
                            rows.add(row);
                        }

                        return new ExecuteCodeResponse("SUCCESS", "SELECT executed successfully", rows, true, "RESULT_SET");
                    }

                } else {
                    boolean hasResultSet = stmt.execute(sqlCode);
                    int updateCount = stmt.getUpdateCount();

                    if (hasResultSet) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            List<Map<String, Object>> rows = new ArrayList<>();
                            ResultSetMetaData meta = rs.getMetaData();
                            int colCount = meta.getColumnCount();

                            while (rs.next()) {
                                Map<String, Object> row = new LinkedHashMap<>();
                                for (int i = 1; i <= colCount; i++) {
                                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                                }
                                rows.add(row);
                            }

                            return new ExecuteCodeResponse("SUCCESS", "Code executed and returned data", rows, true, "RESULT_SET");
                        }

                    } else {
                        String upperCode = sqlCode.trim().toUpperCase();
                        String msg;
                        String resultType;

                        if (upperCode.startsWith("INSERT")) {
                            msg = "Rows inserted: " + updateCount;
                            resultType = "INSERT_COUNT";
                        } else if (upperCode.startsWith("UPDATE")) {
                            msg = "Rows updated: " + updateCount;
                            resultType = "UPDATE_COUNT";
                        } else if (upperCode.startsWith("DELETE")) {
                            msg = "Rows deleted: " + updateCount;
                            resultType = "DELETE_COUNT";
                        } else if (codeType.toUpperCase().startsWith("PROCEDURE")) {
                            msg = "Procedure created successfully.";
                            resultType = "PROCEDURE_EXECUTED";
                        } else if (codeType.toUpperCase().startsWith("FUNCTION")) {
                            msg = "Function created successfully.";
                            resultType = "FUNCTION_EXECUTED";
                        } else {
                            msg = "Code executed successfully.";
                            resultType = "DDL";
                        }

                        return new ExecuteCodeResponse("SUCCESS", msg, null, false, resultType);
                    }
                }

            } catch (SQLException e) {
                return new ExecuteCodeResponse("ERROR", e.getMessage(), null, false, "ERROR");
            }

        } catch (SQLException e) {
            return new ExecuteCodeResponse("ERROR", "Connection error: " + e.getMessage(), null, false, "ERROR");
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
