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
  (update session :guests assoc user-ip guest))

(defn post-session-record [session new-record]
  (update session :records conj new-record)
  ; todo - recalculate aggregate & reports
  )

(defn upsert-reports [session reports-list]
  (update session :config assoc :reports reports-list)
  ; todo - recalculate session reports
  )