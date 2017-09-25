# GraphQL Postgres backend

Тестовый пример был взят отсюда [graphql-clj-starter](https://github.com/tendant/graphql-clj-starter)

Часть, касающаяся фронтенда, была оставлена из оригинального примера, но бэкенд весь переработан на основе подключения к базе Postgresql.

Добавлены 3 пространства имен:

[graphql_parser.clj](https://github.com/Ivana-/GraphQL-Postgres-backend-test-2/blob/master/src/graphql_clj_starter/graphql_parser.clj)

[schema.clj](https://github.com/Ivana-/GraphQL-Postgres-backend-test-2/blob/master/src/graphql_clj_starter/schema.clj)

[test_postgres.clj](https://github.com/Ivana-/GraphQL-Postgres-backend-test-2/blob/master/src/graphql_clj_starter/test_postgres.clj)

а также грамматика для Antlr (взят готовый существующий вариант и переработан)

[GraphQL.g4](https://github.com/Ivana-/GraphQL-Postgres-backend-test-2/blob/master/public/GraphQL.g4)


В текущей версии закомментированы блоки экспериментов с реализацией функционала фрагментов GraphQL, чтобы пример был работоспособен на уровне того функционала, который реализован. При наличии базы Postgresql с нужным форматом таблиц, можно проверить работу примера в браузере.
