package com.wcpk.db_schema_designer.controllers;

import com.wcpk.db_schema_designer.dto.DatabaseUploadRequest;
import com.wcpk.db_schema_designer.service.DatabaseConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
