(ns spectator.data)

(defn init-session [config host-ip]
  ; todo
  config)

(defn join [session user-ip guest]
  (update session :guests assoc user-ip guest))

(defn post-session-record [session new-record]
  (update session :records conj new-record))

(defn upsert-reports [session reports-list]
  (assoc session :reports reports-list))