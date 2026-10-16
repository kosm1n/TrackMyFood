package com.cosmin.trackmyfood.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.cosmin.trackmyfood.data.model.Meal
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelGenerator {

    fun generateMealsExcel(context: Context, meals: List<Meal>) {
        val csvBuilder = StringBuilder()
        // CSV Header including image filename column
        csvBuilder.append("ID,Nombre,Fecha,Descripcion,Ingredientes,Imagen\n")

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val resolver = context.contentResolver

        for (meal in meals) {
            val dateStr = dateFormatter.format(Date(meal.createdAt))
            val ingList = meal.ingredients.joinToString(" - ") { it.name }
            
            var imageName = "Sin foto"
            if (!meal.imagePath.isNullOrBlank()) {
                val srcFile = File(meal.imagePath)
                if (srcFile.exists()) {
                    imageName = srcFile.name
                    // Export image file to shared Downloads folder as well
                    copyImageToDownloads(resolver, srcFile, imageName)
                }
            }

            // Escape quotes for CSV format
            val name = "\"${meal.name.replace("\"", "\"\"")}\""
            val desc = "\"${meal.description.replace("\"", "\"\"")}\""
            val ingredients = "\"${ingList.replace("\"", "\"\"")}\""
            val imgCol = "\"$imageName\""

            csvBuilder.append("${meal.id},$name,$dateStr,$desc,$ingredients,$imgCol\n")
        }

        val csvFileName = "TrackMyFood_Comidas_${System.currentTimeMillis()}.csv"

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, csvFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Excel (CSV) y fotos guardados en Descargas", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Error al guardar archivo Excel", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al generar Excel: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyImageToDownloads(resolver: android.content.ContentResolver, srcFile: File, fileName: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TrackMyFood_Fotos")
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(srcFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } catch (_: Exception) {
            // ignore individual image export failures
        }
    }
}
