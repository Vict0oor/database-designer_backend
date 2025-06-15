package com.wcpk.db_schema_designer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public  class RoutineExecutionRequest {
    private String routineName;
    private String routineType;
    private String returnType;
    private String resultVariable;
    private List<Parameter> parameters;

    @Setter
    @Getter
    public static class Parameter {
        private String name;
        private String mode;
        private String dataType;
        private int position;
        private String valueType;
        private String value;
    }
}
