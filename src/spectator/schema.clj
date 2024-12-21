(ns spectator.schema
  (:require [schema.core :as s]))

(s/defschema ReportsList
  [{:reportTitle s/Str
    :orderBy 
    [{:field s/Str
      :direction (s/enum "asc" "desc")}]}])

(s/defschema Expression
  [(s/one (s/enum "+",
                  "-",
                  "*",
                  "/",
                  "%",
                  "&",
                  "|",
                  "min",
                  "max",
                  "abs",
                  "ceil",
                  "floor",
                  "round",
                  "pow",
                  "true2One",
                  "false2One") "operation")
   (s/cond-pre (s/recursive #'Expression)
               (s/cond-pre s/Str s/Num))])

(s/defschema SessionConfig
  {:$schema s/Str
   :title s/Str
   :page_title s/Str
   :aggregateBy s/Str
   :distinct [s/Str]
   (s/optional-key :reports) ReportsList
   :sections
   [{:title s/Str
     :order s/Int}]
   :fields
   [{:title s/Str
     :sectionTitle s/Str
     :type (s/enum "text" "number" "boolean" "range" "counter" "enum" "enum-set" "markdown" "calculated")
     :required s/Bool
     :code s/Str
     (s/optional-key :disabled) s/Bool
     (s/optional-key :preserveDataOnReset) s/Bool
     (s/optional-key :value) s/Any
     (s/optional-key :defaultValue) s/Any
     (s/optional-key :choices) [s/Str]
     (s/optional-key :min) s/Num
     (s/optional-key :max) s/Num
     (s/optional-key :step) s/Num
     (s/optional-key :autoIncrementOnReset) s/Num
     (s/optional-key :columnOrder) s/Int
     (s/optional-key :calculation) Expression}]
   :aggregators
   [{:title s/Str
     :code s/Str
     :fieldCode s/Str
     :aggFunction (s/enum "min",
                          "max",
                          "sum",
                          "product",
                          "mean",
                          "median",
                          "mode",
                          "countTrue",
                          "countFalse",
                          "countIf",
                          "countIfNot",
                          "union",
                          "intersection",
                          "minCount",
                          "maxCount",
                          "unionCount",
                          "intersectionCount")
     :additionalArguments [s/Any]}]})

(s/defschema DataTableRecord
  {s/Keyword s/Any})

(s/defschema DataTable
  [DataTableRecord])

(s/defschema AggregatedReports
  [{:reportTitle s/Str
    :data DataTable}])

(s/defschema AggregatedData
  {:aggregate DataTable
   :reports AggregatedReports})

(s/defschema SessionId (s/pred string?))

(s/defschema IpAddress (s/pred string?))

(s/defschema Guest
  {(s/optional-key :user-ip) IpAddress
   :initials s/Str
   :firstName s/Str
   :lastName s/Str
   (s/optional-key :middleName) s/Str
   (s/optional-key :teamNumber) s/Str})

(s/defschema SessionInit
  {:config SessionConfig
   :host Guest})

(s/defschema Session 
  {:config SessionConfig
   :host-ip IpAddress
   :session-id SessionId
   :guests {IpAddress Guest}
   :data DataTable
   :aggregate DataTable
   :reports AggregatedReports})

(s/defschema SessionAck
  {:session-id SessionId})