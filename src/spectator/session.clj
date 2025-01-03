(ns spectator.session 
  (:require
   [clojure.core.async :as async]
   [spectator.data :as data]
   [spectator.session :as session]) 
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
    (throw (ExceptionInfo. "" {:error-type :user-is-not-host}))))

(defn- verify-user-ip [session user-ip]
  (when (and (not= (:host-ip @session) user-ip) (not (contains? (:guests @session) user-ip)))
    (throw (ExceptionInfo. "" {:error-type :user-is-not-guest}))))

(defn build [config host host-ip session-id]
  (let [session-atom (atom (data/init-session config host-ip session-id))
        action-chan (async/chan 200)
        exists (atom true)]
    (async/thread
      (while @exists
        (let [action (async/<!! action-chan)]
          (swap! session-atom action))))
    (let [session (reify Session
                    (join [_ user-ip guest]
                      (async/>!! action-chan #(data/join % user-ip guest))
                      guest)
                    (get-session [_ user-ip]
                      (verify-host-ip host-ip user-ip)
                      @session-atom)
                    (get-session-records [_ user-ip]
                      (verify-user-ip @session-atom user-ip)
                      (get-in @session-atom [:config :reports]))
                    (post-session-record [_ user-ip new-record]
                      (verify-user-ip @session-atom user-ip)
                      (async/>!! action-chan #(data/post-session-record % new-record))
                      new-record)
                    (get-session-reports [_ user-ip]
                      (verify-user-ip @session-atom user-ip)
                      (:reports @session-atom))
                    (upsert-reports [_ user-ip reports-list]
                      (verify-user-ip @session-atom user-ip)
                      (async/>!! action-chan #(data/upsert-reports % reports-list))
                      reports-list)
                    (get-aggregate [_ user-ip]
                      (verify-user-ip @session-atom user-ip)
                      (select-keys @session-atom [:aggregate :reports]))
                    (close-session [_ user-ip]
                      (verify-host-ip host-ip user-ip)
                      (reset! exists false)
                      @session-atom))]
      (join session host-ip host)
      session)))
                     
                      