(ns spectator.session-manager 
  (:require
   [clojure.core.async :as async]
   [clojure.pprint :as pp]
   [spectator.session :as session]) 
  (:import
   [clojure.lang ExceptionInfo]))

(defprotocol SessionManager 
  (host [this user-ip session])
  (join [this user-ip session-key guest])
  (get-session [this user-ip session-id])
  (get-session-records [this user-ip session-id])
  (post-session-record [this user-ip session-id new-record])
  (get-session-reports [this user-ip session-id])
  (upsert-reports [this user-ip session-id reports-list])
  (get-aggregate [this user-ip session-id])
  (close-session [this user-ip session-id]))

(defn- get-my-session [sessions session-id]
  (if (contains? sessions session-id)
    (get sessions session-id)
    (throw (ExceptionInfo. {:error-type :session-id-not-found :session-id session-id}))))

(defmulti apply-action :action)

(defmethod apply-action :new [{session-id :session-id session :session}]
  #(assoc % session-id session))

(defmethod apply-action :new [{session-id :session-id}]
  #(dissoc % session-id))

(defn build []
  (let [sessions (atom {})
        new-session-chan (async/chan 200)] 
    (async/thread
      (while true
        (let [action (async/<!! new-session-chan)]
          (swap! sessions (apply-action action)))))
    (reify SessionManager
      (host [_ user-ip session] 
        (let [session-id (random-uuid)]
          (pp/pprint {:fn :host :user-ip user-ip :session session})
          (async/>!! new-session-chan
                     {:action :new
                      :session-id session-id
                      :session (session/build session user-ip)})
          {:session-id session-id}))
      (join [_ user-ip session-id guest] 
        (pp/pprint {:fn :join :user-ip user-ip :session-id session-id :guest guest})
        (session/join (get-my-session sessions session-id) user-ip guest))
      (get-session [_ user-ip session-id] 
        (pp/pprint {:fn :get-session :user-ip user-ip :session-id session-id})
        (session/get-session (get-my-session sessions session-id) user-ip))
      (get-session-records [_ user-ip session-id] 
        (pp/pprint {:fn :get-session-records :user-ip user-ip :session-id session-id})
        (session/get-session-records (get-my-session sessions session-id) user-ip))
      (post-session-record [_ user-ip session-id new-record] 
        (pp/pprint {:fn :post-session-record :user-ip user-ip :session-id session-id :new-record new-record})
        (session/post-session-record (get-my-session sessions session-id) user-ip new-record))
      (get-session-reports [_ user-ip session-id] 
        (pp/pprint {:fn :get-session-reports :user-ip user-ip :session-id session-id})
        (session/get-session-reports (get-my-session sessions session-id) user-ip))
      (upsert-reports [_ user-ip session-id reports-list] 
        (pp/pprint {:fn :upsert-reports :user-ip user-ip :session-id session-id :reports-list reports-list})
        (session/upsert-reports (get-my-session sessions session-id) user-ip reports-list))
      (get-aggregate [_ user-ip session-id] 
        (pp/pprint {:fn :get-aggregate :user-ip user-ip :session-id session-id})
        (session/get-aggregate (get-my-session sessions session-id) user-ip))
      (close-session [_ user-ip session-id] 
        (pp/pprint {:fn :close-session :user-ip user-ip :session-id session-id}) 
        (let [session (get-my-session sessions session-id)]
          (async/>!! new-session-chan
                     {:action :close
                      :session-id session-id})
          (session/close-session session user-ip))))))