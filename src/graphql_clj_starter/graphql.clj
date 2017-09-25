(ns graphql-clj-starter.graphql
  (:require [graphql-clj.resolver :as resolver]
            [graphql-clj.executor :as executor]
            [graphql-clj.query-validator :as qv]
            [graphql-clj.schema-validator :as sv]
            [clojure.core.match :as match]

            [graphql-clj-starter.schema :as schema]
            [graphql-clj-starter.test-postgres :as test-postgres]
            ))


(def starter-schema schema/starter-schema)

(def starter-schema- "
# enum for episode
  enum Episode { NEWHOPE, EMPIRE, JEDI }

# interface for Chapter 
interface Character {
  id: String!
  name: String
  friends: [Character]
  appearsIn: [Episode]
}

# human implements character
type Human implements Character {
  id: String!
  name: String
  # friends of human
  friends: [Character]
  appearsIn: [Episode]
  homePlanet: String
}

type Droid implements Character {
  id: String!
  name: String
  friends: [Character]
  appearsIn: [Episode]
  primaryFunction: String
}

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

type Object {
  id: String!
}

input WorldInput {
  text: String
}

input NestedInput {
  value: WorldInput
}

type Mutation {
  # create human for given name, it accepts list of friends as variable
  createHuman(name: String, friends: [String]): Human
}

schema {
  query: Query
  mutation: Mutation
}

# ------------------------------------

type Diap {
  interval: [Int]
}

scalar Date

")

(def luke {:id "1000",
           :name "Luke Skywalker"
           :friends ["1002" "1003" "2000" "2001"]
           :appearsIn [ 'NEWHOPE 'EMPIRE 'JEDI ],
           :homePlanet "Tatooine"})

(def vader {:id "1001",
            :name "Darth Vader"
            :friends [ "1004"]
            :appearsIn [ 'NEWHOPE 'EMPIRE]
            :homePlanet "Tatooine"})

(def han {
          :id "1002",
          :name "Han Solo",
          :friends [ "1000", "1003", "2001" ],
          :appearsIn [ 'NEWHOPE 'EMPIRE 'JEDI ],})


(def leia {
           :id "1003",
           :name "Leia Organa",
           :friends [ "1000", "1002", "2000", "2001" ],
           :appearsIn [ 'NEWHOPE 'EMPIRE ],
           :homePlanet "Alderaan",})


(def tarkin {
             :id "1004",
             :name "Wilhuff Tarkin",
             :friends [ "1001" ],
             :appearsIn [ 'JEDI ],})


(def humanData  (atom {
                       "1000" luke
                       "1001" vader
                       "1002" han
                       "1003" leia
                       "1004" tarkin}))

(def threepio {
               :id "2000",
               :name "C-3PO",
               :friends [ "1000", "1002", "1003", "2001" ],
               :appearsIn [ 'NEWHOPE 'EMPIRE 'JEDI ],
               :primaryFunction "Protocol",})


(def artoo {
            :id "2001",
            :name "R2-D2",
            :friends [ "1000", "1002", "1003" ],
            :appearsIn [ 'NEWHOPE ],
            :primaryFunction "Astromech",})


(def droidData (atom {"2000" threepio
                      "2001" artoo}))

(defn get-human [id]
  (get @humanData (str id))) ; BUG: String should be parsed as string instead of int

(defn get-droid [id]
  (get @droidData (str id))) ; BUG: String should be parsed as string instead of int

(defn get-character [id]
  (or (get-human id) ; BUG: String should be parsed as string instead of int
      (get-droid id)))

(defn get-friends [character]
  (map get-character (:friends character)))

;; java.lang.String.equals
(defn get-hero [episode]

  (println "episode: " episode (class episode))

  ;;(match/match
  ;;  [episode]
  ;;  ['NEWHOPE] luke
  ;;  :else artoo)
  (if (= episode 'NEWHOPE)
    luke
    artoo))


(def human-id (atom 2050))

(defn create-human [args]
  (let [new-human-id (str (swap! human-id inc))
        new-human {:id new-human-id
                   :name (get args "name")
                   :friends (get args "friends")}]
    (swap! humanData assoc new-human-id new-human)
    new-human))



;; (defn get-diap [l r] {:interval (range l (+ 1 r))})
(defn get-diap [l r] (map get-human (range l (+ 1 r))))

(defn tst-foo [context parent x]
  (println
    ;; "context: " context
    ;; "parent: " parent
    "argument: " x (class x))
  33)


(defn starter-resolver-fn [type-name field-name]
  (match/match
   [type-name field-name]
   ["Query" "hero"] (fn [context parent args]
                      ;; (get-hero (:episode args)))
                      (get-hero (get args "episode")))
   ["Query" "human"] (fn [context parent args]
                       (get-human (str (get args "id"))))
   ["Query" "droid"] (fn [context parent args]
                       (get-droid (str (get args "id"))))
   ["Query" "objectList"] (fn [context parent args]
                            (repeat 3 {:id (java.util.UUID/randomUUID)}))
   ;; Hacky!!! Should use resolver for interface
   ["Human" "friends"] (fn [context parent args]
                         (get-friends parent))
   ["Droid" "friends"] (fn [context parent args]
                         (get-friends parent))
   ["Character" "friends"] (fn [context parent args]
                             (get-friends parent))
   ["Mutation" "createHuman"] (fn [context parent args]
                                (create-human args))
   ["Query" "hello"] (fn [context parent args]
                       (let [world (get args "world")]
                         world))

   ["Query" "diap"] (fn [context parent args]
                      (get-diap (get args "l") (get args "r")))

   ["Query" "inDate"] (fn [context parent args]
                        (tst-foo context parent (get args "x")))

   :else nil))

(def validated-schema (sv/validate-schema starter-schema))

(defn execute
  [query variables operation-name]
  ;(println "queryyyyyy " query)
  (if (.contains query "IntrospectionQuery")
    (executor/execute nil validated-schema starter-resolver-fn query variables operation-name)

    (test-postgres/execute query variables operation-name)
  ))

