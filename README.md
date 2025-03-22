# LINQRA Inventory Service

This microservice manages inventory data for the LINQRA system. It provides RESTful APIs for managing inventory items and communicates with the Product Service for enhanced product availability information.

## Project Structure

```
LINQRA_INVENTORY_SERVICE/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── org/
        │       └── lite/
        │           └── inventory/
        │               ├── InventoryServiceApplication.java
        │               ├── config/
        │               │   ├── EurekaClientConfig.java
        │               │   ├── RestTemplateConfig.java
        │               │   └── SecurityConfig.java
        │               ├── controller/
        │               │   ├── HealthController.java
        │               │   └── InventoryController.java 
        │               ├── filter/
        │               │   └── JwtRoleValidationFilter.java
        │               ├── interceptor/
        │               │   └── ServiceNameInterceptor.java
        │               └── model/
        │                   ├── HealthStatus.java
        │                   ├── InventoryItem.java
        │                   ├── ProductAvailabilityResponse.java
        │                   └── ProductInfo.java
        └── resources/
            └── application.yml
```

## Features

- CRUD operations for inventory items
- Integration with Product Service through an API gateway
- Enhanced product availability information that combines product data with inventory status
- JWT-based security and role validation
- Health check endpoints for service monitoring
- Custom interceptors for service name annotation

## Prerequisites

Before running this service, you must have the following components of the main Linqra application up and running:

1. **Discovery Server (Eureka)** - For service registration and discovery
2. **API Gateway** - For routing requests to the appropriate microservices

These components need to be started before launching this Inventory Service. The service is configured to register with the Eureka server and communicate through the API gateway.

## API Endpoints

### Inventory Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/api/inventory` | Get all inventory items |
| GET    | `/api/inventory/{id}` | Get inventory item by ID |
| POST   | `/api/inventory` | Create a new inventory item |
| PUT    | `/api/inventory/{id}` | Update an existing inventory item |
| DELETE | `/api/inventory/{id}` | Delete an inventory item |

### Service Integration

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/api/inventory/product-availability` | Get product availability information from Product Service |
| GET    | `/api/inventory/product-availability?productId={id}` | Get availability for a specific product |

### Health Checking

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/health` | Get service health status information |

## How to Use

### Starting the Service

1. First, ensure the Linqra Discovery Server and API Gateway services are running
2. Then start the Inventory Service, which will register with Eureka automatically
3. All requests should be routed through the API Gateway

### API Examples (Postman)

#### Get All Inventory Items
- **Method**: GET
- **URL**: `https://localhost:7777/inventory-service/api/inventory`
- **Headers**: `Accept: application/json`

#### Get Specific Inventory Item
- **Method**: GET
- **URL**: `https://localhost:7777/inventory-service/api/inventory/1`
- **Headers**: `Accept: application/json`

#### Create New Inventory Item
- **Method**: POST
- **URL**: `https://localhost:7777/inventory-service/api/inventory`
- **Headers**: 
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Body**:
```json
{
    "name": "Monitor",
    "quantity": 15,
    "price": 349.99
}
```

#### Update Existing Inventory Item
- **Method**: PUT
- **URL**: `https://localhost:7777/inventory-service/api/inventory/1`
- **Headers**: 
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Body**:
```json
{
    "name": "Laptop",
    "quantity": 12,
    "price": 1199.99
}
```

#### Delete Inventory Item
- **Method**: DELETE
- **URL**: `https://localhost:7777/inventory-service/api/inventory/3`

#### Patch Inventory Item (Partial Update)
- **Method**: PATCH
- **URL**: `https://localhost:7777/inventory-service/api/inventory/1`
- **Headers**: 
  - `Content-Type: application/json`
  - `Accept: application/json`
- **Body**:
```json
{
    "quantity": 25
}
```

#### Check Item Existence (HEAD)
- **Method**: HEAD
- **URL**: `https://localhost:7777/inventory-service/api/inventory/1`
- **Headers**: `Accept: application/json`
- **Response Headers**:
  - `X-Item-Found`: indicates if item exists
  - `X-Item-Quantity`: shows current quantity if item exists

#### Get Available Methods (OPTIONS)
- **Method**: OPTIONS
- **URL**: `https://localhost:7777/inventory-service/api/inventory/1`
- **Headers**: `Accept: application/json`
- **Response Headers**:
  - `Allow`: lists all allowed HTTP methods
  - `X-Item-Exists`: indicates if the item exists

#### Get Product Availability
- **Method**: GET
- **URL**: `https://localhost:7777/inventory-service/api/inventory/product-availability`
- **Headers**: `Accept: application/json`

#### Check Service Health
- **Method**: GET
- **URL**: `https://localhost:7777/inventory-service/health`
- **Headers**: `Accept: application/json`

## Configuration

In the `application.yml` file:

```yaml
gateway:
  base-url: https://localhost:7777/inventory-service  # Set to your API gateway URL
```

## Dependencies

This service uses:
- Spring Boot
- Spring Cloud (Eureka Client)
- Project Lombok
- RestTemplate for service communication

## Running the Service

You can run the service using Maven:

```
cd LINQRA_INVENTORY_SERVICE
mvn spring-boot:run
```

## Service Startup Order

For proper functionality, services should be started in the following order:
1. Discovery Server (Eureka)
2. API Gateway
3. Inventory Service (this service)
4. Other microservices (Product Service, etc.)

## Notes

- This service is designed to work with Eureka for service discovery
- Mock data is provided for demonstration purposes
- For production use, replace the in-memory storage with a proper database
- Communication between services happens through the API Gateway