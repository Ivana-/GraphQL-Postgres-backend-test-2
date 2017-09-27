# GraphQL Postgres backend

В качестве шаблона проекта был взят этот демо-пример: [graphql-clj-starter](https://github.com/tendant/graphql-clj-starter)

Часть, касающаяся фронтенда, оставлена из оригинального примера, но весь бэкенд полностью переработан для работы с базой Postgresql. Проект содержит демопример работы с тестовой базой из вэб-интерфейса GraphiQL.

#### Установка и использование:

- создать базу Postgresql из [бекапа](https://github.com/Ivana-/GraphQL-Postgres-backend-test-2/blob/master/db_backup/)

- задать ваши параметры подключения к базе (dbname, user, password) в файле [db_spec.clj](https://github.com/Ivana-/GraphQL-Postgres-backend-test-2/blob/master/src/graphql_postgres_clj/db_spec.clj)

- запуск сервера `lein ring server-headless`

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
            # etc....
          }
        }
      }
    }
  }
}
```

алиасы полей (в т.ч. для дублирующихся на одном уровне)

```
{
  users {
    name
    name_too: name
    posts {
      text: title
    }
  }
  users_again: users {
    id
    name
  }
}
```

типы-объединения и инлайн-фрагменты

```
{
  users {
    name
    messages {
      id
      ... on Post {
        title
      }
      ... on Comment {
        text
        author {
          name
        }
      }
    }
  }
}
```

а также любые комбинации приведенных вариантов.

При загрузке/обновлении страницы в окне результата показывается текст GraphQL-схемы, с указанием типов и полей, какие можно запрашивать. При отсутствии запрашиваемого поля в схеме возвращается ответ в виде списка ошибок - попробуйте выполнить запрос

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

- фрагменты

- интерфейсы

- директивы

- мутаторы

- проверка соответствия аргументов ожидаемым типам и заполнение обязательных аргументов

- валидация результата запроса на нарушение non-nullable полей

- и т.д.
