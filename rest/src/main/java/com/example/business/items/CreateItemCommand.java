package com.example.business.items;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

public class CreateItemCommand {
    
    private final String name;

    @JsonbCreator
    public CreateItemCommand(@JsonbProperty("name") String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
