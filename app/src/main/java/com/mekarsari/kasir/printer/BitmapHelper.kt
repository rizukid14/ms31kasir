package com.mekarsari.kasir.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object BitmapHelper {
    fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        if (uriString.isEmpty()) {
            return getDefaultPrintLogo(context)
        }
        return try {
            val uri = Uri.parse(uriString)
            val decoded = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
            decoded.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            getDefaultPrintLogo(context)
        }
    }

    fun getDefaultPrintLogo(context: Context): Bitmap {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                com.mekarsari.kasir.R.drawable.logo_print
            )
            bitmap?.copy(Bitmap.Config.ARGB_8888, true) ?: getPlaceholderLogo()
        } catch (e: Exception) {
            getPlaceholderLogo()
        }
    }

    fun getPlaceholderLogo(): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background putih
        canvas.drawColor(Color.WHITE)
        
        // Paint untuk border luar
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRect(4f, 4f, size.toFloat() - 4f, size.toFloat() - 4f, borderPaint)
        
        // Border dalam
        val innerBorderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRect(8f, 8f, size.toFloat() - 8f, size.toFloat() - 8f, innerBorderPaint)
        
        // Text Paint
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val textY = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("RM.", size / 2f, textY - 14f, textPaint)
        
        val textPaintSmall = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("MEKAR SARI", size / 2f, textY + 14f, textPaintSmall)
        
        return bitmap
    }
}
