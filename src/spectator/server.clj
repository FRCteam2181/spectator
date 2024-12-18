(ns spectator.server
  (:require
   [cheshire.core :refer [parse-string]]
   [clojure.pprint :as pprint]
   [compojure.api.core :as api]
   [compojure.api.sweet :as sweet]
   [compojure.route :as route]
   [environ.core :refer [env]]
   [org.httpkit.server :as server]
   [ring.util.http-response :as http]
   [ring.util.response :as resp]
   [schema.core :as s]
   [spectator.errors :as errors]
   [spectator.schema :as schema]
   [spectator.session-manager :as sm])
   (:import
    (java.io ByteArrayInputStream)))

(defn- build-reports-upsert [session-manager]
  {:summary    ""
   :parameters {:path {:session-id s/Str}
                :body schema/ReportsList}
   :consumes   ["application/json"]
   :produces   ["application/json"]
   :responses  {200 {:schema schema/ReportsList}}
   :handler    (errors/wrap-error-handling [:path :session-id] schema/SessionId
                (fn [{:keys [body] {:keys [session-id]} :path user-ip :remote-addr :as req}]
                  (pprint/pprint {:req req})
                  (pprint/pprint {:body body :session-id session-id :user-ip user-ip})
                  (let [text (slurp ^ByteArrayInputStream body)
                        _ (println text)
                        result (->> (parse-string text true)
                                    (sm/upsert-reports user-ip session-manager session-id))]
                    (-> (http/ok result)
                        (http/content-type "application/json")))))})

(defn build-app []
  (let [session-manager (sm/build)
        reports-upsert (build-reports-upsert session-manager)]
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
                      :handler    (fn [{:keys [body] user-ip :remote-addr :as req}]
                                    (pprint/pprint {:req req :user-ip user-ip})
                                    (let [text (slurp ^ByteArrayInputStream body)
                                          result (->> (parse-string text true)
                                                      (sm/host user-ip session-manager))]
                                      (-> (http/ok result)
                                          (http/content-type "application/json"))))}}))
           (api/context
             "/join" []
             (sweet/resource
              {:description ""
               :post {:summary    ""
                      :parameters {:query-params {:key s/Str}
                                   :body schema/Guest}
                      :consumes   ["application/json"]
                      :produces   ["application/json"]
                      :responses  {200 {:schema schema/GuestAck}}
                      :handler    (errors/wrap-error-handling
                                   [:query-params :key] schema/SessionId
                                   (fn [{:keys [body] {:keys [key]} :query-params user-ip :remote-addr :as req}]
                                     (pprint/pprint {:req req})
                                     (print/pprint {:key key :body body})
                                     (let [text (slurp ^ByteArrayInputStream body)
                                           result (->> (parse-string text true)
                                                       (sm/join user-ip session-manager key))]
                                       (-> (http/ok result)
                                           (http/content-type "application/json")))))}}))
           (api/context
             "/:session-id" []
             (sweet/resource
              {:description ""
               :get {:summary    ""
                     :parameters {:path {:session-id s/Str}}
                     :produces   ["application/json"]
                     :responses  {200 {:schema schema/Session}}
                     :handler    (errors/wrap-error-handling
                                  [:path :session-id] schema/SessionId
                                  (fn [{{:keys [session-id]} :path user-ip :remote-addr :as req}]
                                    (pprint/pprint {:req req})
                                    (pprint/pprint {:session-id session-id})
                                    (let [result (sm/get-session session-manager user-ip session-id)]
                                      (-> (http/ok result)
                                          (http/content-type "application/json")))))}})
             (api/context
               "/records" []
               (sweet/resource
                {:description ""
                 :get {:summary    ""
                       :parameters {:path {:session-id s/Str}}
                       :produces   ["application/json"]
                       :responses  {200 {:schema schema/DataTable}}
                       :handler    (errors/wrap-error-handling
                                    [:path :session-id] schema/SessionId
                                    (fn [{{:keys [session-id]} :path user-ip :remote-addr}]
                                      (let [result (sm/get-session-records user-ip session-manager session-id)]
                                        (-> (http/ok result)
                                            (http/content-type "application/json")))))}}))
             (api/context
               "/record" []
               (sweet/resource
                {:description ""
                 :post {:summary    ""
                        :parameters {:path {:session-id s/Str}
                                     :body schema/DataTableRecord}
                        :consumes   ["application/json"]
                        :produces   ["application/json"]
                        :responses  {200 {:schema schema/ReportsList}}
                        :handler    (errors/wrap-error-handling
                                     [:path :session-id] schema/SessionId
                                     (fn [{:keys [body] {:keys [session-id]} :path user-ip :remote-addr :as req}]
                                       (pprint/pprint {:req req})
                                       (pprint/pprint {:session-id session-id :body body})
                                       (let [text (slurp ^ByteArrayInputStream body)
                                             result (->> (parse-string text true)
                                                         (sm/post-session-record user-ip session-manager session-id))]
                                         (-> (http/ok result)
                                             (http/content-type "application/json")))))}}))
             (api/context
               "/reports" []
               (sweet/resource
                {:description ""
                 :get {:summary    ""
                       :parameters {:path {:session-id s/Str}}
                       :produces   ["application/json"]
                       :responses  {200 {:schema schema/ReportsList}}
                       :handler    (errors/wrap-error-handling
                                    [:path :session-id] schema/SessionId
                                    (fn [{{:keys [session-id]} :path user-ip :remote-addr}]
                                      (let [result (sm/get-session-reports user-ip session-manager session-id)]
                                        (-> (http/ok result)
                                            (http/content-type "text/plain")))))}
                 :post reports-upsert
                 :put reports-upsert}))
             (api/context
               "/aggregate" []
               (sweet/resource
                {:description ""
                 :get {:summary    ""
                       :parameters {:path {:session-id s/Str}}
                       :produces   ["application/json"]
                       :responses  {200 {:schema schema/AggregatedData}}
                       :handler    (errors/wrap-error-handling
                                    [:path :session-id] schema/SessionId
                                    (fn [{{:keys [session-id]} :path user-ip :remote-addr}]
                                      (let [result (sm/get-aggregate user-ip session-manager session-id)]
                                        (-> (http/ok result)
                                            (http/content-type "application/json")))))}}))
             (api/context
               "/close" []
               (sweet/resource
                {:description ""
                 :delete {:summary    ""
                          :parameters {:path {:session-id s/Str}}
                          :produces   ["application/json"]
                          :responses  {200 {:schema schema/Session}}
                          :handler    (errors/wrap-error-handling
                                       [:path :session-id] schema/SessionId
                                       (fn [{{:keys [session-id]} :path user-ip :remote-addr}]
                                        (let [result (sm/close-session user-ip session-manager session-id)]
                                          (-> (http/ok result)
                                              (http/content-type "application/json")))))}}))))
         (sweet/GET "/" [] (resp/redirect "/index.html")))
        (sweet/routes
         (route/resources "/")
         (route/not-found "404 Not Found")))))


(defn -main [& [port]]
  (let [my-app (build-app)
        port (Integer/parseInt (str (or port (env :port) 5000)))]
    (println port)
    (server/run-server my-app
                       {:port     port
                        :join?    false
                        :max-line 131072})))