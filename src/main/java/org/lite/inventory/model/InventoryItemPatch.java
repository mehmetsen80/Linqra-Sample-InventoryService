package org.lite.inventory.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemPatch {
    private String name;
    private Integer quantity;
    private Double price;
} 