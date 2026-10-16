package com.cosmin.trackmyfood.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cosmin.trackmyfood.ai.GeminiHelper
import com.cosmin.trackmyfood.data.db.DatabaseHelper
import com.cosmin.trackmyfood.data.model.Ingredient
import com.cosmin.trackmyfood.data.model.Meal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper = DatabaseHelper(application)
    private val geminiHelper = GeminiHelper()
    private val prefs = application.getSharedPreferences("track_my_food_prefs", Context.MODE_PRIVATE)

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    private val _allIngredients = MutableStateFlow<List<Ingredient>>(emptyList())
    val allIngredients: StateFlow<List<Ingredient>> = _allIngredients.asStateFlow()

    private val _apiKey = MutableStateFlow(
        prefs.getString("gemini_api_key", null) ?: ""
    )
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _draftMealName = MutableStateFlow("")
    val draftMealName: StateFlow<String> = _draftMealName.asStateFlow()

    private val _draftDescription = MutableStateFlow("")
    val draftDescription: StateFlow<String> = _draftDescription.asStateFlow()

    private val _draftIngredients = MutableStateFlow<List<String>>(emptyList())
    val draftIngredients: StateFlow<List<String>> = _draftIngredients.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _meals.value = dbHelper.getAllMeals()
            _allIngredients.value = dbHelper.getAllIngredientsAlphabetical()
        }
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun setCapturedBitmap(bitmap: Bitmap) {
        _currentBitmap.value = bitmap
    }

    fun analyzeCurrentBitmap() {
        val bitmap = _currentBitmap.value ?: return
        viewModelScope.launch {
            _isAnalyzing.value = true
            val result = geminiHelper.analyzeMealImage(bitmap, _apiKey.value)
            result.onSuccess { analyzed ->
                _draftMealName.value = analyzed.name
                _draftDescription.value = analyzed.description
                _draftIngredients.value = analyzed.ingredients
            }
            _isAnalyzing.value = false
        }
    }

    fun updateDraftName(name: String) {
        _draftMealName.value = name
    }

    fun updateDraftDescription(desc: String) {
        _draftDescription.value = desc
    }

    fun addIngredient(ing: String) {
        if (ing.isNotBlank()) {
            _draftIngredients.value = _draftIngredients.value + ing.trim()
        }
    }

    fun updateIngredient(index: Int, newName: String) {
        val list = _draftIngredients.value.toMutableList()
        if (index in list.indices) {
            list[index] = newName.trim()
            _draftIngredients.value = list
        }
    }

    fun removeIngredient(index: Int) {
        val list = _draftIngredients.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _draftIngredients.value = list
        }
    }

    fun saveCurrentMeal(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val bitmap = _currentBitmap.value
            var imagePath: String? = null
            if (bitmap != null) {
                imagePath = saveBitmapToFile(bitmap)
            }

            val ingredients = _draftIngredients.value.filter { it.isNotBlank() }.map { Ingredient(name = it) }
            val meal = Meal(
                name = _draftMealName.value.ifBlank { "Untitled Meal" },
                description = _draftDescription.value.ifBlank { "No description" },
                createdAt = System.currentTimeMillis(),
                imagePath = imagePath,
                ingredients = ingredients
            )

            dbHelper.insertMeal(meal)
            loadData()
            onSuccess()
        }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            dbHelper.deleteMeal(mealId)
            loadData()
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        return try {
            val context = getApplication<Application>()
            val directory = File(context.filesDir, "meal_images")
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, "meal_${UUID.randomUUID()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
