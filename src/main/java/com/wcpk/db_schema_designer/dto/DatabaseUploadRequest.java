package com.wcpk.db_schema_designer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseUploadRequest {
 private DatabaseConnectionRequest databaseConnectionRequest;
 private String sqlCode;
}
