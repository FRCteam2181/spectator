(ns spectator.schema
  (:require [schema.core :as s]))

(def ^:private dummy {:a s/Str})

(s/defschema Session 
  dummy)

(s/defschema SessionId (s/pred string?))

(s/defschema SessionAck
  {:session-id SessionId})

(s/defschema Guest
  dummy)

(s/defschema GuestAck
  dummy)

(s/defschema DataTable
  dummy)

(s/defschema DataTableRecord
  dummy)

(s/defschema ReportsList
  dummy)

(s/defschema AggregatedData
  dummy)