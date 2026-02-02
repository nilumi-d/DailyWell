package com.example.wellnesmate.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.wellnesmate.R
import kotlin.math.min

class WaterBottleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bottleOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent_blue)
        style = Paint.Style.FILL
    }

    private val extraWaterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary_green)
        style = Paint.Style.FILL
    }

    private val goalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private var progress = 0f // 0 to 1f (can exceed 1 for extra water)
    private var animator: ValueAnimator? = null
    private var goalAmount = 2000 // Default goal in ml
    private var currentAmount = 0 // Current intake in ml

    fun setProgress(progress: Float, currentMl: Int, goalMl: Int, animate: Boolean = true) {
        this.currentAmount = currentMl
        this.goalAmount = goalMl
        val newProgress = progress.coerceIn(0f, 1.5f) // Allow up to 150% for extra water
        
        if (animate) {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(this.progress, newProgress).apply {
                duration = 800
                addUpdateListener { animation ->
                    this@WaterBottleView.progress = animation.animatedValue as Float
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
        
        // Bottle dimensions
        val bottleTop = height * 0.15f
        val bottleBottom = height * 0.85f
        val bottleHeight = bottleBottom - bottleTop
        val bottleWidth = width * 0.7f
        val bottleLeft = (width - bottleWidth) / 2
        val bottleRight = bottleLeft + bottleWidth
        
        // Draw bottle cap
        val capRect = RectF(
            bottleLeft + bottleWidth * 0.3f,
            height * 0.05f,
            bottleRight - bottleWidth * 0.3f,
            bottleTop
        )
        canvas.drawRoundRect(capRect, 8f, 8f, bottleOutlinePaint)
        
        // Draw bottle outline
        val bottlePath = Path().apply {
            moveTo(bottleLeft, bottleTop)
            lineTo(bottleLeft, bottleBottom - 20f)
            quadTo(bottleLeft, bottleBottom, bottleLeft + 20f, bottleBottom)
            lineTo(bottleRight - 20f, bottleBottom)
            quadTo(bottleRight, bottleBottom, bottleRight, bottleBottom - 20f)
            lineTo(bottleRight, bottleTop)
        }
        canvas.drawPath(bottlePath, bottleOutlinePaint)
        
        // Calculate water levels
        val goalLineY = bottleBottom - (bottleHeight * 1.0f) // Goal is at 100% of bottle
        val waterHeight = bottleHeight * min(progress, 1.0f)
        val waterTop = bottleBottom - waterHeight
        
        // Draw blue water (up to goal)
        if (progress > 0f) {
            val blueWaterPath = Path().apply {
                moveTo(bottleLeft + 3f, bottleBottom - 3f)
                lineTo(bottleLeft + 3f, waterTop)
                
                // Add wave effect at top
                val wavePoints = 20
                for (i in 0..wavePoints) {
                    val x = bottleLeft + 3f + (bottleWidth - 6f) * (i.toFloat() / wavePoints)
                    val waveOffset = kotlin.math.sin(i * 0.5).toFloat() * 3f
                    val y = waterTop + waveOffset
                    lineTo(x, y)
                }
                
                lineTo(bottleRight - 3f, bottleBottom - 3f)
                close()
            }
            canvas.drawPath(blueWaterPath, waterPaint)
        }
        
        // Draw green water (extra above goal)
        if (progress > 1.0f) {
            val extraProgress = progress - 1.0f
            val extraHeight = bottleHeight * extraProgress
            val extraWaterTop = goalLineY - extraHeight
            
            val greenWaterPath = Path().apply {
                moveTo(bottleLeft + 3f, goalLineY)
                lineTo(bottleLeft + 3f, extraWaterTop)
                
                // Add wave effect at top
                val wavePoints = 20
                for (i in 0..wavePoints) {
                    val x = bottleLeft + 3f + (bottleWidth - 6f) * (i.toFloat() / wavePoints)
                    val waveOffset = kotlin.math.sin(i * 0.5).toFloat() * 3f
                    val y = extraWaterTop + waveOffset
                    lineTo(x, y)
                }
                
                lineTo(bottleRight - 3f, goalLineY)
                close()
            }
            canvas.drawPath(greenWaterPath, extraWaterPaint)
        }
        
        // Draw goal line
        canvas.drawLine(bottleLeft - 10f, goalLineY, bottleRight + 10f, goalLineY, goalLinePaint)
        
        // Draw goal label
        val goalTextPaint = Paint(textPaint).apply {
            textSize = 24f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("Goal: ${goalAmount}ml", bottleRight + 15f, goalLineY + 8f, goalTextPaint)
        
        // Draw current amount at top
        val currentTextPaint = Paint(textPaint).apply {
            textSize = 32f
            color = if (progress > 1.0f) ContextCompat.getColor(context, R.color.primary_green) else Color.BLACK
        }
        canvas.drawText("${currentAmount}ml", width / 2, height * 0.95f, currentTextPaint)
        
        // Draw extra amount if over goal
        if (progress > 1.0f) {
            val extraAmount = currentAmount - goalAmount
            val extraTextPaint = Paint(textPaint).apply {
                textSize = 20f
                color = ContextCompat.getColor(context, R.color.primary_green)
            }
            canvas.drawText("+${extraAmount}ml", width / 2, bottleTop - 10f, extraTextPaint)
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * 2.0f).toInt() // Taller aspect ratio for bottle
        setMeasuredDimension(width, height)
    }
}
