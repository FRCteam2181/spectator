## server api

* /api/v1/session
  * /host
    * POST
      * request body: Session
      * response body: SessionAck
  * /join?key={sessionKey}
    * POST
      * query params: "key": SessionKey
      * request body: Guest
      * response body: GuestAck
  * /{sessionId}
    * GET
      * response body: Session
    * /records
      * GET
        * response body: DataTable
    * /record
      * POST
        * request body: DataTableRecord
        * response body: DataTable
    * /reports
      * POST/PUT
        * request body: ReportsList
        * response body: ReportsList
      * GET
        * response body: ReportsList
    * /aggregate
      * GET
        * response body: AggregatedData
    * /close
      * DELETE
        * response body: Session
