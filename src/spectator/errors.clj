(ns spectator.errors 
  (:require
   [ring.util.http-response :as http]
   [schema.core :as s]) 
  (:import
   [clojure.lang ExceptionInfo]))

(defmulti handle-error :error-type)

(defmethod handle-error :default
  [error] (throw (ExceptionInfo. "" error)))

(defmethod handle-error :session-id-not-found
  [error]
  (-> (http/bad-request error)
      (http/content-type "application/json")))

(defmethod handle-error :user-ip-not-found
  [error]
  (-> (http/unauthorized error)
      (http/content-type "application/json")))

(defn wrap-error-handling [req-path schema handler]
  (fn [req]
    (let [session-id (get-in req req-path)
          error (try
                  (s/validate schema session-id)
                  nil
                  (catch Throwable t
                    (.getMessage t)))]
      (if (nil? error)
        (try
          (handler req)
          (catch ExceptionInfo info
            (let [data (ex-data info)]
              (handle-error data))))
        (-> (http/bad-request {:error error})
            (http/content-type "application/json"))))))

