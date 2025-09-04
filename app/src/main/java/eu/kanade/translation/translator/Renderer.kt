package eu.kanade.tachiyomi.translate

import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.util.*

object Renderer {

    fun renderArabicBubble(
        src: Bitmap,
        bubbleRect: Rect,
        text: String,
        context: Context,
        fontPath: String = "fonts/NotoNaskhArabic-Regular.ttf"
    ): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val padding = 8
        val erasePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL; color = Color.WHITE }
        val eraseRect = RectF(
            bubbleRect.left.toFloat() - padding,
            bubbleRect.top.toFloat() - padding,
            bubbleRect.right.toFloat() + padding,
            bubbleRect.bottom.toFloat() + padding
        )
        canvas.drawRoundRect(eraseRect, 12f, 12f, erasePaint)

        val typeface = Typeface.createFromAsset(context.assets, fontPath)
        val paint = TextPaint().apply {
            isAntiAlias = true
            textSize = 28f * (src.width / 1080f)
            textAlign = Paint.Align.RIGHT
            this.typeface = typeface
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textLocale = Locale("ar")
            }
        }

        val shaped = TranslationService.shapeArabic(text)

        val alignment = Layout.Alignment.ALIGN_OPPOSITE
        val width = (eraseRect.width() - padding * 2).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(shaped, 0, shaped.length, paint, width)
                .setAlignment(alignment)
                .setIncludePad(false)
                .setLineSpacing(0f, 1f)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(shaped, paint, width, alignment, 1.0f, 0.0f, false)
        }

        val x = eraseRect.right - padding
        val y = eraseRect.top + padding
        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()

        return out
    }
}
