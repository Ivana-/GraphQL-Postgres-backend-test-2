# GraphQL Postgres backend

В качестве шаблона проекта был взят этот демо-пример: [graphql-clj-starter](https://github.com/tendant/graphql-clj-starter)

Часть, касающаяся фронтенда, была оставлена из оригинального примера, но весь бэкенд переработан для работы с базой Postgresql. Проект содержит демопример работы с тестовой базой из вэб-интерфейса GraphiQL.

#### Установка и использование:

- создать базу Postgresql из [бекапа](https://github.com/Ivana-/GraphQL-Postgres-backend-test-2/blob/master/db_backup/)

- задать ваши параметры подключения к базе в файле [db-spec.clj](https://github.com/Ivana-/GraphQL-Postgres-backend-test-2/blob/master/src/graphql_postgres_clj/db-spec.clj)

- запустить сервер `lein ring server-headless`

- доступ к интерфейсу в браузере `http://localhost:3002/index.html`

#### Примеры запросов:

объекты с полями-примитивами

```
{
  users {
    id
    name
  }
}
```

независимые подзапросы

```
{
  users {
    id
    name
  }
  posts {
    title
  }
}
```

вложенные объекты

```
{
  users {
    id
    name
    posts {
      id
      title
    }
  }
}
```

передача аргументов

```
{
  user(id: 2) {
    id
    name
    comments {
      text
      author {
        id
        name
      }
    }
  }
}
```

циклические ссылки

```
{
  user(id: 1) {
    name
    comments {
      text
      author {
        name
        comments {
          text
          author {
            name
            comments {
              text
              author {
                name
                comments {
                  text
                  author {
                    name
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

и любые комбинации приведенных вариантов.

В правом верхнем углу страницы есть меню `Docs` с навигацией по схеме, типам и полям, какие можно запрашивать. При отсутствии запрашиваемого поля в схеме возвращается ответ в виде списка ошибок - попробуйте выполнить запрос

```
{
  users {
    id
    first_name
  }
}
```

#### TODO

- параметрические запросы

- фрагменты, инлайн-фрагменты

- директивы

- типы-суммы и интерфейсы

- проверка соответствия аргументов ожидаемым типам и заполнение обязательных аргументов

- валидация результата запроса на нарушение non-nullable полей

- и т.д.
