package com.laucoin.registry.core.model.event

import com.laucoin.registry.core.model.util.GenericModel
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(addressTable)
class AddressModel(
    val number: String? = null,
    val street: String? = null,
    @Column(addressComplementaryInformationField)
    val complementaryInformation: String? = null,
    @Column(addressZipCodeField)
    val zipCode: String? = null,
    val city: String? = null,
    val country: String? = null,
): GenericModel()
