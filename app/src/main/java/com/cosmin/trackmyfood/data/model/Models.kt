package com.cosmin.trackmyfood.data.model

data class Ingredient(
    val id: Long = 0L,
    val name: String
)

data class Meal(
    val id: Long = 0L,
    val name: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val ingredients: List<Ingredient> = emptyList()
)
