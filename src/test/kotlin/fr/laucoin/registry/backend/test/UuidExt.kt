package fr.laucoin.registry.backend.test

import java.util.UUID

object UuidExt {
	fun UUID.distinctCopy(): UUID = UUID.fromString(this.toString())
}
