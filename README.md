## Launch Tutorial: 
1. Install docker, docker-compose, git
2. Unpack the dataset that was provided to the root of the project
#### Next
Arch:
``bash
sudo pacman -S docker docker-compose git
sudo systemctl enable docker.service
sudo systemctl start docker.service
sudo usermod -a -G docker $USER
docker -compose up -d --build // -d optional if you want to view logs
``
Windows:

```shell
docker-compose up --build
```

The application runs on localhost:3000

## Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Client    │────▶│ REST API     │────▶│   Redis     │
│             │      │ Controller   │      │   Queue     │
└─────────────┘      └──────────────┘      └─────────────┘
                                                   │
                                                   ▼
                     ┌──────────────┐      ┌─────────────┐
                     │  PostgreSQL  │◀────│   Workers   │
                     │   Database   │      │             │
                     └──────────────┘      └─────────────┘
                            ▲                      │
                            │                      ▼
                     ┌──────────────┐      ┌─────────────┐
                     │ Rule Engine  │◀─────│ Processing │
                     │ (4 types)    │      │  Service    │
                     └──────────────┘      └─────────────┘
                            │                      │
                            ▼                      ▼
                     ┌──────────────┐      ┌─────────────┐
                     │  ML Model    │      │Notification │
                     │              │      │  Service    │
                     └──────────────┘      └─────────────┘
```
## API
OpenApi 
```
http://localhost:8080/swagger-ui.html - swagger
```
If you want to conveniently view the endpoints, use swagger, if you want to get a description, use the tables below :)

### Transactions

| Method | Endpoint | Description |
|-------|----------|----------|
| POST | `/api/transactions` | Creating a new transaction |
| GET | `/api/transactions/{id}` | Getting a transaction by ID |
| GET | `/api/transactions/correlation/{correlationId}` | Getting a transaction by correlation ID |

### Admin panel - Dashboard

| Method | Endpoint | Description |
|-------|----------|----------|
| GET | `/api/admin/dashboard/stats` | Getting dashboard statistics |
| GET | `/api/admin/dashboard/recent-transactions` | Getting the latest transactions |

### Admin Panel - Transactions

| Method | Endpoint | Description |
|-------|----------|----------|
| GET | `/api/admin/transactions` | Getting a list of transactions with filtering and pagination |
| GET | `/api/admin/transactions/{id}` | Getting detailed information about a transaction |
| POST | `/api/admin/transactions/{id}/review` | Mark the transaction as verified |
| GET | `/api/admin/transactions/search` | Search for transactions based on various criteria |

### Admin Panel - Rules

| Method | Endpoint | Description |
|-------|----------|----------|
| GET | `/api/admin/rules` | Getting a list of all rules |
| GET | `/api/admin/rules/{id}` | Getting a rule by ID |
| POST | `/api/admin/rules` | Creating a new rule |
| PUT | `/api/admin/rules/{id}` | Updating an existing rule |
| PATCH | `/api/admin/rules/{id}/toggle` | Enable/disable the rule |
| DELETE | `/api/admin/rules/{id}` | Deleting a rule |
| GET | `/api/admin/rules/{id}/history` | Getting the history of rule changes |
| GET | `/api/admin/rules/types` | Getting the available types of rules |

### Admin panel - Reference books

| Method | Endpoint | Description |
|-------|----------|----------|
| GET | `/api/admin/statuses` | Getting available transaction statuses |
