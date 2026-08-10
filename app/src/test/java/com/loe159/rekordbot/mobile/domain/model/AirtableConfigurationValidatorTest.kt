package com.loe159.rekordbot.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirtableConfigurationValidatorTest {
    private val validConfiguration = AirtableConfiguration(
        personalAccessToken = "pat-test",
        baseId = "appTest",
        table = "Morceaux",
    )

    @Test
    fun `complete local configuration has no error`() {
        assertTrue(AirtableConfigurationValidator.localErrors(validConfiguration).isEmpty())
    }

    @Test
    fun `missing access values return understandable errors`() {
        val errors = AirtableConfigurationValidator.localErrors(AirtableConfiguration(table = ""))

        assertTrue(errors.contains("Le token Airtable est obligatoire."))
        assertTrue(errors.contains("Le Base ID est obligatoire."))
        assertTrue(errors.contains("La table est obligatoire."))
    }

    @Test
    fun `configured optional fields are checked against table schema`() {
        val schema = AirtableTableSchema(
            id = "tblTest",
            name = "Morceaux",
            fields = validConfiguration.fields.configuredNames()
                .filterNot { it == "Genre brut" }
                .mapIndexed { index, name ->
                    AirtableFieldSchema(id = "fld$index", name = name, type = "singleLineText")
                },
        )

        val errors = AirtableConfigurationValidator.schemaErrors(validConfiguration, schema)

        assertEquals(listOf("Champ « Genre brut » introuvable dans la table Morceaux."), errors)
    }

    @Test
    fun `blank optional field is ignored during schema validation`() {
        val configuration = validConfiguration.copy(
            fields = validConfiguration.fields.copy(rawGenre = ""),
        )
        val schema = AirtableTableSchema(
            id = "tblTest",
            name = "Morceaux",
            fields = configuration.fields.configuredNames().mapIndexed { index, name ->
                AirtableFieldSchema(id = "fld$index", name = name, type = "singleLineText")
            },
        )

        assertTrue(AirtableConfigurationValidator.schemaErrors(configuration, schema).isEmpty())
    }
}
