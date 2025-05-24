package com.wcpk.db_schema_designer.service;

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
