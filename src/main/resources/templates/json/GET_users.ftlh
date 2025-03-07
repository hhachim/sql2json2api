<#--
  @api-route: /api/users/${result.id}
  @api-method: GET
  @api-headers: {"Content-Type": "application/json", "Accept": "application/json"}
  @api-params: {"includeDetails": true}
-->
{
  "user": {
    "id": ${result.id},
    "username": "${result.username}",
    "email": "${result.email!""}"
  },
  "timestamp": "${.now?string["yyyy-MM-dd'T'HH:mm:ss"]}"
}