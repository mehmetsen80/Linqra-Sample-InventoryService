package org.lite.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfo {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    
    // Inventory-related fields that get enriched by the inventory service
    private boolean inStock;
    private Integer availableQuantity;
    private String estimatedDelivery;
    private String warehouseLocation;
} 