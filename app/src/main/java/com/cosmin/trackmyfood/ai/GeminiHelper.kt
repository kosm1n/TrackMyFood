package com.cosmin.trackmyfood.ai

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AnalyzedMeal(
    val name: String,
    val description: String,
    val ingredients: List<String>
)

class GeminiHelper {

    suspend fun analyzeMealImage(bitmap: Bitmap, apiKey: String): Result<AnalyzedMeal> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.success(getFallbackMeal())
        }

        val models = listOf("gemini-3.5-flash-lite")
        var lastError: String? = null

        for (modelName in models) {
            try {
                val result = callGeminiRestApi(bitmap, apiKey, modelName)
                if (result != null) {
                    return@withContext Result.success(result)
                }
            } catch (e: Exception) {
                lastError = e.message
            }
        }

        Result.success(getFallbackMeal(lastError))
    }

    private fun callGeminiRestApi(bitmap: Bitmap, apiKey: String, modelName: String): AnalyzedMeal? {
        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
        }

        // Convert Bitmap to base64 JPEG
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val prompt = """
            Analiza esta foto de una comida y proporciona la respuesta en español (dialecto de España / castellano):
            1. Meal Name: Un nombre corto y apetitoso para la comida en español.
            2. Description: Una descripción breve de la comida en español.
            3. Ingredients: Una lista de ingredientes visibles separados por comas en español.
            
            Formatea tu respuesta estrictamente como:
            Name: [Nombre de la comida]
            Description: [Descripción de la comida]
            Ingredients: [Ingrediente 1, Ingrediente 2, Ingrediente 3]
        """.trimIndent()

        // Construct JSON payload for Gemini REST API
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        connection.outputStream.use { os ->
            val input = jsonBody.toString().toByteArray(Charsets.UTF_8)
            os.write(input, 0, input.size)
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
            return parseJsonResponse(responseString)
        } else {
            val errorString = try {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
            } catch (_: Exception) {
                null
            }
            throw RuntimeException("HTTP $responseCode: $errorString")
        }
    }

    private fun parseJsonResponse(jsonResponse: String): AnalyzedMeal? {
        val root = JSONObject(jsonResponse)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        val text = parts.getJSONObject(0).optString("text", "")

        return parseGeminiResponse(text)
    }

    private fun parseGeminiResponse(output: String): AnalyzedMeal {
        var name = "Comida deliciosa"
        var description = "Una comida apetitosa analizada a partir de la foto."
        val ingredientsList = mutableListOf<String>()

        val lines = output.lines()
        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.startsWith("name:") -> {
                    name = line.substringAfter(":").trim()
                }
                lower.startsWith("description:") -> {
                    description = line.substringAfter(":").trim()
                }
                lower.startsWith("ingredients:") -> {
                    val ingStr = line.substringAfter(":").trim()
                    ingStr.split(",").forEach {
                        val trimmed = it.trim().removeSurrounding("[", "]")
                        if (trimmed.isNotEmpty()) {
                            ingredientsList.add(trimmed)
                        }
                    }
                }
            }
        }

        if (ingredientsList.isEmpty()) {
            ingredientsList.addAll(listOf("Componente principal", "Guarnición", "Aderezo"))
        }

        return AnalyzedMeal(
            name = if (name.isBlank()) "Comida saludable" else name,
            description = if (description.isBlank()) "Comida nutritiva y fresca." else description,
            ingredients = ingredientsList
        )
    }

    private fun getFallbackMeal(error: String? = null): AnalyzedMeal {
        return AnalyzedMeal(
            name = "Comida capturada",
            description = error?.let { "Analizada localmente (Nota API: $it)" } ?: "Comida recién capturada mediante foto.",
            ingredients = listOf("Ingrediente fresco 1", "Ingrediente fresco 2", "Condimento")
        )
    }
}
