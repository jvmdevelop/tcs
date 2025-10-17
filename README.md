# Fraud Detection System

Комплексная система обнаружения мошеннических транзакций с использованием Java, PostgreSQL, Redis и Machine Learning.

## 🎯 Основные возможности

### 1. Прием и очередь транзакций
- ✅ REST API endpoint для приема транзакций (POST `/api/transactions`)
- ✅ Полная валидация входных данных с подробными сообщениями об ошибках
- ✅ Redis-based очередь для асинхронной обработки
- ✅ Correlation ID для трассировки на всех этапах
- ✅ Защита от дубликатов транзакций
- ✅ Конфигурируемое количество worker threads

### 2. Движок правил
Поддержка четырех типов правил:

#### Threshold Rules
Пороговые правила для сравнения числовых полей:
```json
{
  "field": "amount",
  "operator": ">",
  "value": 100000
}
```

#### Pattern Rules
Обнаружение паттернов поведения:
```json
{
  "type": "multiple_small_transactions",
  "timeWindowMinutes": 10,
  "minTransactions": 5,
  "maxAmountPerTransaction": 1000,
  "accountField": "from"
}
```

#### Composite Rules
Комбинация условий (AND/OR/NOT):
```json
{
  "operator": "AND",
  "conditions": [
    {"type": "amount", "operator": ">", "value": 50000},
    {"type": "nighttime", "startHour": 22, "endHour": 6}
  ]
}
```

#### ML Rules
Интеграция с предобученной ML моделью:
```json
{
  "modelPath": "models/fraud_model.pt",
  "threshold": 0.7,
  "modelVersion": "1.0"
}
```

**Особенности:**
- ✅ Все правила хранятся в БД
- ✅ Hot-reload правил без перезапуска
- ✅ История изменений всех правил
- ✅ Приоритизация и короткое замыкание
- ✅ Аудит выполнения правил

### 3. Отчетность и уведомления
- ✅ Структурированное JSON логирование с correlation ID
- ✅ Три канала уведомлений: Email, Telegram, Webhook
- ✅ Настраиваемые шаблоны сообщений
- ✅ Retry механизм для failed notifications
- ✅ Маршрутизация по уровню критичности
- ✅ Полная история уведомлений в БД

### 4. Админ-панель
Минималистичная веб-панель на Thymeleaf:
- ✅ Dashboard со статистикой (processed, alerted, reviewed)
- ✅ Просмотр и фильтрация транзакций
- ✅ Детальная карточка транзакции с correlation ID и историей
- ✅ CRUD управление правилами
- ✅ История изменений правил
- ✅ Экспорт данных в CSV
- ✅ Возможность пометить транзакцию как reviewed

## 🏗️ Архитектура

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

## 🚀 Быстрый старт

### Предварительные требования
- Java 21
- Docker & Docker Compose
- Gradle 8.5+

### Запуск с Docker Compose

```bash
# Клонировать проект
cd transation-app

# Запустить все сервисы
docker-compose up -d

# Проверить логи
docker-compose logs -f app
```

Сервисы будут доступны на:
- **Приложение**: http://localhost:8080
- **Админ-панель**: http://localhost:8080/admin
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

### Локальный запуск для разработки

```bash
# Запустить только инфраструктуру
docker-compose up -d postgres redis prometheus grafana

# Запустить приложение
./gradlew bootRun
```

## 📊 Мониторинг и метрики

### Prometheus метрики
- `transactions.processed` - количество обработанных транзакций
- `transactions.alerted` - количество алертов с тегом severity
- `transactions.reviewed` - количество просмотренных транзакций
- `transactions.errors` - количество ошибок обработки
- `transactions.processing.time` - время обработки транзакций
- `rules.execution.time` - время выполнения правил
- `notifications.sent` - количество отправленных уведомлений

### Структурированное логирование
Все логи в JSON формате с полями:
- `timestamp` - время события
- `level` - уровень логирования
- `correlationId` - ID корреляции
- `component` - компонент системы
- `message` - сообщение
- `logger` - имя логгера

### Grafana Dashboards
Импортируйте готовые дашборды из `monitoring/` директории для визуализации:
- Пропускная способность транзакций
- Количество алертов по severity
- Латентность обработки
- Статус очереди

## 🔧 Конфигурация

### Переменные окружения

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fraud_detection
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=deltaq123

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# Email
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-app-password

# Telegram
TELEGRAM_BOT_TOKEN=your-bot-token
TELEGRAM_ENABLED=true

# Webhook
WEBHOOK_ENABLED=true
WEBHOOK_URL=https://your-webhook-url.com/alerts
```

## 📝 API Документация

### POST /api/transactions
Создать новую транзакцию

**Request:**
```json
{
  "amount": 150000,
  "from": "ACC123",
  "to": "ACC456",
  "type": "TRANSFER",
  "timestamp": "2024-01-17T23:45:00",
  "ipAddress": "192.168.1.1",
  "deviceId": "device-123",
  "location": "Moscow"
}
```

**Response (202 Accepted):**
```json
{
  "id": "uuid",
  "correlationId": "corr-uuid",
  "amount": 150000,
  "from": "ACC123",
  "to": "ACC456",
  "type": "TRANSFER",
  "timestamp": "2024-01-17T23:45:00",
  "status": "PROCESSING",
  "message": "Transaction accepted and queued for processing"
}
```

### GET /api/transactions/{id}
Получить статус транзакции

### GET /api/transactions/correlation/{correlationId}
Получить транзакцию по correlation ID

## 🧪 Тестирование

### Создание тестовой транзакции

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 150000,
    "from": "ACC001",
    "to": "ACC002",
    "type": "TRANSFER",
    "timestamp": "2024-01-17T23:45:00"
  }'
```

### Пример ответа с ошибкой валидации

```json
{
  "timestamp": "2024-01-17T23:45:20",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "details": [
    "amount: Amount must be greater than 0",
    "from: From account is required"
  ],
  "correlationId": "corr-abc-123"
}
```

## 🗄️ Структура БД

Основные таблицы:
- `transactions` - все транзакции
- `rules` - правила обнаружения
- `rule_change_history` - история изменений правил
- `notification_config` - конфигурации уведомлений
- `notification_log` - лог отправленных уведомлений

## 🔒 Безопасность

- Валидация всех входных данных
- Sanitization параметров
- Prepared statements для защиты от SQL injection
- Rate limiting (можно добавить через Spring Security)
- Audit trail для всех изменений правил

## 📈 Производительность

- Асинхронная обработка через Redis queue
- Конфигурируемый пул worker threads (по умолчанию 5)
- Connection pooling для БД
- Кэширование активных правил
- Оптимизированные индексы в БД

## 🤝 Разработка

### Добавление нового типа правила

1. Добавить тип в enum `RuleType`
2. Создать evaluator класс
3. Зарегистрировать в `RuleEngine`
4. Добавить пример конфигурации в админ-панель

### Добавление нового канала уведомлений

1. Добавить канал в enum `NotificationChannel`
2. Создать sender класс
3. Зарегистрировать в `NotificationService`
4. Добавить конфигурацию в `DataInitializationService`

## 📦 ML Model

Для использования ML правил:

1. Обучите модель используя `ml-model` модуль
2. Сохраните модель в `models/fraud_model.pt`
3. Включите ML правило в админ-панели
4. Настройте threshold в конфигурации правила

## 🐛 Troubleshooting

### Проблемы с подключением к БД
```bash
docker-compose logs postgres
```

### Очередь не обрабатывается
```bash
docker-compose logs redis
# Проверить worker threads в логах приложения
```

### ML модель не загружается
- Убедитесь что файл модели существует в `models/`
- Проверьте формат модели (должен быть PyTorch)
- Посмотрите логи при старте приложения

## 📄 Лицензия

MIT License

## 👥 Контакты

Для вопросов и предложений создавайте issue в репозитории.
