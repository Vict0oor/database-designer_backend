package com.wcpk.db_schema_designer.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Column {
    private String name;
    private String type;

    public Column(String columnName, String dataType) {
        this.name=columnName;
        this.type=dataType;
    }
}
