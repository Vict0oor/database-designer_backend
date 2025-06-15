package com.wcpk.db_schema_designer.controllers;

import com.wcpk.db_schema_designer.dto.PLSQLRequest;
import com.wcpk.db_schema_designer.dto.QueryRequest;
import com.wcpk.db_schema_designer.dto.RoutineExecutionRequest;
import com.wcpk.db_schema_designer.service.PlSqlGenerateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plsql")
public class PlSqlGenerateController {

    @Autowired
    private PlSqlGenerateService plSqlGenerateService;

    @PostMapping("/generate/query")
    public ResponseEntity<String> generateQuery (@RequestBody QueryRequest queryRequest)
    {
        return ResponseEntity.ok(plSqlGenerateService.generateQueryCode(queryRequest));
    }

    @PostMapping("/generate/procedure")
    public ResponseEntity<String> generateProcedure(@RequestBody PLSQLRequest procedureRequest)
    {
        return ResponseEntity.ok(plSqlGenerateService.generateProcedureCode(procedureRequest));
    }
    @PostMapping("/generate/function")
    public ResponseEntity<String> generateFunction(@RequestBody PLSQLRequest functionRequest)
    {
        return ResponseEntity.ok(plSqlGenerateService.generateFunctionCode(functionRequest));
    }
    @PostMapping("/generate/exe-code")
    public ResponseEntity<String> generateRoutineExecutionCode(@RequestBody RoutineExecutionRequest request)
    {
        return ResponseEntity.ok(plSqlGenerateService.generateRoutineExecutionCode(request));
    }



}
