package com.example.business.items;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class ItemRepository {

    private final AtomicLong atomicLong = new AtomicLong(3);
    private final Set<Item> items;

    public ItemRepository() {
        this.items = new HashSet(Arrays.asList(new Item(1L, "Item 1"), new Item(2L, "Item 2"), new Item(3L, "Item 3")));
    }

    public Item create(CreateItemCommand command) {
        var item = new Item(atomicLong.incrementAndGet(), command.getName());
        
        items.add(item);
        
        return item;
    }
    
    public Optional<Item> getById(Long id) {
        return items.stream().filter(item -> item.getId().equals(id)).findAny();
    }

    public Set<Item> getItems() {
        return items;
    }
}