package com.example

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.min

interface ProgressDrawable {
    var progress: Float
}

class MinimalHourglassDrawable : Drawable(), ProgressDrawable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val path = Path()
    
    override var progress: Float = 0f
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0 || h <= 0) return
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        
        val hw = w * 0.35f
        val hh = h * 0.4f
        
        // Progress split:
        // 0.0 - 0.7: Fluid flows from top to bottom
        // 0.7 - 1.0: Hourglass rotates 180
        val flowProgress = (progress / 0.7f).coerceIn(0f, 1f)
        val rotProgress = ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f)
        
        canvas.save()
        canvas.rotate(rotProgress * 180f, cx, cy)
        
        // Outline path
        path.reset()
        path.moveTo(cx - hw, cy - hh)
        path.lineTo(cx + hw, cy - hh)
        path.lineTo(cx + hw * 0.2f, cy)
        path.lineTo(cx + hw, cy + hh)
        path.lineTo(cx - hw, cy + hh)
        path.lineTo(cx - hw * 0.2f, cy)
        path.close()
        
        canvas.drawPath(path, paint)
        
        // Top Fluid (shrinking from top - fluid stays at bottom of the top triangle)
        if (flowProgress < 1f) {
            val fluidTopY = cy - hh + hh * flowProgress
            val wOuter = hw * 0.2f + (hw * 0.8f) * ((cy - fluidTopY) / hh)
            path.reset()
            path.moveTo(cx - wOuter, fluidTopY)
            path.lineTo(cx + wOuter, fluidTopY)
            path.lineTo(cx + hw * 0.2f, cy)
            path.lineTo(cx - hw * 0.2f, cy)
            path.close()
            canvas.drawPath(path, fillPaint)
        }
        
        // Bottom Fluid (growing from bottom - fluid fills from bottom up)
        if (flowProgress > 0f) {
            val fluidBottomY = cy + hh - hh * flowProgress
            val wOuter = hw * 0.2f + (hw * 0.8f) * ((fluidBottomY - cy) / hh)
            path.reset()
            path.moveTo(cx - wOuter, fluidBottomY)
            path.lineTo(cx + wOuter, fluidBottomY)
            path.lineTo(cx + hw, cy + hh)
            path.lineTo(cx - hw, cy + hh)
            path.close()
            canvas.drawPath(path, fillPaint)
        }
        
        // Flowing center line
        if (flowProgress > 0f && flowProgress < 1f) {
            val fluidYBottom = cy + hh - hh * flowProgress
            canvas.drawLine(cx, cy, cx, fluidYBottom, fillPaint)
        }
        
        canvas.restore()
    }
    
    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        fillPaint.alpha = alpha
    }
    
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        fillPaint.colorFilter = colorFilter
    }
    
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

class MinimalStopwatchDrawable : Drawable(), ProgressDrawable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    val rect = RectF()
    
    override var progress: Float = 0f
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0 || h <= 0) return
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        
        val radius = min(w, h) * 0.35f
        val topButtonPad = radius * 0.15f
        
        val fillProgress = (progress / 0.8f).coerceIn(0f, 1f)
        val sweepAngle = 360f * fillProgress
        val isCleared = progress >= 0.9f
        
        canvas.drawCircle(cx, cy + topButtonPad, radius, paint)
        canvas.drawLine(cx, cy + topButtonPad - radius, cx, cy + topButtonPad - radius - radius * 0.3f, paint)
        canvas.drawLine(cx - radius * 0.25f, cy + topButtonPad - radius - radius * 0.3f, cx + radius * 0.25f, cy + topButtonPad - radius - radius * 0.3f, paint)
        
        if (!isCleared && sweepAngle > 0f) {
            rect.set(cx - radius * 0.7f, cy + topButtonPad - radius * 0.7f, cx + radius * 0.7f, cy + topButtonPad + radius * 0.7f)
            canvas.drawArc(rect, -90f, sweepAngle, true, fillPaint)
        }
    }
    
    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        fillPaint.alpha = alpha
    }
    
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        fillPaint.colorFilter = colorFilter
    }
    
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
