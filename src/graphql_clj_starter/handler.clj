(ns graphql-clj-starter.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [ring.util.response :as response :refer [redirect]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer :all]
            [cheshire.core :as json]
            [graphql-clj-starter.graphql :as graphql]

            [graphql-clj-starter.test-postgres :as test-postgres]
            ))


; lein ring server-headless
; http://localhost:3002/index.html

;; Andrey
(defn foo-response []
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<html><body><h1>Hello, My Real World!</h1></body></html>"})

(defroutes routes

  (GET "/about" [] (foo-response))

  (GET "/" [] (redirect "index.html"))
  (GET "/graphql" [schema query variables :as request]
       (println "GET query: " query)
       (response/response

        (graphql/execute query variables)
        ;test-postgres/starter-schema

        ))
  (POST "/graphql" [schema query variables operationName :as request]
        ;; (prn "operationName:" operationName)
        ;; (println "POST query: " query)
        ;; (println "Post variables: " (json/parse-string variables))
        (response/response
         (try
           ;(let [result (graphql/execute query (json/parse-string variables) operationName)]
           ;  (prn (class result))
           ;  (prn "result:" result)
           ;  result)

           (let [result (graphql/execute ;test-postgres/execute
                          query (json/parse-string variables) operationName)
                 ;"{}"
                 ]
             ;(prn "zzzzzzzzzzzzz schema:" schema)
             ;(prn "zzzzzzzzzzzzz query:" query)
             ;(prn "zzzzzzzzzzzzz variables:" variables)
             ;;(prn "zzzzzzzzzzzzz (json/parse-string variables):" (json/parse-string variables))
             ;(prn "zzzzzzzzzzzzz operationName:" operationName)
             ;(prn "zzzzzzzzzzzzz result:" result)
             result)

           (catch Throwable e
             (println e)))))
  (route/resources "/" {:root ""})
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> routes
      wrap-json-response
      (wrap-cors :access-control-allow-origin [#"http://localhost:8080" #"http://.*"]
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-defaults api-defaults)
      (wrap-json-params)))


