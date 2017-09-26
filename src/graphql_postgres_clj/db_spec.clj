(ns graphql-postgres-clj.db-spec)

; параметры подключения к базе данных postgres
; jdbc:postgresql://localhost:5432/contactdb
(def db-spec
  {:dbtype "postgresql"
   :host "localhost"
   :dbname "contactdb"
   :user "postgres"
   :password "postgress"})
