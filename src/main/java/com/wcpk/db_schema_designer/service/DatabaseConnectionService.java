package com.wcpk.db_schema_designer.service;

import com.wcpk.db_schema_designer.dto.*;
import com.wcpk.db_schema_designer.model.Column;
import com.wcpk.db_schema_designer.model.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

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

    public List<RoutineInfo> getAllRoutinesWithParams(DatabaseConnectionRequest dcr) {
        String url = "jdbc:postgresql://" + dcr.getHost() + ":" + dcr.getPort() + "/" + dcr.getDatabaseName();
        List<RoutineInfo> routines = new ArrayList<>();

        String routinesSql = "SELECT routine_name, routine_type, data_type, specific_name " +
                "FROM information_schema.routines " +
                "WHERE specific_schema = ?";

        String paramsSql = "SELECT specific_name, parameter_name, data_type, parameter_mode, ordinal_position " +
                "FROM information_schema.parameters " +
                "WHERE specific_schema = ?";

        try (Connection conn = DriverManager.getConnection(url, dcr.getUsername(), dcr.getPassword());
             PreparedStatement routineStmt = conn.prepareStatement(routinesSql);
             PreparedStatement paramsStmt = conn.prepareStatement(paramsSql)) {

            routineStmt.setString(1, "public");
            paramsStmt.setString(1, "public");

            Map<String, List<ParameterInfo>> paramMap = new HashMap<>();

            try (ResultSet prs = paramsStmt.executeQuery()) {
                while (prs.next()) {
                    String specificName = prs.getString("specific_name");
                    String paramName = prs.getString("parameter_name");
                    String dataType = prs.getString("data_type");
                    String mode = prs.getString("parameter_mode");
                    int position = prs.getInt("ordinal_position");

                    if (paramName == null) continue;

                    ParameterInfo param = new ParameterInfo();
                    param.setName(paramName);
                    param.setDataType(dataType);
                    param.setMode(mode);
                    param.setPosition(position);

                    paramMap.computeIfAbsent(specificName, k -> new ArrayList<>()).add(param);
                }
            }

            try (ResultSet rs = routineStmt.executeQuery()) {
                while (rs.next()) {
                    String routineName = rs.getString("routine_name");
                    String routineType = rs.getString("routine_type");
                    String returnType = rs.getString("data_type");
                    String specificName = rs.getString("specific_name");

                    RoutineInfo info = new RoutineInfo();
                    info.setName(routineName);
                    info.setType(routineType);
                    info.setReturnType("FUNCTION".equalsIgnoreCase(routineType) ? returnType : null);
                    info.setParameters(paramMap.getOrDefault(specificName, new ArrayList<>()));

                    routines.add(info);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving routines: " + e.getMessage(), e);
        }

        return routines;
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
