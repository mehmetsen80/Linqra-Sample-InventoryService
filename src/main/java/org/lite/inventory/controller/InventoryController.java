package org.lite.inventory.controller;

import lombok.extern.slf4j.Slf4j;
import org.lite.inventory.model.InventoryItem;
import org.lite.inventory.model.ProductAvailabilityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final RestTemplate restTemplate;
    private final Map<Long, InventoryItem> inventoryItems = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    
    @Value("${gateway.base-url:http://localhost:8080}")
    private String gatewayBaseUrl;

    @Autowired
    public InventoryController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        // Initialize with some mock data
        addMockItem("Laptop", 10, 999.99);
        addMockItem("Smartphone", 20, 699.99);
        addMockItem("Headphones", 30, 149.99);
    }

    private void addMockItem(String name, int quantity, double price) {
        long id = idCounter.getAndIncrement();
        inventoryItems.put(id, new InventoryItem(id, name, quantity, price));
    }

    // GET all inventory items
    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        return ResponseEntity.ok(new ArrayList<>(inventoryItems.values()));
    }

    // GET a specific inventory item by ID
    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getItemById(@PathVariable Long id) {
        if (inventoryItems.containsKey(id)) {
            return ResponseEntity.ok(inventoryItems.get(id));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // POST a new inventory item
    @PostMapping
    public ResponseEntity<InventoryItem> createItem(@RequestBody InventoryItem item) {
        long id = idCounter.getAndIncrement();
        InventoryItem newItem = new InventoryItem(id, item.getName(), item.getQuantity(), item.getPrice());
        inventoryItems.put(id, newItem);
        return new ResponseEntity<>(newItem, HttpStatus.CREATED);
    }

    // PUT update an existing inventory item
    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable Long id, @RequestBody InventoryItem item) {
        if (inventoryItems.containsKey(id)) {
            InventoryItem updatedItem = new InventoryItem(id, item.getName(), item.getQuantity(), item.getPrice());
            inventoryItems.put(id, updatedItem);
            return ResponseEntity.ok(updatedItem);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE an inventory item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        if (inventoryItems.containsKey(id)) {
            inventoryItems.remove(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // PATCH - Update partial inventory item
    @PatchMapping("/{id}")
    public ResponseEntity<InventoryItem> patchItem(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        if (!inventoryItems.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }

        InventoryItem existingItem = inventoryItems.get(id);
        
        // Apply only the fields that are present in the update request
        if (updates.containsKey("name")) {
            existingItem.setName((String) updates.get("name"));
        }
        if (updates.containsKey("quantity")) {
            existingItem.setQuantity(((Number) updates.get("quantity")).intValue());
        }
        if (updates.containsKey("price")) {
            existingItem.setPrice(((Number) updates.get("price")).doubleValue());
        }

        inventoryItems.put(id, existingItem);
        return ResponseEntity.ok(existingItem);
    }

    // HEAD - Check if inventory item exists (returns only headers, no body)
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headItem(@PathVariable Long id) {
        if (inventoryItems.containsKey(id)) {
            return ResponseEntity
                .ok()
                .header("X-Item-Found", "true")
                .header("X-Item-Quantity", String.valueOf(inventoryItems.get(id).getQuantity()))
                .build();
        }
        return ResponseEntity.notFound().build();
    }

    // OPTIONS - Show available methods for inventory items
    @RequestMapping(value = "/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsForItem(@PathVariable Long id) {
        return ResponseEntity
            .ok()
            .allow(HttpMethod.GET, HttpMethod.PUT, HttpMethod.PATCH, 
                  HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS)
            .header("X-Item-Exists", String.valueOf(inventoryItems.containsKey(id)))
            .build();
    }

    @GetMapping(value = "/product-availability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductAvailabilityResponse> getProductAvailability(
            @RequestParam(required = false) String productId) {
        String url = gatewayBaseUrl + "/product-service/api/product/products";
        if (productId != null) {
            url += "/" + productId;
        }
        
        try {
            ProductAvailabilityResponse response = restTemplate.getForObject(url, ProductAvailabilityResponse.class);
            log.info("Retrieved product information from Product Service: {}", response);
            
            // Enrich product data with inventory availability information
            if (response != null && response.getProducts() != null) {
                response.getProducts().forEach(product -> {
                    // Here we're simulating checking inventory for the product
                    boolean inStock = inventoryItems.values().stream()
                        .anyMatch(item -> item.getName().equalsIgnoreCase(product.getName()) && item.getQuantity() > 0);
                    product.setInStock(inStock);
                    
                    // Add estimated delivery information based on stock status
                    product.setEstimatedDelivery(inStock ? "1-2 business days" : "3-4 weeks");
                });
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
        } catch (Exception e) {
            log.error("Error retrieving product information", e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}