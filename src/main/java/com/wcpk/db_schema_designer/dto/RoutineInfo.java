package com.wcpk.db_schema_designer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoutineInfo {
    private String name;
    private String type;
    private String returnType;
    private List<ParameterInfo> parameters;
}

