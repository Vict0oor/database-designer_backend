package com.wcpk.db_schema_designer.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QueryRequest {
    private String table;
    private String type;
    private List<String> columns;
    private List<String> groupBy;
    private List<Value> values;
    private List<OrderBy> orderBy;
    private List<WhereCondition> where;
    public Integer limit;

    @Getter
    @Setter
    public static class Value {
        private String column;
        private String columnType;
        private boolean include;
        private String value;
    }
    @Getter
    @Setter
    public static class OrderBy {
        private String column;
        private String direction;
    }
    @Getter
    @Setter
    public static class WhereCondition {
        private String id;
        private String logicalOperator;
        private String column;
        private String columnType;
        private String operator;
        private String value;
        private String value2;
    }
}

