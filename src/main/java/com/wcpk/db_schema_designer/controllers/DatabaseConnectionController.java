package com.wcpk.db_schema_designer.controllers;

import com.wcpk.db_schema_designer.dto.*;
import com.wcpk.db_schema_designer.model.Table;
import com.wcpk.db_schema_designer.service.DatabaseConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/database-connection")
public class DatabaseConnectionController {

    @Autowired
    private DatabaseConnectionService databaseConnectionService;
    @PostMapping("/execute-sql-script")
    public ResponseEntity<String> executeSqlScript (@RequestBody DatabaseUploadRequest request)
    {
        String result = databaseConnectionService.uploadSqlCodeToDatabase(request);
        if (result.startsWith("SQL script executed successfully")){
            return ResponseEntity.ok(result);
        }
        else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/get-tables")
    public ResponseEntity<TablesResponse> getTables (@RequestBody DatabaseConnectionRequest databaseConnectionRequest)
    {
        List<Table> tables = databaseConnectionService.getTablesData(databaseConnectionRequest);
        return ResponseEntity.ok(new TablesResponse(tables));
    }
    @PostMapping("/get-routines")
    public ResponseEntity<List<RoutineInfo>> getRoutines (@RequestBody DatabaseConnectionRequest databaseConnectionRequest)
    {
        List<RoutineInfo> routines = databaseConnectionService.getAllRoutinesWithParams(databaseConnectionRequest);
        return ResponseEntity.ok(routines);
    }


    @PostMapping("/execute-code")
    public ResponseEntity<ExecuteCodeResponse> executeSqlCode(@RequestBody ExecuteCodeRequest request) {
        ExecuteCodeResponse response = databaseConnectionService.executeCode(request);

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}
