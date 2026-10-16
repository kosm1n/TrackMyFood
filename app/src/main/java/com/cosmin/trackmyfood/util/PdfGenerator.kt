package com.cosmin.trackmyfood.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import com.cosmin.trackmyfood.data.model.Meal
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateMealsPdf(context: Context, meals: List<Meal>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val titlePaint = TextPaint().apply {
            color = Color.rgb(33, 150, 243)
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 40f
        val x = 40f
        val pageWidth = 515 // 595 - 80 margins

        // Title
        canvas.drawText("TrackMyFood - Informe de Comidas", x, y, titlePaint)
        y += 25f

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        canvas.drawText("Generado el: ${dateFormatter.format(Date())}", x, y, textPaint)
        y += 30f

        if (meals.isEmpty()) {
            canvas.drawText("No hay comidas registradas todavía.", x, y, textPaint)
        } else {
            for ((index, meal) in meals.withIndex()) {
                if (y > 700f) break // Stop if page is full

                val dateStr = dateFormatter.format(Date(meal.createdAt))
                canvas.drawText("${index + 1}. ${meal.name} ($dateStr)", x, y, headerPaint)
                y += 20f

                // Draw image with software config, correct EXIF rotation, and aspect ratio
                if (!meal.imagePath.isNullOrBlank()) {
                    try {
                        val file = File(meal.imagePath)
                        if (file.exists()) {
                            // 1. Decodificar con opciones optimizadas para evitar consumo excesivo de memoria
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = false
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            var bmp = BitmapFactory.decodeFile(file.absolutePath, options)

                            if (bmp != null) {
                                // 2. Corregir la orientación EXIF
                                bmp = correctBitmapOrientation(file.absolutePath, bmp)

                                val originalWidth = bmp.width.toFloat()
                                val originalHeight = bmp.height.toFloat()

                                // Dimensiones máximas permitidas en el PDF
                                val maxWidth = 160f
                                val maxHeight = 120f

                                var targetWidth = maxWidth
                                var targetHeight = (originalHeight * maxWidth) / originalWidth
                                if (targetHeight > maxHeight) {
                                    targetHeight = maxHeight
                                    targetWidth = (originalWidth * maxHeight) / originalHeight
                                }

                                // 3. Usar Matrix y Paint con filtro para mantener la máxima nitidez vectorial en el PDF
                                val matrix = Matrix().apply {
                                    val scaleX = targetWidth / originalWidth
                                    val scaleY = targetHeight / originalHeight
                                    postScale(scaleX, scaleY)
                                    postTranslate(x, y)
                                }

                                val paint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    isFilterBitmap = true // Clave para que no se vea pixelada
                                    isDither = true
                                }

                                canvas.drawBitmap(bmp, matrix, paint)

                                // Actualizar la coordenada Y para los textos siguientes
                                y += targetHeight + 15f
                            }
                        }
                    } catch (e: Exception) {
                        // Opcional: imprime el error en Logcat si necesitas depurar
                        e.printStackTrace()
                    }
                }

                // Description with text wrapping
                val descText = "Descripción: ${meal.description}"
                y = drawWrappedText(canvas, descText, x, y, pageWidth, textPaint)
                y += 5f

                // Ingredients with text wrapping
                val ingNames = meal.ingredients.joinToString(", ") { it.name }
                val ingText = "Ingredientes: ${if (ingNames.isNotBlank()) ingNames else "Ninguno especificado"}"
                y = drawWrappedText(canvas, ingText, x, y, pageWidth, textPaint)
                y += 20f
            }
        }

        pdfDocument.finishPage(page)

        val fileName = "TrackMyFood_Comidas_${System.currentTimeMillis()}.pdf"

        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                Toast.makeText(context, "PDF guardado en Descargas: $fileName", Toast.LENGTH_LONG).show()
            } else {
                pdfDocument.close()
                Toast.makeText(context, "Error al guardar en Descargas", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            pdfDocument.close()
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun correctBitmapOrientation(imagePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, width: Int, paint: TextPaint): Float {
        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()
        return y + staticLayout.height + 5f
    }
}
