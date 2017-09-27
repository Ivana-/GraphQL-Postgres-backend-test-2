(defproject graphql-postgres-clj "0.1.0-SNAPSHOT"
  :description "graphql-postgres-clj project"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.0"]
                 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-cors "0.1.8"]

                 [org.clojure/java.jdbc "0.7.1"]
                 [clj-antlr "0.2.4"]
                 ]

  :main ^:skip-aot graphql-postgres-clj.core
  :target-path "target/%s"
  :resource-paths ["build" "jar/postgresql-42.1.4.jar"]
  :profiles {:uberjar {:aot :all}
             :dev {; :ring {:stacktrace-middleware prone.middleware/wrap-exceptions}
                   ; http://localhost:3000/prone/latest
                   :resource-paths ["build"]
                   :dependencies [[prone "1.1.1"]]}}
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler graphql-postgres-clj.handler/app
         :auto-reload? true
         :port 3002
         })
