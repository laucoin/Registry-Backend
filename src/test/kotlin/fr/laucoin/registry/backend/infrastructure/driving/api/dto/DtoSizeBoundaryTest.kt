package fr.laucoin.registry.backend.infrastructure.driving.api.dto

import com.tngtech.archunit.core.importer.ClassFileImporter
import jakarta.validation.constraints.Size
import java.lang.reflect.Field
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DtoSizeBoundaryTest {
	@Test
	fun `Should every text and collection of the writer and query DTOs declare an upper bound`() {
		// Arrange
		val inputs = inputFields()

		// Act
		val unbounded = inputs.filter { (it.getAnnotation(Size::class.java)?.max ?: Int.MAX_VALUE) == Int.MAX_VALUE }

		// Assert
		assertTrue(inputs.size > MIN_INPUT_FIELDS)
		assertEquals(emptyList<String>(), unbounded.map(::label))
	}

	@Test
	fun `Should every declared bound keep its minimum below its maximum`() {
		// Arrange
		val sizes = inputFields().mapNotNull { it.getAnnotation(Size::class.java) }

		// Act
		val inverted = sizes.filter { it.min > it.max }

		// Assert
		assertEquals(emptyList<Size>(), inverted)
	}

	private fun inputFields(): List<Field> {
		val base = DtoSizeBoundaryTest::class.java.packageName
		return ClassFileImporter()
			.importPackages("$base.writer", "$base.query")
			.flatMap { it.reflect().declaredFields.toList() }
			.filter { it.type == String::class.java || Collection::class.java.isAssignableFrom(it.type) }
			.filterNot { it.isSynthetic }
	}

	private fun label(field: Field) = "${field.declaringClass.simpleName}.${field.name}"

	private companion object {
		const val MIN_INPUT_FIELDS = 15
	}
}
