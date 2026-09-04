package com.inverter.monitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * İnverterin fiziksel LCD ekranındaki yarım daire göstergelere benzer,
 * arka plan izi + renkli ilerleme yayı çizen basit bir gauge view.
 *
 * Kullanım (XML): <com.inverter.monitor.GaugeView android:id="@+id/gaugePv" .../>
 * Kod tarafında: gaugePv.setValue(currentValue, maxValue, progressColor)
 */
class GaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#2a2a28")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#3ecf3e")
    }

    private val arcRect = RectF()
    private var fraction = 0f // 0f..1f

    fun setValue(value: Float, max: Float, color: Int) {
        fraction = (value / max).coerceIn(0f, 1f)
        progressPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val strokeHalf = trackPaint.strokeWidth / 2f
        arcRect.set(strokeHalf, strokeHalf, width - strokeHalf, width - strokeHalf)

        // Yarım daire: 180°'den başlayıp 180° tarıyor (soldan sağa alt yarım daire)
        canvas.drawArc(arcRect, 180f, 180f, false, trackPaint)
        canvas.drawArc(arcRect, 180f, 180f * fraction, false, progressPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * 0.6f).toInt() // yarım daire oranı
        setMeasuredDimension(width, height)
    }
}
