package com.wcpk.db_schema_designer.controllers;

import com.wcpk.db_schema_designer.dto.DatabaseConnectionRequest;
import com.wcpk.db_schema_designer.dto.DatabaseUploadRequest;
import com.wcpk.db_schema_designer.dto.TablesResponse;
import com.wcpk.db_schema_designer.model.Table;
import com.wcpk.db_schema_designer.service.DatabaseConnectionService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
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

}
