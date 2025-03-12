<#--
  @api-route: https://httpbin.org/anything/${result.id}
  @api-method: POST
  @api-headers: {"Content-Type": "application/json", "Accept": "application/json", "X-Test-Header": "test-value"}
  @api-params: {"source": "sql2json2api-demo", "test": true}
-->
{
  "user": {
    "id": ${result.id},
    "name": "${result.username}",
    "email": "${result.email!""}"
  },
  "timestamp": "${.now?string["yyyy-MM-dd'T'HH:mm:ss"]}"
}