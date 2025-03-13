<#--
  @api-route: /discounts
  @api-method: POST
  @api-headers: {"Content-Type": "application/json", "Accept": "application/json"}
-->
{
    "default_label": "${result.default_label}",
    "reference": "${result.reference}",
    "external_ref": "${result.external_ref}",
    "condition_type": "${result.condition_type}",
    "auto": true,
    "discount_type": "${result.discount_type}",
    "value": "${result.value?c}",
    "basket_impacted": ${result.basket_impacted},
    "repeatable": ${result.repeatable},
    "cumulable": ${result.cumulable},
    "entity": {
        "id": "1"
    },
    "enabled": true,
    "condition_time": {
        "weekdays": null,
        "date_start": "${result.condition_time_date_start?string('yyyy-MM-dd')}",
        "date_end": "${result.condition_time_date_end?string('yyyy-MM-dd')}",
        "hour_start": "00:00",
        "hour_end": "23:59"
    },
    "impacted_product_price": "${result.impacted_product_price}",
    "product_groups": [
        {
            "product": null,
            "product_group": {
                "uuid": <#if result.product_group_uuid??>
                            "${result.product_group_uuid}"
                        <#else>
                            null
                        </#if>
            },
            "division": null,
            "min_item_number": "${result.product_groups_min_item_number}",
            "product_group_impacted": "${result.product_groups_product_group_impacted}",
            "number_impacted": "${result.product_groups_number_impacted}"
        }
    ]
}