package com.example.business.items;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

public class Item {

    private final Long id;
    private final String name;

    @JsonbCreator
    public Item(@JsonbProperty("id") Long id, @JsonbProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
