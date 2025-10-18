# Туториал по запуску: 
1. Установите docker, docker-compose, git

#### Далее
Арч:
```bash 
sudo pacman -S docker docker-compose git
sudo systemctl enable docker.service
sudo systemctl start docker.service
sudo usermod -a -G docker $USER
docker-compose up -d --build // -d опционально, если хотите смотреть логи
```
Винда:

```shell
docker-compose up --build
```

## Архитектура

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Client    │─────▶│ REST API     │─────▶│   Redis     │
│             │      │ Controller   │      │   Queue     │
└─────────────┘      └──────────────┘      └─────────────┘
                                                   │
                                                   ▼
                     ┌──────────────┐      ┌─────────────┐
                     │  PostgreSQL  │◀─────│   Workers   │
                     │   Database   │      │ (Async)     │
                     └──────────────┘      └─────────────┘
                            ▲                      │
                            │                      ▼
                     ┌──────────────┐      ┌─────────────┐
                     │ Rule Engine  │◀─────│ Processing  │
                     │ (4 types)    │      │  Service    │
                     └──────────────┘      └─────────────┘
                            │                      │
                            ▼                      ▼
                     ┌──────────────┐      ┌─────────────┐
                     │  ML Model    │      │Notification │
                     │ (DJL/PyTorch)│      │  Service    │
                     └──────────────┘      └─────────────┘
```
## API