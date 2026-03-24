<h1 align="center">tcs</h1>
<p align="center" >
  <img alt="Java" src="https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=spring-boot&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white">
  <img alt="Redis" src="https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white">
  <img alt="Prometheus" src="https://img.shields.io/badge/Prometheus-E6522C?logo=prometheus&logoColor=white">
  <img alt="Grafana" src="https://img.shields.io/badge/Grafana-F46800?logo=grafana&logoColor=white">
  <img alt="Docker" src="https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white">
  <img alt="Status" src="https://img.shields.io/badge/status-beta-yellow">
  <img alt="License" src="https://img.shields.io/badge/license-ISC-blue">
</p>

<br>

**tcs** is an intelligent fraud detection system powered by Spring Boot and AI, featuring real-time transaction monitoring, machine learning-based fraud detection, and comprehensive monitoring capabilities.

## Features

- ai-powered fraud detection with machine learning models
- real-time transaction processing with redis queuing
- comprehensive monitoring with prometheus and grafana
- centralized logging with graylog, elasticsearch, and mongodb
- restful api with openapi documentation
- postgresql database with jpa
- spring security validation
- docker compose support
- next.js frontend application
- ollama integration for ai model inference

## Installation

### Prerequisites:

- java 21 
- gradle 7.0+
- postgresql database
- redis
- docker & docker compose 

### From source:

```bash
git clone git@github.com:jvmdevelop/transaction-checker-service.git
cd transaction-checker-service
./gradlew build
./gradlew bootRun
```

### With Docker Compose:

```bash
cd transaction-checker-service
docker-compose up
```

## Usage

### Configuration

Configure your `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/fraud_detection
spring.datasource.username=postgres
spring.datasource.password=deltaq123
spring.redis.host=localhost
spring.redis.port=6379
```

### Running the application:

```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080` 
Frontend will be available at `http://localhost:3000`

## API Endpoints

### Transactions

| Endpoint | Method | Description |
|:---------|:------:|:------------|
| `/api/transactions` | POST | create new transaction |
| `/api/transactions/{id}` | GET | get transaction by ID |
| `/api/transactions/correlation/{correlationId}` | GET | get transaction by correlation ID |

### Monitoring

| service | port | description |
|:---------|:----:|:------------|
| prometheus | 9090 | metrics collection |
| grafana | 3001 | visualization dashboard |
| graylog | 9000 | log management |

## Project Structure

- `src/main/java/com/jvmd/transationapp/` - Main package
  - `controller/` - REST API controllers
  - `service/` - Business logic services
  - `model/` - JPA entities
  - `repository/` - Repository interfaces
  - `dto/` - Data transfer objects
  - `config/` - Configuration classes
- `fraud-detection-frontend/` - Next.js frontend application
- `monitoring/` - Monitoring configuration files
- `ml-model/` - Machine learning model module

## Examples

Create a new transaction:

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000.00,
    "from": "ACC001",
    "to": "ACC002",
    "type": "TRANSFER",
    "timestamp": "2024-01-15T10:30:00",
    "ipAddress": "192.168.1.1",
    "deviceId": "DEV123",
    "location": "New York, NY"
  }'
```

Get transaction by ID:

```bash
curl -X GET http://localhost:8080/api/transactions/550e8400-e29b-41d4-a716-446655440000
```

## Dependencies

- spring boot 3.5.6
- spring data jpa
- spring boot actuator
- spring boot mail
- spring boot webflux
- postgresql driver
- redis
- prometheus
- grafana
- graylog
- elasticsearch
- mongodb
- ollama
- djl 
- spring ai
- lombok
- docker compose

## AI Integration

The application integrates with AI models for:
- fraud detection and risk scoring
- anomaly detection in transaction patterns
- real-time decision making
- pattern recognition and analysis

## Monitoring & Logging

- prometheus: metrics collection and monitoring
- grafana: data visualization and dashboards
- graylog: centralized log management
- elasticsearch: log indexing and search
- mongodb: log storage

## Contributing

1. fork the repository
2. create a feature branch
3. submit a pull request

## License

ISC — see [LICENSE](LICENSE) for details.

## EOF
