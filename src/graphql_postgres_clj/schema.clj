(ns graphql-postgres-clj.schema)

; модуль содержит определения типов graphql с указанием полей, связей и параметров
; запроса к базе postgres для маппинга запрашиваемых в базе полей и структур данных
; по этому набору типов формируется и экспортируется текст базовой схемы graphql

; также экспортирует функцию ast-to-sql-text-and-errors[ast], которая по переданному
; ast (полученному из graphql-запроса) и перечисленному здесь набору типов строит
; текст sql-запроса, попутно выявляя список возникших при этом построении ошибок


;------------------------------------------------------------------------------------
; типы данных, формирующие схему, структуру маппинга полей и т.п.

; базовые типы graphql
(def GraphQLInt {:name "Int"})
(def GraphQLFloat {:name "Float"})
(def GraphQLString {:name "String"})
(def GraphQLBoolean {:name "Boolean"})

; пользовательские типы
;(declare User)

(def Comment
  {:name "Comment"
   :comment "Comments in our system"
   :sqlTable "comms",
   :sqlTableAlias "cs",
   :fields
   {:id {:type GraphQLInt, :sqlColumn "comm_id"},
    :text {:type GraphQLString, :sqlColumn "comm_text"},
    :author
    {:type  "graphql-postgres-clj.schema/User", ; потом @(resolve (symbol "User")) даст тип :)
     :sqlWhere (fn [args] "jc.contact_id = cs.contact_id")},
    }})

(def Post
  {:name "Post"
   :comment "Posts in our system"
   :sqlTable "posts",
   :sqlTableAlias "ps",
   :fields
   {:id {:type GraphQLInt, :sqlColumn "post_id"},
    :title {:type GraphQLString, :sqlColumn "title"},
    :comments
    {:type Comment,
     :is-list true,
     :sqlWhere (fn [args] "ps.post_id = cs.post_id")},
    }})

; UNION type
(def Message
  {:name "Message"
   :comment "Posts and comments in our system"
   :union (list Post Comment),
   })

(def User
  {:name "User"
   :comment "Users of our system"
   :sqlTable "jc_contact",
   :sqlTableAlias "jc",
   :fields
   {:id {:type GraphQLInt, :sqlColumn "contact_id"},
    :name {:type GraphQLString, :sqlColumn "first_name"},
    :posts
    {:type Post,
     :is-list true,
     :sqlWhere (fn [args] "jc.contact_id = ps.contact_id")},
    :comments
    {:type Comment,
     :is-list true,
     :sqlWhere (fn [args] "jc.contact_id = cs.contact_id")},
    :messages
    {:type Message,
     :is-list true,
     :sqlWhere {"Post" (fn [args] "jc.contact_id = ps.contact_id"),
                "Comment" (fn [args] "jc.contact_id = cs.contact_id")}},
    }})

; основной тип-запрос graphql - что в принципе можно запрашивать на корневом уровне
(def Query
  {:name "Query"
   :comment "Root Query"
   :fields
   {:user {:type User,
           :args {:id GraphQLInt},
           :sqlWhere (fn [args] (str "jc.contact_id = " (:id args)))},
    :users {:type User, :is-list true},
    :post {:type Post,
           :args {:id GraphQLInt},
           :sqlWhere (fn [args] (str "ps.post_id = " (:id args)))},
    :posts {:type Post, :is-list true},
    :messages {:type Message, :is-list true},
    }})


;------------------------------------------------------------------------------------
; блок общих утилитарных констант и функций:

; отступ слева для красивого форматирования текста sql-запроса (необязателен)
(def left-indent "    ")

; алиас для перевода строки - на всякий, вдруг придется заменить из-за платформозависимости :)
(def new-line-sym \newline)

; возвращает сам тип из мапы-параметров текущего поля
; нужно для возможности задания взаимных ссыллок при формировании циклического графа типов
(defn get-type [type-field]
  (let [type (:type type-field)]
    (if (string? type) @(resolve (symbol type)) type)))


;------------------------------------------------------------------------------------
; блок функций получения текста схемы graphql:

; получение строки аргументов из списка: "(id: Int, name: String, author: User)"
(defn get-args-str [args]
  (if (nil? args)
    ""
    (str "("
         (clojure.string/join
           ", "
           (map #(str (name (first %)) ": " (:name (second %))) args))
         ")")))

; получение строки типа-списка или объекта: "[User]" или "User"
(defn get-whole-type [type-field]
  (if (:is-list type-field)
    (str "[" (:name (get-type type-field)) "]")
    (:name (get-type type-field))))

; получение строки поля типа: "comment(id: Int, name: String, author: User): Comment"
(defn str-graphql-field [kv]
  (let [v (second kv)
        args-str (get-args-str (:args v))]
    (str left-indent (name (first kv)) args-str ": " (get-whole-type v) )))

; склейка списка строк через перевод строки
(defn concat-lines [strings] (clojure.string/join new-line-sym strings))

; получение строки типа-объдинения "union PostComment = Post | Comment"
(defn str-graphql-union-type [t]
  (str new-line-sym
       (if (nil? (:comment t)) "" (str "# " (:comment t) new-line-sym))
       "union " (:name t) " = "
       (clojure.string/join " | " (map #(:name %) (:union t))) ))

; получение строки типа-объекта в схеме graphql:
; # Root Query
; type Query {
;     user(id: Int): User
;     users: [User]
;     post(id: Int): Post
;     posts: [Post]
; }
(defn str-graphql-object-type [t]
  (str new-line-sym
       (if (nil? (:comment t)) "" (str "# " (:comment t) new-line-sym))
       "type " (:name t) " {" new-line-sym
       (concat-lines (map str-graphql-field (:fields t)))
       new-line-sym "}"))

; получение строки типа общего вида в схеме graphql:
(defn str-graphql-schema [t]
  (if (nil? (:union t))
    (str-graphql-object-type t)
    (str-graphql-union-type t) ))

; получение полного текста схемы переданного списка типов
(defn make-gql-schema [& types]
  (concat-lines (map str-graphql-schema types)))

; служебные типы схемы
(def schema-footer
"
type Mutation {
  # TODO
}

schema {
  query: Query
  mutation: Mutation
}
")

; финал - определение экспортируемой строковой константы
; - текста gpaphql-схемы, составленной по заданным в начале модуля типам
(def starter-schema
  (concat-lines [
"# GraphQL Schema of this demo example
# (press F5 to see it again)",
                 (make-gql-schema
                   Comment
                   Post
                   Message
                   User
                   Query), schema-footer]))


;------------------------------------------------------------------------------------
; блок функций получения текста sql-запроса

; возвращает имя или псевдоним поля текущего узла ast graphql-запроса
(defn fieldName-or-alias [node]
  (let [alias (:alias node)]
    (if (nil? alias) (:fieldName node) alias)))

; возвращает список найденных ошибок при сопоставлении текущего узла ast graphql-запроса
; и параметров соответствующих текущего и родительского типов
(defn get-errors [node type-field parent-type-field]
  (if (nil? type-field)
    (list (str "field "
              (name (:fieldName node))
               " does not exist on "
               (:name (get-type parent-type-field)))
          )
    nil))

(declare sql-query)


; функция строит блок union как значение поля запроса - для типов UNION и INTERFACE
(defn make-union [node type-field parent-type-field d]

  (let [type (get-type type-field)
        sql-where (:sqlWhere type-field)
        ;is-list (:is-list type-field)

        ; строим новый список type-field для подзапросов блока union
        union-type-fields
          (map (fn [t]
                 ; передаем условие для конкретного типа t
                 {:type t, :sqlWhere (if (map? sql-where) (sql-where (:name t)) nil)}
                 ) (:union type))

        d-new (str left-indent d)

        ; подзапросы блока union
        sub-querys (map (fn [t-f] (sql-query node t-f parent-type-field d-new)
                          ) union-type-fields)

        ; отделим их тексты и ошибки для дальнейшей обработки
        sub-querys-texts (map :sql-text sub-querys)
        sub-querys-errors (apply concat (map :errors sub-querys))

        ; тексты свернем в блок union
        sql-text
          (str
            "(select json_agg(json_build_object) from ("
            new-line-sym d-new
            (clojure.string/join
              (str new-line-sym
                   d-new "union all"
                   new-line-sym d-new)
              sub-querys-texts)
            new-line-sym d-new ") as " (name (:fieldName node)) ")"
            )
        ]
    ; возвращаем пару - текст запроса и накопленный список ошибок
    {:sql-text sql-text, :errors sub-querys-errors}
    ))


; утилитарная проверка равенства 2 строк по значению
(defn eq-strings? [s1 s2] (and (string? s1) (string? s2) (.equals s1 s2)))


; получаем линейный список подчиненных нод для переданного типа
; он может быть разным для текущей ноды и разных типов - из-за фрагментов
(defn get-nodes-on-type [node type]
   (let [sub-nodes (:nodes node)
         all-type-nodes (filter #(not (nil? (:fieldName %))) sub-nodes)
         fragments-on-type (filter #(eq-strings? (:onType %) (:name type)) sub-nodes)]
      ;(println (str (:name type) " - " (clojure.string/join " " (map #(:fieldName %) r))) )
      (concat all-type-nodes (apply concat (map #(:nodes %) fragments-on-type))) ))


; функция-ядро, рекурсивно обходит ast graphql-запроса параллельно с типами
; задающими схему данных, и возвращает пару в мапе - сам текст sql-запроса
; и список ошибок при его построении
(defn sql-query [node type-field parent-type-field d]

  (let [type (get-type type-field)
        sql-table (:sqlTable type)
        sql-table-alias (:sqlTableAlias type)
        sql-parent-table-alias (:sqlTableAlias (get-type parent-type-field))
        sql-where (:sqlWhere type-field)
        is-list (:is-list type-field)
        children-fields (:fields type)
        args-fact (:args node)
        nodes-on-type (get-nodes-on-type node type)
        errors (get-errors node type-field parent-type-field)
        ]

    (cond
      ; это ошибка
      (not (nil? errors)) {:sql-text "", :errors errors}

      ; это примитивный тип - лист
      (nil? (:nodes node))
        {:sql-text (str sql-parent-table-alias
                        "." (:sqlColumn type-field)) :errors nil}

      ; это составной тип - объект
      :else
      (if (not (nil? (:union type)))

        ; если тип-объединение, то формируем текст запроса отдельной функцией
        (make-union node type-field parent-type-field d)

        ; иначе рекурсивно вызываем sql-query на подчиненных полях
        ; данного типа и склеиваем результаты
        (let [; подзапросы блока select
              sub-querys (map #(sql-query
                                 %
                                 ((:fieldName %) children-fields)
                                 type-field
                                 (str "    " d)) nodes-on-type)

              ; отделим их тексты и ошибки для дальнейшей обработки
              sub-querys-texts (map :sql-text sub-querys)
              sub-querys-errors (apply concat (map :errors sub-querys))

              ; список строк блока select
              names-fields (map #(str
                                   (str "    " d)
                                   "'" (name (fieldName-or-alias %2)) "', "
                                   %1) sub-querys-texts nodes-on-type)

              ; свернем их в блок select
              sql-text
              (str "(select "
                   (if is-list "json_agg(" "(")
                   "json_build_object(" new-line-sym

                   ; select section
                   (clojure.string/join (str "," new-line-sym) names-fields)

                   new-line-sym d ")) "

                   ; from section
                   (if (nil? sql-table)
                     "as data" (str "from " sql-table " as " sql-table-alias))

                   new-line-sym d

                   ; where section
                   (if (nil? sql-where) "" (str "where " (sql-where args-fact)))
                   ")"
                   )
              ]
          ; возвращаем пару - текст запроса и накопленный список ошибок
          {:sql-text sql-text, :errors sub-querys-errors})
        ))))

; основная экспортируемая функция модуля - по переданному ast graphql-запроса
; получает текст sql-запроса и список ошибок при его построении
(defn ast-to-sql-text-and-errors[ast] (sql-query ast {:type Query} nil ""))
