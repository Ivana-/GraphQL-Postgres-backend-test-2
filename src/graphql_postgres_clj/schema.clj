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
   :uniqueKey "comm_id",
   :fields
   {:id
    {:type      GraphQLInt,
     :sqlColumn "comm_id"},
    :text
    {:type      GraphQLString,
     :sqlColumn "comm_text"},
    :author
    {:type  "graphql-postgres-clj.schema/User", ; потом @(resolve (symbol "User")) даст тип :)
     :sqlWhere (fn [args] "jc.contact_id = cs.contact_id")},
    }})

(def Post
  {:name "Post"
   :comment "Posts in our system"
   :sqlTable "posts",
   :sqlTableAlias "ps",
   :uniqueKey "post_id",
   :fields
   {:id
    {:type      GraphQLInt,
     :sqlColumn "post_id"},
    :title
    {:type      GraphQLString,
     :sqlColumn "title"},
    :comments
    {:type Comment,
     :is-list true,
     :sqlWhere (fn [args] "ps.post_id = cs.post_id")},
    }})

; TODO
; UNION types
;(def PostComment
;  {:name "PostComment"
;   :comment "Posts and comments in our system"
;   :union [Post, PostComment],
;;   :sqlTableAlias "ps",
;   })

(def User
  {:name "User"
   :comment "Users of our system"
   :sqlTable "jc_contact",
   :sqlTableAlias "jc",
   :uniqueKey "contact_id",
   :fields
   {:id
    {:type GraphQLInt,
     :sqlColumn "contact_id"},
    :name
    {:type GraphQLString,
     :sqlColumn "first_name"},
    ;:immortal
    ;{:type GraphQLBoolean,
    ; :args {:id GraphQLString, :test GraphQLInt},
    ; :resolve 33},
    :posts
    {:type Post,
     :is-list true,
     :sqlWhere (fn [args] "jc.contact_id = ps.contact_id")},
    :comments
    {:type Comment,
     :is-list true,
     :sqlWhere (fn [args] "jc.contact_id = cs.contact_id")},

    ;:posts_comments
    ;{:type PostComment,
    ; :is-list true,
    ; :sqlWhere (fn [args] "jc.contact_id = cs.contact_id")},
    }})

; основной тип-запрос graphql - что в принципе можно запрашивать на корневом уровне
(def Query
  {:name "Query"
   :comment "Root Query"
   :fields
   {:user {:type User,
           :args {:id GraphQLInt},
           :sqlWhere (fn [args] (str "jc.contact_id = "
                                     (:id args)))},
    :users {:type User, :is-list true},
    :post {:type Post,
           :args {:id GraphQLInt},
           :sqlWhere (fn [args] (str "ps.post_id = "
                                     (:id args)))},
    :posts {:type Post, :is-list true},
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

; получение полной строки - описания типа в схеме graphql:
; # Root Query
; type Query {
;     user(id: Int): User
;     users: [User]
;     post(id: Int): Post
;     posts: [Post]
; }
(defn str-graphql-schema [t]
  (str new-line-sym
       (if (nil? (:comment t)) "" (str "# " (:comment t) new-line-sym))
       "type " (:name t) " {" new-line-sym
       (concat-lines (map str-graphql-field (:fields t)))
       new-line-sym "}"))

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
  (concat-lines [(make-gql-schema Comment Post User Query), schema-footer]))


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

; функция-ядро, рекурсивно обходит ast graphql-запроса параллельно с типами
; задающими схему данных, и возвращает пару в мапе - сам текст sql-запроса
; и список ошибок при его построении
(defn sql-query [node type-field parent-type-field d]

  (let [type-zzz (get-type type-field)
        sql-table (:sqlTable type-zzz)
        sql-table-alias (:sqlTableAlias type-zzz)
        sql-parent-table-alias (:sqlTableAlias (get-type parent-type-field))
        sql-where (:sqlWhere type-field)
        is-list (:is-list type-field)
        children-fields (:fields type-zzz)
        args-fact (:args node)

        errors (get-errors node type-field parent-type-field)
        ]

    (cond
      ; это ошибка
      (not (nil? errors))
      {:sql-text "", :errors errors}

      ; это примитивный тип - лист
      (nil? (:nodes node))
      {:sql-text (str sql-parent-table-alias
                      "." (:sqlColumn type-field)) :errors nil}

      ; это составной тип - объект
      :else
      (let [sub-querys (map #(sql-query
                         %
                         ((:fieldName %) children-fields)
                         type-field
                         (str "    " d)) (:nodes node))

            names-fields (map #(str
                          (str "    " d)
                          "'" (name (fieldName-or-alias %2)) "', "
                          (:sql-text %1)) sub-querys (:nodes node))

            errors (apply concat (map :errors sub-querys))

            sql-text
            (str "(select "
                 (if is-list "json_agg(" "(")
                 "json_build_object(" new-line-sym

                 ; select section
                 (clojure.string/join (str "," new-line-sym) names-fields)

                 new-line-sym d ")) "

                 ; from section
                 (if (nil? sql-table)
                   "as data" (str "from " sql-table
                                  " as " sql-table-alias))

                 new-line-sym d

                 ; where section
                 (if (nil? sql-where)
                   "" (str "where " (sql-where args-fact)))
                 ")"
                 )
            ]

        {:sql-text sql-text, :errors errors})
        )))

; основная экспортируемая функция модуля - по переданному ast graphql-запроса
; получает текст sql-запроса и список ошибок при его построении
(defn ast-to-sql-text-and-errors[ast] (sql-query ast {:type Query} nil ""))
