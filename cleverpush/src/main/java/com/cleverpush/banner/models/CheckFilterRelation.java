package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum CheckFilterRelation {
    GreaterThan,
    Equals,
    LessThan,
    Between,
    NotEqual,
    Contains,
    NotContains;

    private static final Map<String, CheckFilterRelation> relations = new HashMap<>();

    static {
        relations.put("greaterThan", CheckFilterRelation.GreaterThan);
        relations.put("equals", CheckFilterRelation.Equals);
        relations.put("lessThan", CheckFilterRelation.LessThan);
        relations.put("between", CheckFilterRelation.Between);
        relations.put("notEqual", CheckFilterRelation.NotEqual);
        relations.put("contains", CheckFilterRelation.Contains);
        relations.put("notContains", CheckFilterRelation.NotContains);
    }

    public static CheckFilterRelation fromString(String raw) {
        if (relations.containsKey(raw)) {
            return relations.get(raw);
        }
        return null;
    }

}
