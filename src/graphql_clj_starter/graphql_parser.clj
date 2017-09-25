(ns graphql-clj-starter.graphql-parser
  (:require [clj-antlr.core :as antlr]
            ;[clojure.core.match :as match]
            [graphql-clj-starter.schema :as schema]
            ))

(def aaa (antlr/parser "grammar Aaa;
                               aaa : AA+;
                               AA : [Aa]+ ;
                               WS : ' ' -> channel(HIDDEN) ;"))
;(println (aaa "aAAaa A aAA AAAAaAA"))

;(def json (antlr/parser "grammars/Json.g4"))
;(println (json "[1,2,3]"))

(def graphql (antlr/parser "public/GraphQL.g4"))

(def s1 "
{
  hero {
    name
  }
}")
(def s2 "
{
  hero {
    name
    # Queries can have comments!
    friends {
      name
    }
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "{\n  hero {\n    name\n    # Queries can have comments!\n    friends {\n      name\n    }\n  }\n}"
;"zzzzzzzzzzzzz variables:" nil
;"zzzzzzzzzzzzz (json/parse-string variables):" nil
;"zzzzzzzzzzzzz operationName:" nil

(def s3 "
{
  human(id: \"1000\") {
    name
    height
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "{\n  human(id: \"1000\") {\n    name\n    height\n  }\n}"
;"zzzzzzzzzzzzz variables:" nil
;"zzzzzzzzzzzzz (json/parse-string variables):" nil
;"zzzzzzzzzzzzz operationName:" nil

(def s4 "
{
  human(id: \"1000\") {
    name
    height(unit: FOOT)
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "{\n  human(id: \"1000\") {\n    name\n    height(unit: FOOT)\n  }\n}"
;"zzzzzzzzzzzzz variables:" nil
;"zzzzzzzzzzzzz (json/parse-string variables):" nil
;"zzzzzzzzzzzzz operationName:" nil

(def s5 "
{
  empireHero: hero(episode: EMPIRE) {
    name
  }
  jediHero: hero(episode: JEDI) {
    name
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "{\n  empireHero: hero(episode: EMPIRE) {\n    name\n  }\n  jediHero: hero(episode: JEDI) {\n    name\n  }\n}"
;"zzzzzzzzzzzzz variables:" nil
;"zzzzzzzzzzzzz (json/parse-string variables):" nil
;"zzzzzzzzzzzzz operationName:" nil

(def s6 "
{
  leftComparison: hero(episode: EMPIRE) {
    ...comparisonFields
  }
  rightComparison: hero(episode: JEDI) {
    ...comparisonFields
  }
}

fragment comparisonFields on Character {
  name
  appearsIn
  friends {
    name
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "{\n  leftComparison: hero(episode: EMPIRE) {\n    ...comparisonFields\n  }\n  rightComparison: hero(episode: JEDI) {\n    ...comparisonFields\n  }\n}\n\nfragment comparisonFields on Character {\n  name\n  appearsIn\n  friends {\n    name\n  }\n }"
;"zzzzzzzzzzzzz variables:" nil
;"zzzzzzzzzzzzz (json/parse-string variables):" nil

(def s7 "
query HeroNameAndFriends($episode: Episode) {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "query HeroNameAndFriends($episode: Episode) {\n  hero(episode: $episode) {\n    name\n    friends {\n      name\n    }\n  }\n}"
;"zzzzzzzzzzzzz variables:" {:episode "JEDI"}
;"zzzzzzzzzzzzz operationName:" "HeroNameAndFriends"

(def s8 "
query HeroNameAndFriends($episode: Episode = \"JEDI\") {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "query HeroNameAndFriends($episode: Episode = \"JEDI\") {\n  hero(episode: $episode) {\n    name\n    friends {\n      name\n    }\n  }\n}"
;"zzzzzzzzzzzzz variables:" nil
;"zzzzzzzzzzzzz operationName:" "HeroNameAndFriends"

(def s9 "
query Hero($episode: Episode, $withFriends: Boolean!) {
  hero(episode: $episode) {
    name
    friends @include(if: $withFriends) {
      name
    }
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "query Hero($episode: Episode, $withFriends: Boolean!) {\n  hero(episode: $episode) {\n    name\n    friends @include(if: $withFriends) {\n      name\n    }\n  }\n}"
;"zzzzzzzzzzzzz variables:" {:episode "JEDI", :withFriends false}
;"zzzzzzzzzzzzz operationName:" "Hero"

(def s10 "
mutation CreateReviewForEpisode($ep: Episode!, $review: ReviewInput!) {
  createReview(episode: $ep, review: $review) {
    stars
    commentary
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "mutation CreateReviewForEpisode($ep: Episode!, $review: ReviewInput!) {\n  createReview(episode: $ep, review: $review) {\n    stars\n    commentary\n  }\n}"
;"zzzzzzzzzzzzz variables:" {:ep "JEDI", :review {:stars 5, :commentary "This is a great movie!"}}
;"zzzzzzzzzzzzz operationName:" "CreateReviewForEpisode"

(def s11 "
query HeroForEpisode($ep: Episode!) {
  hero(episode: $ep) {
    name
    ... on Droid {
      primaryFunction
    }
    ... on Human {
      height
    }
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "query HeroForEpisode($ep: Episode!) {\n  hero(episode: $ep) {\n    name\n    ... on Droid {\n      primaryFunction\n    }\n    ... on Human {\n      height\n    }\n  }\n}"
;"zzzzzzzzzzzzz variables:" {:ep "JEDI"}
;"zzzzzzzzzzzzz operationName:" "HeroForEpisode"

(def s12 "
{
  search(text: \"an\") {
    __typename
    ... on Human {
      name
    }
    ... on Droid {
      name
    }
    ... on Starship {
      name
    }
  }
}")
;"zzzzzzzzzzzzz schema:" nil
;"zzzzzzzzzzzzz query:" "{\n  search(text: \"an\") {\n    __typename\n    ... on Human {\n      name\n    }\n    ... on Droid {\n name\n    }\n    ... on Starship {\n      name\n    }\n  }\n}"
;"zzzzzzzzzzzzz variables:" nil
;"zzzzzzzzzzzzz operationName:" nil

(def s0 "
query TTT($ep: [Var!]!) {
  hero
}")


;(println (graphql s1))

(defn map-deep [f l] (if (coll? l) (map #(map-deep f %) l) (f l)))

(defn my-identity
  ([x] x)
  ([x & args] (cons x args)))


(defn cnv [key]
  (case key
    ;:document
    ;:definition

    :operationDefinition merge
    :operationType (fn [x] {:operationType x})
    :operationName (fn [x] {:operationName x})


    ;(:fieldName (:alias "leftComparison" ":" "hero"))
    ;(:fieldName "friends")
;    :alias (fn [x y z] {:fieldName z, :alias x})
;    :fieldName (fn [x] (if (map? x) x {:fieldName x}))
    :alias (fn [x y z] {:fieldName (keyword z), :alias (keyword x)})
    :fieldName (fn [x] (if (map? x) x {:fieldName (keyword x)}))

    ;field : fieldName arguments? directives? selectionSet?
    :field merge

    ;(:directives
    ;  (:directive
    ;    "@"
    ;    "include"
    ;    "("
    ;    (:argument "if" ":" (:valueOrVariable (:variable "$" "withFriends")))
    ;    ")"))

    ;directives : directive+
    ;directive : '@' NAME ':' valueOrVariable | '@' NAME | '@' NAME '(' argument ')'

    :directives (fn [& args] {:directives (filter map? args)})


    ;selection : field | fragmentSpread | inlineFragment

    ;selectionSet : '{' selection ( ','? selection )* '}'
    :selectionSet (fn [& args] {:nodes (filter map? args)})



    :variableDefinitions (fn [& args] {:vars (filter map? args)})

    ;(:variableDefinition (:variable "$" "ep") ":"
    ; (:type (:typeName "Episode") (:nonNullType "!")))
    ;(:variableDefinition
    ;  (:variable "$" "ep")
    ;  ":"
    ;  (:type (:listType "[" (:type (:typeName "Var") (:nonNullType "!")) "]") (:nonNullType "!")))

    ;(:variableDefinition
    ;  (:variable "$" "episode")
    ;  ":"
    ;  (:type (:typeName "Episode"))
    ;  (:defaultValue "=" (:value "\"JEDI\"")))

    ;variableDefinition : variable ':' type defaultValue?
    :variableDefinition (fn ([x y z] {x "???"})
                          ([x y z d] {x "???"}))

    :arguments (fn [& args] {:args (apply merge (filter map? args))})
    ;(:argument "unit" ":" (:valueOrVariable (:value "FOOT")))
    ;(:argument "episode" ":" (:valueOrVariable (:variable "$" "ep")))
    :argument (fn [x y z] {(keyword x) z}) ;#({%1 %3})
    :value (fn [x] x) ;#(str "= " %)
    :variable (fn [x y] y)

    my-identity
    ))

(defn tr [x] (if (keyword? x) (cnv x) x))

;(println (cnv (graphql s1)))
;(println (eval (map-deep tr (graphql s1))))

(defn my-test [s]
  (let [r (graphql s)]
    (println s)
    (println r)
    ;(println (map-deep tr r))
    ;(println (eval (map-deep tr r)))
    ))
;(my-test s1)
;(doseq [s [s1 s2 s3 s4 s5 s6 s7 s8 s9 s10 s11 s12]] (my-test s))
(doseq [s [s6 s11]] (my-test s))

;(my-test s1)
;(print schema/q)

(defn gql-text-to-ast [s]
  (let [r (graphql s)]
    ;(println s)
    ;(println r)
    ;(println (map-deep tr r))
    ;(map-deep tr r)
    (eval (map-deep tr r))
    ))

;(println (schema/test-query (gql-text-to-ast s1)))


(def ss1
"
query TTT($ep: [Var!]!) {
  user(id: 1) {
    name
  }
}")
(defn gql-text-to-sql[s] (schema/test-query (gql-text-to-ast s)))

;(println (gql-text-to-ast ss1))
;(println (gql-text-to-sql ss1))
