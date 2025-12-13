package com.soyesenna.helixquery.entity;

import com.soyesenna.helixquery.annotations.GenerateFields;
import com.soyesenna.helixquery.annotations.IgnoreField;

@GenerateFields
public class IgnoredFieldModel {

    private Long id;

    @IgnoreField
    private String secret;

    private String visible;
}

