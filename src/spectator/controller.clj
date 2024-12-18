(ns spectator.controller)

(defn host [session] {})

(defn join [guest session-key] {})

(defn get-session [session-id] {})

(defn get-session-records [session-id] {})

(defn post-session-record [new-record session-id] {})

(defn get-session-reports [session-id] {})

(defn upsert-reports [reports-list session-id] {})

(defn get-aggregate [session-id] {})

(defn close-session [session-id] {})