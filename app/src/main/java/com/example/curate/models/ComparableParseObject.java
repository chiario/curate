package com.example.curate.models;

import com.parse.ParseObject;

public class ComparableParseObject extends ParseObject {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ComparableParseObject) {
            ComparableParseObject parseObject = (ComparableParseObject) o;
            return getObjectId().equals(parseObject.getObjectId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getObjectId().hashCode();
    }
}
