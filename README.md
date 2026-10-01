# Сервис заметок (Notes Service)

Простое API-приложение на Java для управления заметками.

## Требования
- Docker

## Сборка и запуск

1. Собрать образ:
   docker build -t notes-service .

2. Запустить контейнер:
   docker run -d -p 8080:8080 --name my-notes notes-service

3. Проверить работу:
   - GET /notes — список заметок
   - GET /health — проверка статуса

   Пример:
   curl http://localhost:8080/notes

4. Логи:
   docker logs my-notes

5. Остановка:
   docker stop my-notes
