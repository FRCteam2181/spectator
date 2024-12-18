(ns spectator.session 
  (:require
   [clojure.core.async :as async]) 
  (:import
   [clojure.lang ExceptionInfo]))

(defprotocol Session
  (join [this user-ip guest])
  (get-session [this user-ip])
  (get-session-records [this user-ip])
  (post-session-record [this user-ip new-record])
  (get-session-reports [this user-ip])
  (upsert-reports [this user-ip reports-list])
  (get-aggregate [this user-ip])
  (close-session [this user-ip]))

(defn- verify-host-ip [host-ip user-ip]
  (when (not= user-ip host-ip)
    (throw (ExceptionInfo. {:error-type :user-is-not-host}))))

(defn- verify-user-ip [session user-ip]
  (when (and (not= (:host-ip @session) user-ip) (not (contains? (:guests @session) user-ip)))
    (throw (ExceptionInfo. {:error-type :user-is-not-guest}))))

(defmulti apply-action :action)

(defn build [schematic host-ip]
  (let [session (atom schematic)
        action-chan (async/chan 200)
        exists (atom true)]
    (async/thread
      (while @exists
        (let [action (async/<!! action-chan)]
          (swap! session (apply-action action)))))
    (reify Session
      (join [this user-ip guest]
        (async/>!! action-chan {:action :join :user-ip user-ip :guest guest}))
      (get-session [this user-ip]
        (verify-host-ip host-ip user-ip)
        @session) 
      (get-session-records [this user-ip]
        (verify-user-ip @session user-ip)
        (:records @session)) 
      (post-session-record [this user-ip new-record]
        (verify-user-ip @session user-ip)
        (async/>!! action-chan {:action :post-session-record :new-record new-record})
        new-record)
      (get-session-reports [this user-ip]
        (verify-user-ip @session user-ip)
        (:reports @session)) 
      (upsert-reports [this user-ip reports-list]
        (verify-user-ip @session user-ip)
        (async/>!! action-chan {:action :upsert-reports :reports-list reports-list})
        reports-list)
      (get-aggregate [this user-ip]
        (verify-user-ip @session user-ip)
        (:aggregated @session))
      (close-session [this user-ip]
        (verify-host-ip host-ip user-ip)
        (reset! exists false)
        @session))))
                       
(defmethod apply-action :join [{user-ip :user-ip guest :guest}]
  ; todo - validate guest uniqueness and not host
  #(update % :guests assoc user-ip guest))

(defmethod apply-action :post-session-record [{new-record :new-record}]
  ; todo - validate data schema, run reports
  #(update % :records conj new-record))

(defmethod apply-action :upsert-reports [{reports-list :reports-list}]
  ; todo - run reports
  #(assoc % :reports reports-list))