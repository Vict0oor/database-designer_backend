package com.wcpk.db_schema_designer.dto;

import com.wcpk.db_schema_designer.model.Table;

import java.util.List;

public class TablesResponse {
    private List<Table> tables;

    public TablesResponse(List<Table> tables) {
        this.tables = tables;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }
}

