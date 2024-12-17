(ns spectator.schema
  (:require [schema.core :as s]))

(s/defschema Session 
  {})

(s/defschema SessionKey s/Str)

(s/defschema SessionId s/Str)

(s/defschema SessionAck
  {})

(s/defschema Guest
  {})

(s/defschema GuestAck
  {})

(s/defschema DataTable
  {})

(s/defschema DataTableRecord
  {})

(s/defschema ReportsList
  {})

(s/defschema AggregatedData
  {})