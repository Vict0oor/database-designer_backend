package com.wcpk.db_schema_designer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseConnectionRequest {
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;
    private String sql;
}
