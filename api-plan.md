# API Plan

## Endpoints

`GET /notes` — получить список заметок
`POST /notes` — создать заметку
`PUT /notes/{id}` — обновить заметку
`DELETE /notes/{id}` — удалить заметку

## Формат заметки
{
  "id": 1,
  "title": "Заголовок",
  "body": "Текст заметки",
  "created_at": "2024-01-01"
}