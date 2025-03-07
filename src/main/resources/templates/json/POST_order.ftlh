<#--
  @api-route: /api/orders
  @api-method: POST
  @api-headers: {"Content-Type": "application/json", "Accept": "application/json"}
-->
{
  "order": {
    "id": ${result.order_id},
    "date": "${result.order_date?string["yyyy-MM-dd'T'HH:mm:ss"]}",
    "status": "${result.status}",
    "customer": {
      "id": ${result.customer_id},
      "name": "${result.customer_name}",
      "email": "${result.customer_email}"
    },
    "item": {
      "productId": ${result.product_id},
      "productName": "${result.product_name}",
      "price": ${result.price?c},
      "quantity": ${result.quantity},
      "totalPrice": ${result.total_price?c}
    }
  },
  "submittedAt": "${.now?string["yyyy-MM-dd'T'HH:mm:ss"]}"
}