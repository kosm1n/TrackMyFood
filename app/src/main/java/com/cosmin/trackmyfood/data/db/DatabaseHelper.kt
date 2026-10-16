package com.cosmin.trackmyfood.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.cosmin.trackmyfood.data.model.Ingredient
import com.cosmin.trackmyfood.data.model.Meal

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "track_my_food.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_MEALS = "meals"
        const val COLUMN_MEAL_ID = "id"
        const val COLUMN_MEAL_NAME = "name"
        const val COLUMN_MEAL_DESC = "description"
        const val COLUMN_MEAL_CREATED_AT = "created_at"
        const val COLUMN_MEAL_IMAGE_PATH = "image_path"

        const val TABLE_INGREDIENTS = "ingredients"
        const val COLUMN_ING_ID = "id"
        const val COLUMN_ING_NAME = "name"

        const val TABLE_MEAL_INGREDIENTS = "meal_ingredients"
        const val COLUMN_MI_MEAL_ID = "meal_id"
        const val COLUMN_MI_ING_ID = "ingredient_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createMealsTable = """
            CREATE TABLE $TABLE_MEALS (
                $COLUMN_MEAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_MEAL_NAME TEXT NOT NULL,
                $COLUMN_MEAL_DESC TEXT NOT NULL,
                $COLUMN_MEAL_CREATED_AT INTEGER NOT NULL,
                $COLUMN_MEAL_IMAGE_PATH TEXT
            )
        """.trimIndent()

        val createIngredientsTable = """
            CREATE TABLE $TABLE_INGREDIENTS (
                $COLUMN_ING_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ING_NAME TEXT NOT NULL UNIQUE
            )
        """.trimIndent()

        val createMealIngredientsTable = """
            CREATE TABLE $TABLE_MEAL_INGREDIENTS (
                $COLUMN_MI_MEAL_ID INTEGER NOT NULL,
                $COLUMN_MI_ING_ID INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_MI_MEAL_ID) REFERENCES $TABLE_MEALS($COLUMN_MEAL_ID) ON DELETE CASCADE,
                FOREIGN KEY($COLUMN_MI_ING_ID) REFERENCES $TABLE_INGREDIENTS($COLUMN_ING_ID) ON DELETE CASCADE,
                PRIMARY KEY($COLUMN_MI_MEAL_ID, $COLUMN_MI_ING_ID)
            )
        """.trimIndent()

        db.execSQL(createMealsTable)
        db.execSQL(createIngredientsTable)
        db.execSQL(createMealIngredientsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEAL_INGREDIENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_INGREDIENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEALS")
        onCreate(db)
    }

    fun insertMeal(meal: Meal): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val mealValues = ContentValues().apply {
                put(COLUMN_MEAL_NAME, meal.name)
                put(COLUMN_MEAL_DESC, meal.description)
                put(COLUMN_MEAL_CREATED_AT, meal.createdAt)
                put(COLUMN_MEAL_IMAGE_PATH, meal.imagePath)
            }
            val mealId = db.insert(TABLE_MEALS, null, mealValues)

            if (mealId != -1L) {
                for (ing in meal.ingredients) {
                    val ingId = getOrInsertIngredient(db, ing.name)
                    val miValues = ContentValues().apply {
                        put(COLUMN_MI_MEAL_ID, mealId)
                        put(COLUMN_MI_ING_ID, ingId)
                    }
                    db.insert(TABLE_MEAL_INGREDIENTS, null, miValues)
                }
            }
            db.setTransactionSuccessful()
            return mealId
        } finally {
            db.endTransaction()
        }
    }

    private fun getOrInsertIngredient(db: SQLiteDatabase, name: String): Long {
        val trimmed = name.trim()
        val cursor = db.query(
            TABLE_INGREDIENTS,
            arrayOf(COLUMN_ING_ID),
            "$COLUMN_ING_NAME = ?",
            arrayOf(trimmed),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(0)
            cursor.close()
            return id
        }
        cursor.close()

        val values = ContentValues().apply {
            put(COLUMN_ING_NAME, trimmed)
        }
        return db.insert(TABLE_INGREDIENTS, null, values)
    }

    fun getAllMeals(): List<Meal> {
        val meals = mutableListOf<Meal>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEALS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_MEAL_CREATED_AT DESC"
        )

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MEAL_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_NAME))
            val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_DESC))
            val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_MEAL_CREATED_AT))
            val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEAL_IMAGE_PATH))

            val ingredients = getIngredientsForMeal(db, id)
            meals.add(Meal(id, name, description, createdAt, imagePath, ingredients))
        }
        cursor.close()
        return meals
    }

    private fun getIngredientsForMeal(db: SQLiteDatabase, mealId: Long): List<Ingredient> {
        val ingredients = mutableListOf<Ingredient>()
        val query = """
            SELECT i.$COLUMN_ING_ID, i.$COLUMN_ING_NAME
            FROM $TABLE_INGREDIENTS i
            JOIN $TABLE_MEAL_INGREDIENTS mi ON i.$COLUMN_ING_ID = mi.$COLUMN_MI_ING_ID
            WHERE mi.$COLUMN_MI_MEAL_ID = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(mealId.toString()))
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val name = cursor.getString(1)
            ingredients.add(Ingredient(id, name))
        }
        cursor.close()
        return ingredients
    }

    fun deleteMeal(mealId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_MEAL_INGREDIENTS, "$COLUMN_MI_MEAL_ID = ?", arrayOf(mealId.toString()))
            db.delete(TABLE_MEALS, "$COLUMN_MEAL_ID = ?", arrayOf(mealId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllIngredientsAlphabetical(): List<Ingredient> {
        val ingredients = mutableListOf<Ingredient>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_INGREDIENTS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ING_NAME ASC"
        )
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ING_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ING_NAME))
            ingredients.add(Ingredient(id, name))
        }
        cursor.close()
        return ingredients
    }
}
