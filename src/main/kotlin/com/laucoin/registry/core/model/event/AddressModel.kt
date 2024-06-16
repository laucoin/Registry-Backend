package com.laucoin.registry.core.model.event

import com.laucoin.registry.core.model.util.GenericModel

class AddressModel(
    var number: String? = null,
    var street: String? = null,
    var complementaryInformation: String? = null,
    var zipCode: String? = null,
    var city: String? = null,
    var country: String? = null,
): GenericModel() {
    override fun filterFields(): List<String?> = listOf(number, street, complementaryInformation, zipCode, city, country)
}
