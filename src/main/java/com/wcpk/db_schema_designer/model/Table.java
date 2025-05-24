package com.wcpk.db_schema_designer.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Table {
    private String name;
    private List<Column> columns = new ArrayList<>();

    public Table(String tableName) {
        this.name=tableName;
    }

    public void addColumn(Column column) {
        columns.add(column);
    }
}
