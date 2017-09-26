(ns graphql-postgres-clj.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [ring.util.response :as response :refer [redirect]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer :all]
            [cheshire.core :as json]
            [graphql-postgres-clj.postgres :as postgres]
            ))

; lein ring server-headless
; http://localhost:3002/index.html

(defroutes routes
  (GET "/" [] (redirect "index.html"))
  (GET "/graphql" [schema query variables :as request]
       (println "GET query: " query)
       (response/response (postgres/execute query variables nil)))
  (POST "/graphql" [schema query variables operationName :as request]
        (response/response
         (try
           (let [result (postgres/execute query (json/parse-string variables) operationName)]
             ;(prn "query:" query)
             ;(prn "variables:" variables)
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
