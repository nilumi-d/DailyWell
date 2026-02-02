package com.example.wellnesmate.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.wellnesmate.R
import kotlin.math.min

class WaterDropletView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent_blue)
        style = Paint.Style.FILL
        strokeWidth = 5f
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 33, 150, 243)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var progress = 0f // 0 to 1f
    private val cornerRadius = 16f
    private var animator: ValueAnimator? = null

    fun setProgress(progress: Float, animate: Boolean = true) {
        val newProgress = progress.coerceIn(0f, 1f)
        
        if (animate) {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(this.progress, newProgress).apply {
                duration = 800
                addUpdateListener { animation ->
                    this@WaterDropletView.progress = animation.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            this.progress = newProgress
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Draw background
        val backgroundRect = RectF(0f, 0f, width, height)
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, outlinePaint)
        
        // Draw water level
        val waterLevel = height * (1 - progress)
        val waterRect = RectF(0f, waterLevel, width, height)
        
        // Create a path for the water with wave effect
        val wavePath = Path()
        wavePath.reset()
        
        // Start from bottom-left
        wavePath.moveTo(0f, height)
        
        // Draw bottom curve
        wavePath.cubicTo(
            width * 0.25f, height - 10f,
            width * 0.5f, height - 20f,
            width, height - 15f
        )
        
        // Draw right side
        wavePath.lineTo(width, waterLevel)
        
        // Draw top wave
        val waveHeight = 8f * (1 - progress) // Wave height reduces as water level rises
        for (i in width.toInt() downTo 0 step 10) {
            val x = i.toFloat()
            val y = waterLevel + sin(x * 0.05f) * waveHeight
            if (i == width.toInt()) {
                wavePath.moveTo(x, y)
            } else {
                wavePath.lineTo(x, y)
            }
        }
        
        // Close the path
        wavePath.lineTo(0f, waterLevel)
        wavePath.close()
        
        // Draw the water
        canvas.drawPath(wavePath, waterPaint)
        
        // Draw water surface highlight
        if (progress > 0.1f) {
            val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(50, 255, 255, 255)
                style = Paint.Style.FILL
            }
            
            val highlightRect = RectF(
                0f,
                waterLevel - 10f,
                width,
                waterLevel + 10f
            )
            canvas.drawRoundRect(highlightRect, cornerRadius, cornerRadius, highlightPaint)
        }
    }
    
    private fun sin(rad: Float): Float = kotlin.math.sin(rad.toDouble()).toFloat()
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * 1.5f).toInt() // Keep aspect ratio
        setMeasuredDimension(width, height)
    }
}
