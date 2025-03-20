package org.lite.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAvailabilityResponse {
    private List<ProductInfo> products;
    private String timestamp;
    private String serviceSource = "product-service";
    private String productStatus;
} 