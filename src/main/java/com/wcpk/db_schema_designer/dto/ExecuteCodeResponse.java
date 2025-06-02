package com.wcpk.db_schema_designer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ExecuteCodeResponse {
    private String status;
    private String message;
    private List<Map<String, Object>> result;
    private boolean hasResult;
    private String resultType;
}

