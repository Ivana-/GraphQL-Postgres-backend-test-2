(ns graphql-clj-starter.test-postgres
  (:require [clj-postgresql.core :as pg]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [graphql-clj-starter.schema :as schema]

            [graphql-clj-starter.graphql-parser :as graphql-parser]

            [cheshire.core :as json]
            ))


;------------------------- параметры подключения к базе данных postgres

; jdbc:postgresql://localhost:5432/contactdb
(def db-spec
  {:dbtype "postgresql"
   :host "localhost"
   :dbname "contactdb"
   :user "postgres"
   :password "postgress"})



(def sql-query-1 "
(select
	json_agg(json_build_object(
        'id', jc.contact_id,
        'name', jc.first_name,

        'comms',
			(select
     			json_agg(json_build_object(
        			'id', cs.comm_id,
        			'text', cs.comm_text
                ))
     		 from comms as cs
     		 where cs.contact_id = jc.contact_id),

        'posts',
(select
	json_agg(json_build_object(
        'id', ps.post_id,
        'title', ps.title,
        'comms',
			(select
     			json_agg(json_build_object(
        			'id', cs.comm_id,
        			'text', cs.comm_text,


        			'author',
                    (select
						            (json_build_object(
						                'name', concat (jc.first_name, ' ', jc.last_name),
        					          'id', jc.contact_id,

                                'children',
                                	(select
										json_agg(json_build_object(
        								--'id', ch.children_id,
        								'name', ch.first_name
                            			))
                     				from childrens as ch
                     				where jc.contact_id = ch.contact_id),


                            'posts',
(select
	json_agg(json_build_object(
        'id', ps.post_id,
        'title', ps.title,
        'comms',
			(select
     			json_agg(json_build_object(
        			'id', cs.comm_id,
        			'text', cs.comm_text
                ))
     		 from comms as cs
     		 where cs.post_id = ps.post_id)
     )) as data
from posts as ps
where ps.contact_id = jc.contact_id)

        					          ))
                        from jc_contact as jc
                        where jc.contact_id = cs.contact_id)

                ))
     		 from comms as cs
     		 where cs.post_id = ps.post_id)
     ))
from posts as ps
where ps.contact_id = jc.contact_id)

        )) as data
from jc_contact as jc)
")



(def sql-query-2 "
select
	(json_build_object(
        'id', ps.post_id,
        'title', ps.title,
        'comms',
			(select
     			json_agg(json_build_object(
        			'id', cs.comm_id,
        			'text', cs.comm_text
                ))
     		 from comms as cs
     		 where cs.post_id = ps.post_id)
     )) as data
from posts as ps
where ps.post_id = 1
")


(def sql-query-3 "
select
	json_agg(json_build_object(
        'id', ps.post_id,
        'title', ps.title,
        'comms',
			(select
     			json_agg(json_build_object(
        			'id', cs.comm_id,
        			'text', cs.comm_text,
               'author',
                    (select
						            (json_build_object(
						                'name', jc.first_name,
        					          'id', jc.contact_id
        					          ))
                        from jc_contact as jc
                        where jc.contact_id = cs.contact_id)
                ))
     		 from comms as cs
     		 where cs.post_id = ps.post_id)
     )) as data
from posts as ps
where (ps.post_id = 1 or true)
")


(def sql-query-4 "
select json_agg(json_build_object(
	  'id', jc.contact_id,
    'name', jc.first_name,
	  'chs',
        (select json_agg(json_build_object) from
          (

          select (json_build_object(
	            'id', cs.comm_id,
              'text', cs.comm_text))
          from comms as cs
          where jc.contact_id = cs.contact_id

          union all

          select (json_build_object(
	            'id', ps.post_id,
              'title', ps.title,


        'comms',
			(select
     			json_agg(json_build_object(
        			'id', cs.comm_id,
        			'text', cs.comm_text,
               'author',
                    (select
						            (json_build_object(
						                'name', jc.first_name,
        					          'id', jc.contact_id
        					          ))
                        from jc_contact as jc
                        where jc.contact_id = cs.contact_id)
                ))
     		 from comms as cs
     		 where cs.post_id = ps.post_id)






              ))
          from posts as ps
          where ps.contact_id = jc.contact_id

          union all

          select (json_build_object(
	            'id', cs.comm_id,
              'text', cs.comm_text))
          from comms as cs
          where jc.contact_id = cs.contact_id

          ) as zzz)
    ))
from jc_contact as jc
")




(defn execute [query variables operation-name]
  (let [sql-query
        (graphql-parser/gql-text-to-sql query)
        ;sql-query-4
        ;schema/test-query ;sql-query-1
        query-rezult (jdbc/query db-spec sql-query)
        fst (first query-rezult)]
    ;(doseq [r query-rezult]
    ;  (println ">" r))
    ;(println (class fst))
    ;(println fst)
    fst)
  )


(defn -main [& args]
  (if true
    (println schema/User))

  (let [query-rezult (jdbc/query db-spec sql-query-1)]
        ;query-rezult (jdbc/query db sql-query)

    ;(println "query-rezult:" query-rezult)
    ;query-rezult
    (doseq [r query-rezult]
      (println ">" r))))


(comment "
{
  inDate(x: "ZZZ")
  hello(world: "ZZZ")

  #hero(episode: NEWHOPE) {
  #  name
  #  appearsIn
    #... on Human {
    #  homePlanet
    #}
    #... on Droid {
    #  primaryFunction
    #}
  #}
  diap(l:1000, r:1005) {
    id
    name
    appearsIn
    #friends {
    #  id
    #  name
    #}
  }
	human(id: "2000") {
    id
    name
    friends {
      id
      name
    }
  }
}

#mutation {
#  createHuman(name: "Petya", friends: ["1002", "1003"]) {
#    id
#  }
#}
")
