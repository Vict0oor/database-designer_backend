package com.wcpk.db_schema_designer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProcedureRequest {
    private String name;
    private List<ProcedureParameter> parameters;
    private List<ProcedureVariable> variables;
    private List<ProcedureStep> steps;

    @Setter
    @Getter
    public static class ProcedureParameter {
        private String name;
        private String direction;
        private String type;
        private String size;
        private String precision;
    }
    @Setter
    @Getter
    public static class ProcedureVariable {
        private String name;
        private String type;
        private String size;
        private String precision;
        private String defaultValue;
    }

    @Getter
    @Setter
    public static class ProcedureStep {
        private String stepNumber;
        private String type;

        private String queryType;
        private String tableName;
        private List<String> columns;
        private List<QueryRequest.WhereCondition> whereCondition;
        private List<QueryRequest.OrderBy> orderBy;
        private List<String> groupBy;
        private String limit;

        private List<QueryRequest.Value> values;

        private String condition;

        private String loopType;
        private String loopCondition;

        private String exceptionHandling;
        private String exceptionType;
        private String customExceptionName;
        private String customCode;
        private String intoTarget;

        private List<ProcedureStep> nestedSteps;
    }
}
