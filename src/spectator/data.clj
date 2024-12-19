(ns spectator.data)

(defn init-session [config host-ip session-id]
  {:config config
   :host-ip host-ip
   :session-id session-id
   :guests {}
   :data []
   :aggregate []
   :reports []})

(defn join [session user-ip guest]
  ; todo ?
  (update session :guests assoc user-ip guest))

(defn post-session-record [session new-record]
  ; todo ?
  (update session :records conj new-record))

(defn upsert-reports [session reports-list]
  ; todo ?
  (assoc session :reports reports-list))