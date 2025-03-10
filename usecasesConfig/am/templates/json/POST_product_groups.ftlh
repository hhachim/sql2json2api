<#--
  @api-route: /api/product-groups
  @api-method: POST
  @api-headers: {"Content-Type": "application/json", "Accept": "application/json"}
-->
{
  "entity": { "uuid": "fe11016d-94a5-11e8-b830-0242ac140001" },
  "product_group_label": "${result.product_group_label}",
  "instructions": "${result.instructions_label?c}",
  "instructions_label": "${result.instructions_label?c}"
}