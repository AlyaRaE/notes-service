# notes-service (Сервис заметок)

Учебный проект по предмету DevSecOps. Простой сервис для создания, редактирования и хранения заметок.

Проект состоит из трёх микросервисов, которые выполняют отдельные задачи и взаимодействуют между собой через HTTP-запросы.

## Возможности:

- Создание заметок
- Просмотр заметок
- Редактирование заметок
- Удаление заметок
- Создание и просмотр тегов
- Отправка уведомлений при создании новой заметки
- Хранение данных в PostgreSQL

## Технологии

- Java 17 (чистый JDK, без Spring)
- PostgreSQL 
- JDBC (PostgreSQL JDBC Driver)
- Docker
- Docker Compose
- HTTP
- curl для проверки API

## Структура проекта

```text
notes-service/
│
├── App.java
├── Dockerfile
├── docker-compose.yml
├── postgresql-42.7.3.jar
├── README.md
│
├── tags-service/
│   ├── App.java
│   └── Dockerfile
│
└── notifications-service/
    ├── App.java
    └── Dockerfile
```

## Микросервисы

### 1. Notes Service

Основной сервис для работы с заметками.

Возможности:
- Создание заметок
- Просмотр всех заметок
- Изменение заметок
- Удаление заметок
- Сохранение заметок в PostgreSQL
- Отправка HTTP-уведомления при создании заметки

Порт: `8080`

### 2. Tags Service

Сервис для работы с тегами.

Возможности:
- Создание тегов
- Просмотр всех тегов
- Сохранение тегов в PostgreSQL

Порт: `8081`

### 3. Notifications Service

Сервис для получения уведомлений.

Возможности:
- Получение HTTP POST-запросов от Notes Service
- Вывод уведомлений в консольные логи

Порт: `8082`

## Запуск проекта

### Через Docker Compose

Проект запускается вместе с тремя микросервисами и базой данных PostgreSQL.

1. Запустить все сервисы:

```bash
docker compose up -d --build
```

2. Проверить статус контейнеров:

```bash
docker compose ps
```

Ожидается четыре работающих контейнера:

- `api` — Notes Service
- `tags` — Tags Service
- `notifications` — Notifications Service
- `db` — PostgreSQL

3. Посмотреть логи всех сервисов:

```bash
docker compose logs
```

4. Посмотреть логи Notes Service:

```bash
docker compose logs api
```

5. Посмотреть логи Notifications Service:

```bash
docker compose logs notifications
```

## Примеры запросов

Для выполнения запросов используется `curl`.

### Notes Service

Создать заметку:

```bash
curl -X POST http://localhost:8080/notes \
-H "Content-Type: application/json" \
-d '{"text":"Buy bread"}'
```

Получить все заметки:

```bash
curl http://localhost:8080/notes
```

Обновить заметку:

```bash
curl -X PUT http://localhost:8080/notes/1 \
-H "Content-Type: application/json" \
-d '{"text":"Do homework"}'
```

Удалить заметку:

```bash
curl -X DELETE http://localhost:8080/notes/1
```

### Tags Service

Создать тег:

```bash
curl -X POST http://localhost:8081/tags \
-H "Content-Type: application/json" \
-d '{"name":"College"}'
```

Получить все теги:

```bash
curl http://localhost:8081/tags
```

### Notifications Service

Notifications Service получает уведомления автоматически при создании новой заметки через Notes Service.

Посмотреть полученные уведомления:

```bash
docker compose logs notifications
```

Для отдельной проверки Notifications Service можно отправить запрос:

```bash
curl -X POST http://localhost:8082/notifications \
-H "Content-Type: application/json" \
-d '{"message":"Test notification"}'
```

## Проверка сохранения данных после перезапуска

Для проверки постоянного хранения данных:

1. Остановить контейнеры:

```bash
docker compose down
```

2. Запустить проект снова:

```bash
docker compose up -d
```

3. Проверить, что заметки остались:

```bash
curl http://localhost:8080/notes
```

4. Проверить, что теги остались:

```bash
curl http://localhost:8081/tags
```

Команда `docker compose down -v` удаляет volume вместе с сохранёнными данными. Использовать её нужно только при намеренном удалении базы.

## Остановка проекта

```bash
docker compose down
```

## Результат

В проекте реализованы три отдельных Java-микросервиса, PostgreSQL и запуск всей системы через Docker Compose. Сервисы взаимодействуют через HTTP, а данные сохраняются в постоянном Docker volume.

## Архитектура проекта (C4 Model)

### 1. Context (Уровень 1)
Пользователь взаимодействует с сервисом заметок через HTTP-запросы. Сервис сохраняет данные в PostgreSQL.

### 2. Container (Уровень 2)
**API Gateway / Load Balancer**: Входная точка, распределяет запросы.
**API Service (Java)**: Основная логика приложения (обработка заметок).
**PostgreSQL**: Основное хранилище данных.
**Redis Cache**: Дополнительный компонент для кэширования. (В итоге не использовался)

### 3. Component (Уровень 3)
Внутри API Service:
**Controller**: Принимает HTTP-запросы.
**Service**: Бизнес-логика (создание, редактирование заметок).
**Repository**: Взаимодействие с PostgreSQL.
  
## Граф веток
[GitHub Network](https://github.com/AlyaRaE/notes-service/network)

## Конфликт
Подробнее — в [git-conflict.md](git-conflict.md).

