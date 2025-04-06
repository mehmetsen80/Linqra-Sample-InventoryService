package org.lite.inventory.controller;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import lombok.extern.slf4j.Slf4j;
import org.lite.inventory.model.InventoryItem;
import org.lite.inventory.model.ProductAvailabilityResponse;
import org.lite.inventory.model.ErrorResponse;
import org.lite.inventory.model.InventoryItemPatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Tag(name = "Inventory", description = "Inventory management APIs")
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

    @Operation(summary = "Get all inventory items")
    @ApiResponse(responseCode = "200", description = "Found all items",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = InventoryItem.class))))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        return ResponseEntity.ok(new ArrayList<>(inventoryItems.values()));
    }

    @Operation(summary = "Get an inventory item by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Found the item",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InventoryItem.class))),
        @ApiResponse(responseCode = "404", 
                    description = "Item not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getItemById(
        @Parameter(description = "ID of item to be searched") 
        @PathVariable Long id) {
        if (inventoryItems.containsKey(id)) {
            return ResponseEntity.ok(inventoryItems.get(id));
        } else {
            ErrorResponse error = ErrorResponse.of(
                "Item not found with id: " + id,
                "ITEM_NOT_FOUND",
                "/api/inventory/" + id
            );
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
        }
    }

    @Operation(summary = "Create a new inventory item")
    @ApiResponse(responseCode = "201", description = "Item created successfully")
    @PostMapping
    public ResponseEntity<InventoryItem> createItem(
        @Parameter(description = "Item to be created") 
        @RequestBody InventoryItem item) {
        long id = idCounter.getAndIncrement();
        InventoryItem newItem = new InventoryItem(id, item.getName(), item.getQuantity(), item.getPrice());
        inventoryItems.put(id, newItem);
        return new ResponseEntity<>(newItem, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing inventory item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item updated successfully"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> updateItem(
        @Parameter(description = "ID of item to be updated") 
        @PathVariable Long id,
        @Parameter(description = "Updated item details") 
        @RequestBody InventoryItem item) {
        if (inventoryItems.containsKey(id)) {
            InventoryItem updatedItem = new InventoryItem(id, item.getName(), item.getQuantity(), item.getPrice());
            inventoryItems.put(id, updatedItem);
            return ResponseEntity.ok(updatedItem);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete an inventory item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", 
                    description = "Item deleted successfully",
                    content = @Content),
        @ApiResponse(responseCode = "404", 
                    description = "Item not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(
        @Parameter(description = "ID of item to be deleted") 
        @PathVariable Long id) {
        if (inventoryItems.containsKey(id)) {
            inventoryItems.remove(id);
            return ResponseEntity.noContent().build();
        } else {
            ErrorResponse error = ErrorResponse.of(
                "Unable to delete. Item not found with id: " + id,
                "ITEM_NOT_FOUND",
                "/api/inventory/" + id
            );
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
        }
    }

    @Operation(summary = "Partially update an inventory item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Item updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = InventoryItem.class))),
        @ApiResponse(responseCode = "404", 
                    description = "Item not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid field value",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> patchItem(
        @Parameter(description = "ID of item to be patched") 
        @PathVariable Long id,
        @Parameter(description = "Fields to be updated") 
        @RequestBody InventoryItemPatch patch) {
        
        if (!inventoryItems.containsKey(id)) {
            ErrorResponse error = ErrorResponse.of(
                "Unable to update. Item not found with id: " + id,
                "ITEM_NOT_FOUND",
                "/api/inventory/" + id
            );
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
        }

        try {
            InventoryItem existingItem = inventoryItems.get(id);
            
            if (patch.getName() != null) {
                existingItem.setName(patch.getName());
            }
            if (patch.getQuantity() != null) {
                existingItem.setQuantity(patch.getQuantity());
            }
            if (patch.getPrice() != null) {
                existingItem.setPrice(patch.getPrice());
            }

            inventoryItems.put(id, existingItem);
            return ResponseEntity.ok(existingItem);
            
        } catch (Exception e) {
            ErrorResponse error = ErrorResponse.of(
                "Invalid field value: " + e.getMessage(),
                "INVALID_FIELD_VALUE",
                "/api/inventory/" + id
            );
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
        }
    }

    @Operation(summary = "Check if an inventory item exists",
              description = "Returns only headers with item existence and quantity information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Item exists",
                    headers = {
                        @Header(name = "X-Item-Found", description = "Always true for 200 response", schema = @Schema(type = "boolean")),
                        @Header(name = "X-Item-Quantity", description = "Current quantity in stock", schema = @Schema(type = "integer")),
                        @Header(name = "X-Item-Name", description = "Name of the item", schema = @Schema(type = "string")),
                        @Header(name = "X-Item-Price", description = "Current price of the item", schema = @Schema(type = "number"))
                    }),
        @ApiResponse(responseCode = "404", 
                    description = "Item not found",
                    headers = {
                        @Header(name = "X-Error-Code", description = "Error code for not found", schema = @Schema(type = "string")),
                        @Header(name = "X-Error-Message", description = "Error message details", schema = @Schema(type = "string"))
                    })
    })
    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headItem(
        @Parameter(description = "ID of item to check", required = true) 
        @PathVariable Long id) {
        
        if (inventoryItems.containsKey(id)) {
            InventoryItem item = inventoryItems.get(id);
            return ResponseEntity
                .ok()
                .header("X-Item-Found", "true")
                .header("X-Item-Quantity", String.valueOf(item.getQuantity()))
                .header("X-Item-Name", item.getName())
                .header("X-Item-Price", String.valueOf(item.getPrice()))
                .build();
        }
        
        return ResponseEntity
            .notFound()
            .header("X-Error-Code", "ITEM_NOT_FOUND")
            .header("X-Error-Message", "Item not found with id: " + id)
            .build();
    }

    @Operation(summary = "Get available HTTP methods for inventory item",
              description = "Returns allowed HTTP methods and item existence information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Available methods retrieved",
                    headers = {
                        @Header(name = "Allow", 
                               description = "List of allowed HTTP methods", 
                               schema = @Schema(type = "string")),
                        @Header(name = "X-Item-Exists", 
                               description = "Indicates if item exists", 
                               schema = @Schema(type = "boolean")),
                        @Header(name = "X-Available-Operations", 
                               description = "List of available operations", 
                               schema = @Schema(type = "string"))
                    })
    })
    @RequestMapping(value = "/{id}", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsForItem(
        @Parameter(description = "ID of item to check", required = true) 
        @PathVariable Long id) {
        boolean itemExists = inventoryItems.containsKey(id);
        String operations = itemExists ? 
            "get-item,update-item,delete-item,patch-item" : 
            "create-item";
            
        return ResponseEntity
            .ok()
            .allow(HttpMethod.GET, HttpMethod.PUT, HttpMethod.PATCH, 
                  HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS)
            .header("X-Item-Exists", String.valueOf(itemExists))
            .header("X-Available-Operations", operations)
            .header("Access-Control-Allow-Methods", "GET, PUT, PATCH, DELETE, HEAD, OPTIONS")
            .header("Access-Control-Allow-Headers", "Content-Type, Authorization")
            .build();
    }

    @Operation(summary = "Get product availability information",
              description = "Retrieves product information from Product Service and enriches it with inventory status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Successfully retrieved availability information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductAvailabilityResponse.class))),
        @ApiResponse(responseCode = "500", 
                    description = "Error retrieving product information",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "503", 
                    description = "Product Service is unavailable",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/product-availability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProductAvailability(
        @Parameter(description = "Optional product ID to check specific product", 
                  example = "123") 
        @RequestParam(required = false) String productId) {
        
        String url = gatewayBaseUrl + "/product-service/api/product/products";
        if (productId != null) {
            url += "/" + productId;
        }
        
        try {
            ProductAvailabilityResponse response = restTemplate.getForObject(url, ProductAvailabilityResponse.class);
            log.info("Retrieved product information from Product Service: {}", response);
            
            if (response == null) {
                ErrorResponse error = ErrorResponse.of(
                    "No response received from Product Service",
                    "PRODUCT_SERVICE_ERROR",
                    "/api/inventory/product-availability"
                );
                return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(error);
            }
            
            if (response.getProducts() == null || response.getProducts().isEmpty()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
            }
            
            // Enrich product data with inventory availability information
            response.getProducts().forEach(product -> {
                // Here we're simulating checking inventory for the product
                boolean inStock = inventoryItems.values().stream()
                    .anyMatch(item -> item.getName().equalsIgnoreCase(product.getName()) && item.getQuantity() > 0);
                product.setInStock(inStock);
                
                // Add estimated delivery information based on stock status
                product.setEstimatedDelivery(inStock ? "1-2 business days" : "3-4 weeks");
            });
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (Exception e) {
            log.error("Error retrieving product information: {}", e.getMessage());
            ErrorResponse error = ErrorResponse.of(
                "Error communicating with Product Service: " + e.getMessage(),
                "PRODUCT_SERVICE_ERROR",
                "/api/inventory/product-availability" + (productId != null ? "/" + productId : "")
            );
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
        }
    }
}