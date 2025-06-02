package com.wcpk.db_schema_designer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecuteCodeRequest {
    private DatabaseConnectionRequest databaseConnectionRequest;
    private String CodeType;
    private String sqlCode;
}
