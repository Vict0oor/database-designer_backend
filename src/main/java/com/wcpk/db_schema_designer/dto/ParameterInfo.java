package com.wcpk.db_schema_designer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class  ParameterInfo {
    private String name;
    private String mode;
    private String dataType;
    private int position;
}