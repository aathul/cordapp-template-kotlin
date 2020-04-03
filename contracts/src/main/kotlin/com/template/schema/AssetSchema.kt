package com.template.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for AssetState.
 */
object AssetSchema

/**
 * An AssetState schema.
 */
object AssetSchemaV1 : MappedSchema(
        schemaFamily = AssetSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentAsset::class.java)) {
    @Entity
    @Table(name = "asset_states")
    class PersistentAsset(
            @Column(name = "currentOwner")
            var currentOwner: String,

            @Column(name = "newOwner")
            var newOwner: String,

            @Column(name = "transferValue")
            var transferValue: Int,

            @Column(name = "linear_id")
            var linearId: UUID


    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", 0,UUID.randomUUID())
    }
}