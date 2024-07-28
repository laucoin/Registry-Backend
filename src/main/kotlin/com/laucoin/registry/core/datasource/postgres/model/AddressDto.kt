package com.laucoin.registry.core.datasource.postgres.model

import com.laucoin.registry.core.datasource.postgres.model.util.GenericDto
import com.laucoin.registry.core.datasource.postgres.model.util.addressComplementaryInformationField
import com.laucoin.registry.core.datasource.postgres.model.util.addressTable
import com.laucoin.registry.core.datasource.postgres.model.util.addressZipCodeField
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(addressTable)
class AddressDto(
    val number: String? = null,
    val street: String? = null,
    @Column(addressComplementaryInformationField)
    val complementaryInformation: String? = null,
    @Column(addressZipCodeField)
    val zipCode: String? = null,
    val city: String? = null,
    val country: String? = null,
): GenericDto()
