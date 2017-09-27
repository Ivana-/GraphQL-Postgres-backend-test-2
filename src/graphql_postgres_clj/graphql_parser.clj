(ns graphql-postgres-clj.graphql-parser
  (:require [clj-antlr.core :as antlr]))

; модуль экспортирует функцию graphql-text-to-ast [graphql-text]
; которая преобразует текст graphql-запроса в стандартизованное ast

; antlr-parser по заданной в файле грамматике
(def antlr-parser (antlr/parser "public/GraphQL.g4"))

; волшебная полиаргументная identity - может есть готовая, но написал свою :)
(defn my-identity
  ([x] x)
  ([x & args] (cons x args)))

; преобразует ключевые слова, возвращаемые парсером antlr, в функции
(defn keyword-to-function [key]
  (case key
    ;:document
    ;:definition

    :operationDefinition merge
    :operationType (fn [x] {:operationType x})
    :operationName (fn [x] {:operationName x})

    :alias (fn [x y z] {:fieldName (keyword z), :alias (keyword x)})
    :fieldName (fn [x] (if (map? x) x {:fieldName (keyword x)}))

    :field merge

    :typeName (fn [x] {:typeName x})

    ;(:inlineFragment
    ;  "..."
    ;  "on"
    ;  (:typeCondition (:typeName "Droid"))
    ;  (:selectionSet "{" (:selection (:field (:fieldName "primaryFunction"))) "}"))
    :inlineFragment (fn [_ _ x y] {:onType (:typeName x), :nodes (:nodes y)})

    :directives (fn [& args] {:directives (filter map? args)})
    :selectionSet (fn [& args] {:nodes (filter map? args)})

    :variableDefinitions (fn [& args] {:vars (filter map? args)})

    :variableDefinition (fn ([x y z] {x "???"})
                          ([x y z d] {x "???"}))

    :arguments (fn [& args] {:args (apply merge (filter map? args))})
    ;(:argument "unit" ":" (:valueOrVariable (:value "FOOT")))
    ;(:argument "episode" ":" (:valueOrVariable (:variable "$" "ep")))
    :argument (fn [x _ y] {(keyword x) y})
    :value (fn [x] x)
    :variable (fn [x y] y)

    my-identity
    ))

; волшебный глубокий функтор - может есть готовый, но написал свой :)
(defn map-deep [f l] (if (coll? l) (map #(map-deep f %) l) (f l)))

; хэлпер - преобразует ключевое слово в функцию, а остальное оставляет как есть
; ядро глубокого функтора map-deep
(defn map-core [x] (if (keyword? x) (keyword-to-function x) x))

; ключевая экспортируемая функция модуля -
; преобразует текст graphql-запроса в стандартизованное ast
; сначала из текста graphql получаем глубокое дерево с помощью antlr,
; потом заряжаем головные формы - ключевые слова его списков
; функциями через map-deep и взрываем результат эвалом :)
; почти как Визитор, но функторный, без истории посещений
; гомоиконность рулит! :)
(defn graphql-text-to-ast [graphql-text]
  (eval (map-deep map-core (antlr-parser graphql-text))))
