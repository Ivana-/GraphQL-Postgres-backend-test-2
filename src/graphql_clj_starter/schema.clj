(ns graphql-clj-starter.schema)


(defn m-t [field args & nodes] {:fieldName field, :args args, :nodes nodes})

(def q-users
  (m-t :users {}
       ;(m-t :id {})
       (m-t :name {})
       (m-t :posts {}
            ;(m-t :id {})
            (m-t :title {})
            (m-t :comments {}
                 ;(m-t :id {})
                 (m-t :text {})
                 (m-t :author {}
                      (m-t :name {})
                      )))
       (m-t :comments {}
            ;(m-t :id {})
            (m-t :text {})
            (m-t :author {}
                 (m-t :name {})
                 ))))
(def q-user
  (m-t :user {:id 2}
       (m-t :id {})
       (m-t :name {})
       (m-t :posts {}
            (m-t :id {})
            (m-t :title {})
            (m-t :comments {}
                 ;(m-t :id {})
                 (m-t :text {})
                 ))))
(def q-posts
  (m-t :posts {}
       ;(m-t :id {})
       (m-t :title {})
       ))
(def q-post
  (m-t :post {:id 1}
       (m-t :id {})
       (m-t :title {})
       (m-t :comments {}
            ;(m-t :id {})
            (m-t :text {})
            (m-t :author {}
                 (m-t :name {})
                 )
            )))

(def q (m-t :query {}
                  q-users
                  q-posts
                  ;q-user
                  ;q-post
                  ))
;(identity q)


"
(select
     			json_agg(json_build_object(
        			'id', cs.comm_id,
        			'text', cs.comm_text
                ))
     		 from comms as cs
     		 where cs.post_id = ps.post_id)
"
(def left-indent "    ")
(def new-line-sym \newline)

(defn fieldName-or-alias [node]
  (let [alias (:alias node)]
    (if (nil? alias) (:fieldName node) alias)))

(defn get-type [type-field]
  (let [type (:type type-field)]
    (if (string? type) @(resolve (symbol type)) type)))

(defn sql-query--- [node type-field parent-type-field d]
  (let [type-zzz (get-type type-field)
        sql-table (:sqlTable type-zzz)
        sql-table-alias (:sqlTableAlias type-zzz)
        sql-parent-table-alias
          (:sqlTableAlias (get-type parent-type-field))
        sql-where (:sqlWhere type-field)
        is-list (:is-list type-field)

        children-fields (:fields type-zzz)
        args-fact (:args node)]

    ;(println (:name type-zzz))
    ;(println children-fields)

    (if (nil? (:nodes node))
      ; это лист
      (str sql-parent-table-alias
           "." (:sqlColumn type-field))

      ; это объект
      (str "(select "
           (if is-list "json_agg(" "(")
           "json_build_object(" new-line-sym

           ; select section
           (clojure.string/join
             (str "," new-line-sym)
             (map #(str
                     (str "    " d)
                     "'" (name (fieldName-or-alias %)) "', "
                     (sql-query--- %
                                ((:fieldName %) children-fields)
                                type-field
                                (str "    " d)))
                  (:nodes node)))

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
           ))))

(defn get-errors [node type-field parent-type-field]
  (if (nil? type-field)
    (list (str "field "
              (name (:fieldName node))
               " does not exist on "
               (:name (get-type parent-type-field)))
          )
    nil))

; будем возвращать пару в мапе - текст запроса и список ошибок при его построении

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

      ; это лист
      (nil? (:nodes node))
      {:sql-text (str sql-parent-table-alias
                      "." (:sqlColumn type-field)) :errors nil}

      ; это объект
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



(def GraphQLInt {:name "Int"})
(def GraphQLFloat {:name "Float"})
(def GraphQLString {:name "String"})
(def GraphQLBoolean {:name "Boolean"})

(declare User)

;(def Comment-initial
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
    {:type  "graphql-clj-starter.schema/User", ; потом @(resolve (symbol "User")) даст тип :)
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
    :immortal
    {:type GraphQLBoolean,
     :args {:id GraphQLString, :test GraphQLInt},
     :resolve 33},
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

;(defn upd-kv [m k f] (assoc m k (f (k m))))

;(def Comment
;  (upd-kv Comment-initial :fields
;          #(upd-kv % :author
;    (fn [x] {:type User,
;             :sqlWhere (fn [args] "jc.contact_id = cs.contact_id")}))
;                         ))

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


(def z "
# Root Query
type Query {
  # return hero from a particular episode
  hero(episode: Episode): Character
  human(id: String!): Human
  droid(id: String!): Droid
  hello(world: WorldInput): String
  objectList: [Object!]!
  nestedInput(nested: NestedInput): String

  diap(l: Int!, r: Int!): [Human] #Diap
  inDate(x: NestedInput): Int
}
")



(defn get-args-str [args]
  (if (nil? args)
    ""
    (str "("
         (clojure.string/join ", "
            (map #(str (name (first %)) ": " (:name (second %))) args))
       ")")))

(defn get-whole-type [type-field]
  (if (:is-list type-field)
    (str "[" (:name (get-type type-field)) "]")
    (:name (get-type type-field))))

(defn str-gql-field [kv]
  (let [v (second kv)
        args-str (get-args-str (:args v))]
    (str left-indent (name (first kv)) args-str ": "
         (get-whole-type v)
         )))


(defn concat-lines [fs] (clojure.string/join new-line-sym fs))

(defn str-gql-schema [t]
  (str new-line-sym
       (if (nil? (:comment t)) "" (str "# " (:comment t) new-line-sym))
       "type " (:name t) " {" new-line-sym
       (concat-lines (map str-gql-field (:fields t)))
       new-line-sym "}"))

(defn make-gql-schema [& types]
  (concat-lines (map str-gql-schema types)))



(defn test-query[q] (sql-query q {:type Query} nil ""))
;(println (test-query q))

(def schema-footer
"
type Mutation {
  # create human for given name, it accepts list of friends as variable
  # createHuman(name: String, friends: [String]): Human
}

schema {
  query: Query
  mutation: Mutation
}
")
(def starter-schema
  (concat-lines [(make-gql-schema Comment Post User Query), schema-footer]))
;(println starter-schema)

; запрос схемы с клиента
"
  query IntrospectionQuery {
    __schema {
      queryType { name }
      mutationType { name }
      types {
        ...FullType
      }
      directives {
        name
        description
        locations
        args {
          ...InputValue
        }
      }
    }
  }
"
;(declare b)
;(def a {:name "A" :ref #'b})
;(def b {:name "B" :ref #'a})
;(defn show [x] (str "name " (:name x) ", ref " (:name @(:ref x))))
;(println (show a))
;(println (show b))

;(def Z 33)
(def zzz "
query q {
  user(id: 1) {
    name
    comments {
      text
      author {
        name
        comments {
          text
          #author {
          #  name
            #comments {
            #  text
            #}
          #}
        }
        posts {
          title
        }
      }
    }
  }
}
")
