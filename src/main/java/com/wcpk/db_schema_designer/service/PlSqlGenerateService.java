package com.wcpk.db_schema_designer.service;

import com.wcpk.db_schema_designer.dto.PLSQLRequest;
import com.wcpk.db_schema_designer.dto.QueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlSqlGenerateService {

    public String generateQueryCode (QueryRequest queryRequest)
    {
        return switch (queryRequest.getType().toUpperCase()) {
            case "SELECT" -> buildSelectQuery(queryRequest);
            case "INSERT" -> buildInsertQuery(queryRequest);
            case "DELETE" -> buildDeleteQuery(queryRequest);
            case "UPDATE" -> buildUpdateQuery(queryRequest);
            default -> throw new IllegalArgumentException("Unsupported query type: " + queryRequest);
        };
    }

    public String generateProcedureCode(PLSQLRequest procedureRequest) {
        StringBuilder procedure = new StringBuilder();

        procedure.append("CREATE OR REPLACE PROCEDURE ").append(procedureRequest.getName());

        if (procedureRequest.getParameters() != null && !procedureRequest.getParameters().isEmpty()) {
            procedure.append(" (\n");
            List<String> parameterDeclarations = procedureRequest.getParameters().stream()
                    .map(this::buildParameterDeclaration)
                    .collect(Collectors.toList());
            procedure.append("    ").append(String.join(",\n    ", parameterDeclarations));
            procedure.append("\n)");
        }
        else{
            procedure.append(" ()");
        }

        procedure.append("\nLANGUAGE plpgsql");
        procedure.append("\nAS $$\n");

        if (procedureRequest.getVariables() != null && !procedureRequest.getVariables().isEmpty()) {
            procedure.append("DECLARE\n");
            for (PLSQLRequest.Variable variable : procedureRequest.getVariables()) {
                procedure.append("    ").append(buildVariableDeclaration(variable)).append("\n");
            }
            procedure.append("\n");
        }

        procedure.append("BEGIN\n");

        if (procedureRequest.getSteps() != null && !procedureRequest.getSteps().isEmpty()) {
            for (PLSQLRequest.Step step : procedureRequest.getSteps()) {
                procedure.append(buildProcedureStep(step, 1));
            }
        }

        procedure.append("\nEXCEPTION\n");
        procedure.append("    WHEN OTHERS THEN\n");
        procedure.append("        RAISE;\n");
        procedure.append("END").append(";\n");
        procedure.append("$$;");

        return procedure.toString();
    }

    private String buildParameterDeclaration(PLSQLRequest.Parameter parameter) {
        StringBuilder paramDecl = new StringBuilder();
        paramDecl.append(parameter.getName()).append(" ");

        if (parameter.getDirection() != null && !parameter.getDirection().isEmpty()) {
            paramDecl.append(parameter.getDirection().toUpperCase()).append(" ");
        }

        paramDecl.append(buildDataType(parameter.getType(), parameter.getSize(), parameter.getPrecision()));

        return paramDecl.toString();
    }

    private String buildVariableDeclaration(PLSQLRequest.Variable variable) {
        StringBuilder varDecl = new StringBuilder();
        varDecl.append(variable.getName()).append(" ");
        varDecl.append(buildDataType(variable.getType(), variable.getSize(), variable.getPrecision()));

        if (variable.getDefaultValue() != null && !variable.getDefaultValue().isEmpty()) {
            if (shouldQuote(variable.getType())) {
                varDecl.append(" := '").append(escapeSingleQuotes(variable.getDefaultValue())).append("'");
            } else {
                varDecl.append(" := ").append(variable.getDefaultValue());
            }
        }

        varDecl.append(";");
        return varDecl.toString();
    }

    private String buildDataType(String type, String size, String precision) {
        StringBuilder dataType = new StringBuilder(type.toUpperCase());

        if (size != null && !size.isEmpty()) {
            dataType.append("(").append(size);
            if (precision != null && !precision.isEmpty()) {
                dataType.append(",").append(precision);
            }
            dataType.append(")");
        }

        return dataType.toString();
    }

    private String buildProcedureStep(PLSQLRequest.Step step, int indentLevel) {
        StringBuilder stepCode = new StringBuilder();
        String indent = "    ".repeat(indentLevel);

        if (step.getType() == null) {
            return stepCode.toString();
        }

        switch (step.getType().toUpperCase()) {
            case "QUERY" -> stepCode.append(buildQueryStep(step, indent));
            case "SELECT INTO" -> stepCode.append(buildSelectIntoStep(step, indent));
            case "IF-ELSE" -> stepCode.append(buildIfElseStep(step, indentLevel));
            case "LOOP" -> stepCode.append(buildLoopStep(step, indentLevel));
            case "EXCEPTION" -> stepCode.append(buildExceptionStep(step, indent));
            case "CUSTOM" -> stepCode.append(buildCustomStep(step, indent));
            default -> stepCode.append(indent).append("-- Unknown step type: ").append(step.getType()).append("\n");
        }

        return stepCode.toString();
    }
    private String buildQueryStep(PLSQLRequest.Step step, String indent) {
        StringBuilder queryStep = new StringBuilder();

        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setType(step.getQueryType());
        queryRequest.setTable(step.getTableName());
        queryRequest.setColumns(step.getColumns());
        queryRequest.setWhere(step.getWhereCondition());
        queryRequest.setOrderBy(step.getOrderBy());
        queryRequest.setGroupBy(step.getGroupBy());
        queryRequest.setValues(step.getValues());

        if (step.getLimit() != null && !step.getLimit().isEmpty()) {
            try {
                queryRequest.setLimit(Integer.parseInt(step.getLimit()));
            } catch (NumberFormatException e) {
            }
        }

        String query = generateQueryCode(queryRequest);
        String[] queryLines = query.split("\n");

        for (String line : queryLines) {
            queryStep.append(indent).append(line).append("\n");
        }

        if (!query.trim().endsWith(";")) {
            queryStep.append(indent).append(";\n");
        }

        return queryStep.toString();
    }

    private String buildSelectIntoStep(PLSQLRequest.Step step, String indent) {
        StringBuilder selectInto = new StringBuilder();

        selectInto.append(indent).append("SELECT ");

        if (step.getColumns() == null || step.getColumns().isEmpty()) {
            selectInto.append("*");
        } else {
            selectInto.append(String.join(", ", step.getColumns()));
        }

        selectInto.append("\n").append(indent).append("INTO ").append(step.getIntoTarget());
        selectInto.append("\n").append(indent).append("FROM ").append(step.getTableName());

        if (step.getWhereCondition() != null && !step.getWhereCondition().isEmpty()) {
            selectInto.append("\n").append(indent).append("WHERE ");
            selectInto.append(buildWhereClause(step.getWhereCondition()));
        }

        if (step.getOrderBy() != null && !step.getOrderBy().isEmpty()) {
            selectInto.append("\n").append(indent).append("ORDER BY ");
            selectInto.append(step.getOrderBy().stream()
                    .map(o -> o.getColumn() + " " + o.getDirection())
                    .collect(Collectors.joining(", ")));
        }

        selectInto.append(";\n");

        return selectInto.toString();
    }
    private String buildWhereClause(List<QueryRequest.WhereCondition> whereConditions) {
        StringBuilder whereClause = new StringBuilder();

        for (int i = 0; i < whereConditions.size(); i++) {
            QueryRequest.WhereCondition cond = whereConditions.get(i);

            if (i > 0) {
                whereClause.append(" ").append(cond.getLogicalOperator()).append(" ");
            }

            whereClause.append(cond.getColumn()).append(" ").append(cond.getOperator());

            if ("IS NULL".equalsIgnoreCase(cond.getOperator()) || "IS NOT NULL".equalsIgnoreCase(cond.getOperator())) {
            } else if ("BETWEEN".equalsIgnoreCase(cond.getOperator())) {
                if (shouldQuote(cond.getColumnType())) {
                    whereClause.append(" '").append(escapeSingleQuotes(cond.getValue()))
                            .append("' AND '").append(escapeSingleQuotes(cond.getValue2())).append("'");
                } else {
                    whereClause.append(" ").append(cond.getValue()).append(" AND ").append(cond.getValue2());
                }
            } else if ("IN".equalsIgnoreCase(cond.getOperator())) {
                if (shouldQuote(cond.getColumnType())) {
                    String quotedValues = Arrays.stream(cond.getValue().split(","))
                            .map(String::trim)
                            .map(v -> "'" + escapeSingleQuotes(v) + "'")
                            .collect(Collectors.joining(", "));
                    whereClause.append(" (").append(quotedValues).append(")");
                } else {
                    whereClause.append(" (").append(cond.getValue()).append(")");
                }
            } else {
                if (shouldQuote(cond.getColumnType())) {
                    whereClause.append(" '").append(escapeSingleQuotes(cond.getValue())).append("'");
                } else {
                    whereClause.append(" ").append(cond.getValue());
                }
            }
        }

        return whereClause.toString();
    }

    private String buildIfElseStep(PLSQLRequest.Step step, int indentLevel) {
        StringBuilder ifElse = new StringBuilder();
        String indent = "    ".repeat(indentLevel);

        ifElse.append(indent).append("IF ").append(step.getCondition()).append(" THEN\n");

        if (step.getNestedSteps() != null && !step.getNestedSteps().isEmpty()) {
            for (PLSQLRequest.Step nestedStep : step.getNestedSteps()) {
                ifElse.append(buildProcedureStep(nestedStep, indentLevel + 1));
            }
        }

        ifElse.append(indent).append("END IF;\n");

        return ifElse.toString();
    }

    private String buildLoopStep(PLSQLRequest.Step step, int indentLevel) {
        StringBuilder loop = new StringBuilder();
        String indent = "    ".repeat(indentLevel);

        if ("FOR".equalsIgnoreCase(step.getLoopType())) {
            loop.append(indent).append("FOR ").append(step.getLoopCondition()).append(" LOOP\n");
        } else if ("WHILE".equalsIgnoreCase(step.getLoopType())) {
            loop.append(indent).append("WHILE ").append(step.getLoopCondition()).append(" LOOP\n");
        } else {
            loop.append(indent).append("LOOP\n");
        }

        if (step.getNestedSteps() != null && !step.getNestedSteps().isEmpty()) {
            for (PLSQLRequest.Step nestedStep : step.getNestedSteps()) {
                loop.append(buildProcedureStep(nestedStep, indentLevel + 1));
            }
        }

        if ("WHILE".equalsIgnoreCase(step.getLoopType()) || step.getLoopType() == null) {
            loop.append(indent).append("    EXIT WHEN ").append(step.getLoopCondition()).append(";\n");
        }

        loop.append(indent).append("END LOOP;\n");

        return loop.toString();
    }
    private String buildExceptionStep(PLSQLRequest.Step step, String indent) {
        StringBuilder exception = new StringBuilder();
        exception.append("\nEXCEPTION\n");

        String exceptionType = step.getExceptionType();
        String handling = step.getExceptionHandling();
        String customExceptionName = step.getCustomExceptionName();

        if (exceptionType != null && !exceptionType.trim().isEmpty()) {

            if ("CUSTOM_WHEN".equalsIgnoreCase(exceptionType.trim())) {
                if (customExceptionName != null && !customExceptionName.trim().isEmpty()) {
                    exception.append(indent).append("WHEN ");
                    exception.append(customExceptionName.trim());
                    exception.append(" THEN\n");
                }
            } else {
                exception.append(indent)
                        .append("WHEN ")
                        .append(exceptionType.toUpperCase())
                        .append(" THEN\n");
            }

            if (handling != null && !handling.trim().isEmpty()) {
                String[] handlingLines = handling.split("\n");
                for (String line : handlingLines) {
                    exception.append(indent).append("    ").append(line.trim()).append("\n");
                }
            } else {
                exception.append(indent).append("    RAISE;\n");
            }
        }
        return exception.toString();
    }

    private String buildCustomStep(PLSQLRequest.Step step, String indent) {
        StringBuilder custom = new StringBuilder();

        if (step.getCustomCode() != null && !step.getCustomCode().isEmpty()) {
            String[] customLines = step.getCustomCode().split("\n");
            for (String line : customLines) {
                custom.append(indent).append(line).append("\n");
            }
        }

        return custom.toString();
    }
    public String buildSelectQuery(QueryRequest queryRequest) {
        StringBuilder query = new StringBuilder();

        if (queryRequest.getColumns() == null || queryRequest.getColumns().isEmpty()) {
            query.append("SELECT *");
        } else {
            query.append("SELECT ");
            query.append(String.join(", ", queryRequest.getColumns()));
        }

        query.append("\nFROM ").append(queryRequest.getTable());

        if (queryRequest.getWhere() != null && !queryRequest.getWhere().isEmpty()) {
            query.append("\nWHERE ");

            for (int i = 0; i < queryRequest.getWhere().size(); i++) {
                QueryRequest.WhereCondition cond = queryRequest.getWhere().get(i);

                if (i > 0) {
                    query.append(" ").append(cond.getLogicalOperator()).append(" ");
                }

                query.append(cond.getColumn()).append(" ").append(cond.getOperator());

                if ("IS NULL".equals(cond.getOperator()) || "IS NOT NULL".equals(cond.getOperator())) {
                } else if ("BETWEEN".equals(cond.getOperator())) {
                    if (shouldQuote(cond.getColumnType())) {
                        query.append(" '").append(cond.getValue()).append("' AND '").append(cond.getValue2()).append("'");
                    } else {
                        query.append(" ").append(cond.getValue()).append(" AND ").append(cond.getValue2());
                    }
                } else if ("IN".equals(cond.getOperator())) {
                    if (shouldQuote(cond.getColumnType())) {
                        String quotedValues = Arrays.stream(cond.getValue().split(","))
                                .map(String::trim)
                                .map(v -> "'" + v + "'")
                                .collect(Collectors.joining(", "));
                        query.append(" (").append(quotedValues).append(")");
                    } else {
                        query.append(" (").append(cond.getValue()).append(")");
                    }
                } else {
                    if (shouldQuote(cond.getColumnType())) {
                        query.append(" '").append(cond.getValue()).append("'");
                    } else {
                        query.append(" ").append(cond.getValue());
                    }
                }
            }
        }

        if(queryRequest.getGroupBy() != null && !queryRequest.getGroupBy().isEmpty() )
        {
            query.append("\nGROUP BY ");
            query.append(String.join(", ", queryRequest.getGroupBy()));
        }

        if (queryRequest.getOrderBy() != null && !queryRequest.getOrderBy().isEmpty()) {
            query.append("\nORDER BY ");
            query.append(queryRequest.getOrderBy().stream()
                    .map(o -> o.getColumn() + " " + o.getDirection())
                    .reduce((a, b) -> a + ", " + b).orElse(""));
        }

        if(queryRequest.getLimit() != null)
        {
            query.append("\nLIMIT ").append(queryRequest.getLimit());
        }

        return query.toString();
    }


    public String buildUpdateQuery(QueryRequest queryRequest) {
        StringBuilder query = new StringBuilder();

        query.append("UPDATE ").append(queryRequest.getTable()).append("\nSET ");

        List<String> setClauses = queryRequest.getValues().stream()
                .filter(QueryRequest.Value::isInclude)
                .map(val -> {
                    String valueStr = shouldQuote(val.getColumnType())
                            ? "'" + escapeSingleQuotes(val.getValue()) + "'"
                            : val.getValue();
                    return val.getColumn() + " = " + valueStr;
                })
                .collect(Collectors.toList());

        if (setClauses.isEmpty()) {
            throw new IllegalArgumentException("No columns selected for update.");
        }

        query.append(String.join(", ", setClauses));

        if (queryRequest.getWhere() != null && !queryRequest.getWhere().isEmpty()) {
            query.append("\nWHERE ");
            for (int i = 0; i < queryRequest.getWhere().size(); i++) {
                QueryRequest.WhereCondition cond = queryRequest.getWhere().get(i);

                if (i > 0) {
                    query.append(" ").append(cond.getLogicalOperator()).append(" ");
                }

                query.append(cond.getColumn()).append(" ").append(cond.getOperator());

                if ("IS NULL".equalsIgnoreCase(cond.getOperator()) || "IS NOT NULL".equalsIgnoreCase(cond.getOperator())) {
                } else if ("BETWEEN".equalsIgnoreCase(cond.getOperator())) {
                    if (shouldQuote(cond.getColumnType())) {
                        query.append(" '").append(escapeSingleQuotes(cond.getValue()))
                                .append("' AND '").append(escapeSingleQuotes(cond.getValue2())).append("'");
                    } else {
                        query.append(" ").append(cond.getValue()).append(" AND ").append(cond.getValue2());
                    }
                } else if ("IN".equalsIgnoreCase(cond.getOperator())) {
                    if (shouldQuote(cond.getColumnType())) {
                        String quotedValues = Arrays.stream(cond.getValue().split(","))
                                .map(String::trim)
                                .map(v -> "'" + escapeSingleQuotes(v) + "'")
                                .collect(Collectors.joining(", "));
                        query.append(" (").append(quotedValues).append(")");
                    } else {
                        query.append(" (").append(cond.getValue()).append(")");
                    }
                } else {
                    if (shouldQuote(cond.getColumnType())) {
                        query.append(" '").append(escapeSingleQuotes(cond.getValue())).append("'");
                    } else {
                        query.append(" ").append(cond.getValue());
                    }
                }
            }
        }

        return query.toString();
    }

    public String buildInsertQuery(QueryRequest queryRequest) {
        StringBuilder query = new StringBuilder();

        query.append("INSERT INTO ")
                .append(queryRequest.getTable())
                .append(" (");

        List<QueryRequest.Value> includedValues = queryRequest.getValues().stream()
                .filter(v -> v.getValue() != null && !v.getValue().trim().isEmpty())
                .toList();

        String columnNames = includedValues.stream()
                .map(QueryRequest.Value::getColumn)
                .collect(Collectors.joining(", "));
        query.append(columnNames);
        query.append(")\nVALUES (");

        String values = includedValues.stream()
                .map(v -> shouldQuote(v.getColumnType())
                        ? "'" + escapeSingleQuotes(v.getValue().trim()) + "'"
                        : v.getValue().trim()
                )
                .collect(Collectors.joining(", "));
        query.append(values);
        query.append(");");

        return query.toString();
    }

    public String buildDeleteQuery(QueryRequest queryRequest) {
        StringBuilder query = new StringBuilder();

        query.append("DELETE FROM ").append(queryRequest.getTable());

        if (queryRequest.getWhere() != null && !queryRequest.getWhere().isEmpty()) {
            query.append("\nWHERE ");
            for (int i = 0; i < queryRequest.getWhere().size(); i++) {
                QueryRequest.WhereCondition cond = queryRequest.getWhere().get(i);

                if (i > 0) {
                    query.append(" ").append(cond.getLogicalOperator()).append(" ");
                }

                query.append(cond.getColumn()).append(" ").append(cond.getOperator());

                if ("IS NULL".equalsIgnoreCase(cond.getOperator()) || "IS NOT NULL".equalsIgnoreCase(cond.getOperator())) {
                } else if ("BETWEEN".equalsIgnoreCase(cond.getOperator())) {
                    if (shouldQuote(cond.getColumnType())) {
                        query.append(" '").append(escapeSingleQuotes(cond.getValue()))
                                .append("' AND '").append(escapeSingleQuotes(cond.getValue2())).append("'");
                    } else {
                        query.append(" ").append(cond.getValue()).append(" AND ").append(cond.getValue2());
                    }
                } else if ("IN".equalsIgnoreCase(cond.getOperator())) {
                    if (shouldQuote(cond.getColumnType())) {
                        String quotedValues = Arrays.stream(cond.getValue().split(","))
                                .map(String::trim)
                                .map(v -> "'" + escapeSingleQuotes(v) + "'")
                                .collect(Collectors.joining(", "));
                        query.append(" (").append(quotedValues).append(")");
                    } else {
                        query.append(" (").append(cond.getValue()).append(")");
                    }
                } else {
                    if (shouldQuote(cond.getColumnType())) {
                        query.append(" '").append(escapeSingleQuotes(cond.getValue())).append("'");
                    } else {
                        query.append(" ").append(cond.getValue());
                    }
                }
            }
        }

        return query.toString();
    }

    public String generateFunctionCode(PLSQLRequest functionRequest) {
        StringBuilder function = new StringBuilder();

        function.append("CREATE OR REPLACE FUNCTION ").append(functionRequest.getName());

        if (functionRequest.getParameters() != null && !functionRequest.getParameters().isEmpty()) {
            function.append(" (\n");
            List<String> parameterDeclarations = functionRequest.getParameters().stream()
                    .map(this::buildFunctionParameterDeclaration)
                    .collect(Collectors.toList());
            function.append("    ").append(String.join(",\n    ", parameterDeclarations));
            function.append("\n)");
        } else {
            function.append("()");
        }
        if (functionRequest.getReturnType() != null) {
            function.append("\nRETURNS ");
            function.append(buildDataType(
                    functionRequest.getReturnType().getType(),
                    functionRequest.getReturnType().getSize(),
                    functionRequest.getReturnType().getPrecision()
            ));
        } else {
            function.append("\nRETURNS VOID");
        }

        function.append("\nLANGUAGE plpgsql");
        function.append("\nAS $$\n");

        if (functionRequest.getVariables() != null && !functionRequest.getVariables().isEmpty()) {
            function.append("DECLARE\n");
            for (PLSQLRequest.Variable variable : functionRequest.getVariables()) {
                function.append("    ").append(buildVariableDeclaration(variable)).append("\n");
            }
            function.append("\n");
        }

        function.append("BEGIN\n");

        if (functionRequest.getSteps() != null && !functionRequest.getSteps().isEmpty()) {
            for (PLSQLRequest.Step step : functionRequest.getSteps()) {
                function.append(buildProcedureStep(step, 1));
            }
        }

        function.append("END;\n");
        function.append("$$;");

        return function.toString();
    }

    private String buildFunctionParameterDeclaration(PLSQLRequest.Parameter parameter) {
        StringBuilder paramDecl = new StringBuilder();
        paramDecl.append(parameter.getName()).append(" ");

        if (parameter.getDirection() != null && !parameter.getDirection().isEmpty()) {
            String direction = parameter.getDirection().toUpperCase();
            if ("IN".equals(direction) || "INOUT".equals(direction)) {
                paramDecl.append(direction).append(" ");
            }
        }

        paramDecl.append(buildDataType(parameter.getType(), parameter.getSize(), parameter.getPrecision()));

        return paramDecl.toString();
    }

    private boolean shouldQuote(String columnType) {
        if (columnType == null) return true;
        return switch (columnType.toLowerCase()) {
            case "text", "varchar", "char", "bpchar", "uuid",
                    "date", "time", "timetz", "timestamp", "timestamptz" -> true;
            default -> false;
        };
    }
    private String escapeSingleQuotes(String value) {
        return value.replace("'", "''");
    }

}
