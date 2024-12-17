(ns spectator.server
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [compojure.api.core :as api]
            [compojure.api.sweet :as sweet]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [org.httpkit.server :as server]
            [ring.util.http-response :as http]
            [ring.util.response :as resp]
            [spectator.schema :as schema]
            [spectator.controller :as ctrl])
   (:import (java.io ByteArrayInputStream)))

(def ^:private reports-upsert
  {:summary    ""
   :parameters {:path-params {:session-id schema/SessionId}
                :body schema/ReportsList}
   :consumes   ["text/plain"]
   :produces   ["text/plain"]
   :responses  {200 {:schema schema/ReportsList}}
   :handler    (fn [{:keys [body] {:keys [session-id]} :path-params :as req}]
                 (pprint/pprint {:req req})
                 (let [text (slurp ^ByteArrayInputStream body)
                       _ (println text)
                       result (-> (edn/read-string text)
                                  (ctrl/upsert-reports session-id))]
                   (-> (http/ok result)
                       (http/content-type "application/json"))))})

(defn build-app []
  (-> {:swagger
       {:ui   "/swagger/ui"
        :spec "/swagger.json"
        :data {:info {:title       "Spectator"
                      :description "robotics scouting app"}
               :tags [{:name "api" :description "background api"}]}}}
      (sweet/api
       (api/context
         "/api/v1/session" []
         :tags ["api"]
         (api/context
           "/host" []
           (sweet/resource
            {:description ""
             :post {:summary    ""
                    :parameters {:body schema/Session}
                    :consumes   ["application/json"]
                    :produces   ["application/json"]
                    :responses  {200 {:schema schema/SessionAck}}
                    :handler    (fn [{:keys [body] :as req}]
                                  (pprint/pprint {:req req})
                                  (let [text (slurp ^ByteArrayInputStream body)
                                        _ (println text)
                                        result (-> (edn/read-string text)
                                                   (ctrl/host))]
                                    (-> (http/ok result)
                                        (http/content-type "application/json"))))}}))
         (api/context
           "/join" []
           (sweet/resource
            {:description ""
             :post {:summary    ""
                    :parameters {:query-params {:key schema/SessionKey}
                                 :body schema/Guest}
                    :consumes   ["application/json"]
                    :produces   ["application/json"]
                    :responses  {200 {:schema schema/GuestAck}}
                    :handler    (fn [{:keys [body] {:keys [key]} :query-params}]
                                  (let [text (slurp ^ByteArrayInputStream body)
                                        _ (println text)
                                        result (-> (edn/read-string text)
                                                   (ctrl/join key))]
                                    (-> (http/ok result)
                                        (http/content-type "application/json"))))}}))
         (api/context
           "/:session-id" []
           (sweet/resource
            {:description ""
             :get {:summary    ""
                   :parameters {:path-params {:session-id schema/SessionId}}
                   :produces   ["application/json"]
                   :responses  {200 {:schema schema/Session}}
                   :handler    (fn [{{:keys [session-id]} :path-params}]
                                 (let [result (ctrl/get-session session-id)]
                                   (-> (http/ok result)
                                       (http/content-type "application/json"))))}})
           (api/context
             "/records" []
             (sweet/resource
              {:description ""
               :get {:summary    ""
                     :parameters {:path-params {:session-id schema/SessionId}}
                     :produces   ["application/json"]
                     :responses  {200 {:schema schema/DataTable}}
                     :handler    (fn [{{:keys [session-id]} :path-params}]
                                   (let [result (ctrl/get-session-records session-id)]
                                     (-> (http/ok result)
                                         (http/content-type "application/json"))))}}))
           (api/context
             "/record" []
             (sweet/resource
              {:description ""
               :post {:summary    ""
                     :parameters {:path-params {:session-id schema/SessionId}
                                  :body schema/DataTableRecord}
                     :consumes   ["application/json"]
                     :produces   ["application/json"]
                     :responses  {200 {:schema schema/ReportsList}}
                     :handler    (fn [{:keys [body] {:keys [session-id]} :path-params}]
                                   (let [text (slurp ^ByteArrayInputStream body)
                                         _ (println text)
                                         result (-> (edn/read-string text) 
                                                    (ctrl/post-session-record session-id))]
                                     (-> (http/ok result)
                                         (http/content-type "application/json"))))}}))
           (api/context
             "/reports" []
             (sweet/resource
              {:description ""
               :get {:summary    ""
                     :parameters {:path-params {:session-id schema/SessionId}}
                     :produces   ["application/json"]
                     :responses  {200 {:schema schema/ReportsList}}
                     :handler    (fn [{{:keys [session-id]} :path-params}]
                                   (let [result (ctrl/get-session-reports session-id)]
                                     (-> (http/ok result)
                                         (http/content-type "text/plain"))))}
               :post reports-upsert
               :put reports-upsert}))
           (api/context
             "/aggregate" []
             (sweet/resource
              {:description ""
               :get {:summary    ""
                     :parameters {:path-params {:session-id schema/SessionId}}
                     :produces   ["application/json"]
                     :responses  {200 {:schema schema/AggregatedData}}
                     :handler    (fn [{{:keys [session-id]} :path-params}]
                                   (let [result (ctrl/get-aggregate session-id)]
                                     (-> (http/ok result)
                                         (http/content-type "text/plain"))))}}))
           (api/context
             "/close" []
             (sweet/resource
              {:description ""
               :delete {:summary    ""
                     :parameters {:path-params {:session-id schema/SessionId}}
                     :produces   ["application/json"]
                     :responses  {200 {:schema schema/Session}}
                     :handler    (fn [{{:keys [session-id]} :path-params}]
                                   (let [result (ctrl/close-session session-id)]
                                     (-> (http/ok result)
                                         (http/content-type "application/json"))))}}))))
       (sweet/GET "/" [] (resp/redirect "/index.html")))
      (sweet/routes
       (route/resources "/")
       (route/not-found "404 Not Found"))))


(defn -main [& [port]]
  (let [my-app (build-app)
        port (Integer/parseInt (str (or port (env :port) 5000)))]
    (println port)
    (server/run-server my-app
                       {:port     port
                        :join?    false
                        :max-line 131072})))