(ns graphql-postgres-clj.postgres
  (:require [clojure.java.jdbc :as jdbc]
            [graphql-postgres-clj.db-spec :as db-spec]
            [graphql-postgres-clj.schema :as schema]
            [graphql-postgres-clj.graphql-parser :as graphql-parser]
            [cheshire.core :as json]
            ))

; модуль экспортирует функцию execute [query variables operation-name]
; которая выполняет graphql-запрос и возвращает json для отправки клиенту


;------------------------------------------------------------------------------------
; копипаста из библиотеки clj-postgresql - для получения json из PGobject
; вся библиотека весьма обширна, дает интерфейс ко всем типам Postgres
; нам пока нужен только json

(defmulti read-pgobject
          "Convert returned PGobject to Clojure value."
          #(keyword (when % (.getType ^org.postgresql.util.PGobject %))))

(defmethod read-pgobject :json
  [^org.postgresql.util.PGobject x]
  (when-let [val (.getValue x)]
    (json/parse-string val)))

(extend-protocol jdbc/IResultSetReadColumn

  ;; Covert java.sql.Array to Clojure vector
  java.sql.Array
  (result-set-read-column [val _ _]
    (into [] (.getArray val)))

  ;; PGobjects have their own multimethod
  org.postgresql.util.PGobject
  (result-set-read-column [val _ _]
    (read-pgobject val)))
;------------------------------------------------------------------------------------
; конец копипасты



; собственно функция получения ответа на входящий graphql-запрос
; сначала преобразуем текст graphql-запроса в стандартизованное ast
; потом получим по этому ast текст sql-запроса и список ошибок при построении
; если список ошибок пуст - делаем запрос в Postgres и возвращаем результат
; (пока как есть, без постобработки - проверки на null-ы в полях и т.п.)
; если есть ошибки - не лезем в базу а возвращаем их в json-е клиенту
(defn execute-core [query variables operation-name]
  (let [ast (graphql-parser/graphql-text-to-ast query)
        sql-query (schema/ast-to-sql-text-and-errors ast)
        sql-query-text (:sql-text sql-query)
        sql-query-erros (:errors sql-query)]
    (if (empty? sql-query-erros)
      (let [query-rezult (jdbc/query db-spec/db-spec sql-query-text)]
        (first query-rezult))
      (json/generate-string {:errors sql-query-erros}) )))

; обертка нашей функции ответов на запросы - ради отправки текста GrqphQL-схемы
; клиенту по IntrospectionQuery при загрузке/обновлении страницы
; во всех остальных случаях запросов работает наша функция execute-core
(defn execute [query variables operation-name]
  (if (.contains query "IntrospectionQuery")
    (json/generate-string schema/starter-schema)
    (execute-core query variables operation-name) ))
