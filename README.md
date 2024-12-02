# LSEG
Data processing application

## Running locally

This project uses Java 21 and Gradle 8.11.1

Add data to be processed to the input folder which is configured in the application.yaml. Sample data provided

```
lseg:
  input:
    path: ./inputData
```

To start locally run:
```gradlew bootRun```

Running the application with Swagger UI: http://localhost:8081/api/swagger-ui/index.html

Running the application with cURL: 
``curl -X GET "http://localhost:8081/api/stock?fileLimit=2"``
