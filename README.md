# notes-service (Сервис заметок)

Учебный проект по предмету DevSecOps. Простой сервис для создания,
редактирования и хранения заметок.

## Возможности:

Создание заметок
Редактирование заметок
Удаление заметок
Поиск по заметкам

## Технологии

Java 17 (чистый JDK, без сторонних библиотек)
Docker

## Структура проекта
`App.java` — Исходный код API-приложения (все эндпоинты)
`Dockerfile` — Инструкция для сборки Docker-образа
`README.md` — описание проекта
`project-notes.md` — рабочие заметки по проекту
`api-plan.md` — план API
`git-conflict.md` — описание конфликта и его решения

## Запуск

### Локально (без Docker)

1. Скомпилировать:
   `javac App.java`
2. Запустить:
   `java App`
3. Проверить работу:
   `curl http://localhost:8080/notes`
4. Остановить: `Ctrl+C`

## Через Docker

1. Собрать образ:
   `docker build -t notes-service .`
2. Запустить контейнер:
   `docker run -d -p 8080:8080 --name my-notes notes-service`
3. Проверить, что контейнер работает:
   `docker ps`
4. Проверить API:
   `curl http://localhost:8080/notes`
5. Посмотреть логи:
   `docker logs my-notes`
6. Остановить и удалить контейнер:
   `docker stop my-notes`
   `docker rm my-notes`

## Примеры запросов

### Создать заметку: 
  `curl -X POST http://localhost:8080/notes \
  -H "Content-Type: application/json" \
  -d '{"text":"Купить хлеб"}'`

### Обновить заметку:
  `curl -X PUT http://localhost:8080/notes/1 \
  -H "Content-Type: application/json" \
  -d '{"text":"Купить молоко"}'`

### Удалить заметку:
  `curl -X DELETE http://localhost:8080/notes/1`
  
## Граф веток
[GitHub Network](https://github.com/AlyaRaE/notes-service/network)

## Конфликт
Подробнее — в [git-conflict.md](git-conflict.md).

