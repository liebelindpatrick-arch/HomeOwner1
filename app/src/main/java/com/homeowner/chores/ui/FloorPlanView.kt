package com.homeowner.chores.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import com.homeowner.chores.data.Room
import kotlin.math.abs

data class RoomDisplay(
    val room: Room,
    val taskCount: Int,
    val overdueCount: Int,
    val dueTodayCount: Int
)

class FloorPlanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var isEditMode: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var onRoomClick: ((Room) -> Unit)? = null
    var onRoomPositionChanged: ((Room, Float, Float) -> Unit)? = null

    private var rooms: List<RoomDisplay> = emptyList()

    private val bgPaint = Paint().apply {
        color = Color.parseColor("#F5F5F5")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(3f)
        isAntiAlias = true
    }

    private val overlayPaint = Paint().apply {
        color = Color.argb(30, 255, 255, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
        textSize = spToPx(14f)
    }

    private val subTextPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        textSize = spToPx(11f)
    }

    private val badgePaint = Paint().apply {
        color = Color.parseColor("#D32F2F")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val badgeTextPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        textSize = spToPx(11f)
        isFakeBoldText = true
    }

    private val cornerRadius = dpToPx(16f)
    private val badgeRadius = dpToPx(14f)

    // Touch/drag state
    private var dragRoom: RoomDisplay? = null
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var dragCurrentRelX = 0f
    private var dragCurrentRelY = 0f
    private val dragThreshold = dpToPx(10f)
    private var hasDragged = false

    fun setRooms(list: List<RoomDisplay>) {
        rooms = list
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Grid lines every 10%
        for (i in 1..9) {
            val x = w * i / 10f
            val y = h * i / 10f
            canvas.drawLine(x, 0f, x, h, gridPaint)
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Draw rooms
        for (rd in rooms) {
            val room = rd.room
            if (!room.isOnPlan) continue

            val rx = if (dragRoom?.room?.id == room.id) dragCurrentRelX else room.planX
            val ry = if (dragRoom?.room?.id == room.id) dragCurrentRelY else room.planY

            val left = rx * w
            val top = ry * h
            val right = left + room.planW * w
            val bottom = top + room.planH * h
            val rect = RectF(left, top, right, bottom)

            // Fill (50% alpha of room color)
            val baseColor = try { Color.parseColor(room.colorHex) } catch (e: Exception) { Color.parseColor("#4A90D9") }
            fillPaint.color = Color.argb(128, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)

            // Stroke (100% alpha)
            strokePaint.color = baseColor
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, strokePaint)

            // Edit mode overlay
            if (isEditMode) {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, overlayPaint)
            }

            // Room name centered
            val centerX = (left + right) / 2f
            val centerY = (top + bottom) / 2f
            val nameY = centerY - spToPx(7f)
            canvas.drawText(room.name, centerX, nameY, textPaint)

            // Chore count below name
            val countText = "${rd.taskCount} pligter"
            canvas.drawText(countText, centerX, nameY + spToPx(18f), subTextPaint)

            // Overdue badge in top-right corner
            if (rd.overdueCount > 0) {
                val badgeCx = right - badgeRadius * 0.5f
                val badgeCy = top + badgeRadius * 0.5f
                canvas.drawCircle(badgeCx, badgeCy, badgeRadius, badgePaint)
                canvas.drawText(
                    rd.overdueCount.toString(),
                    badgeCx,
                    badgeCy + spToPx(4f),
                    badgeTextPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragStartX = event.x
                dragStartY = event.y
                hasDragged = false

                // Find which room was touched
                dragRoom = rooms.lastOrNull { rd ->
                    if (!rd.room.isOnPlan) return@lastOrNull false
                    val rx = rd.room.planX
                    val ry = rd.room.planY
                    val left = rx * w
                    val top = ry * h
                    val right = left + rd.room.planW * w
                    val bottom = top + rd.room.planH * h
                    event.x in left..right && event.y in top..bottom
                }

                dragRoom?.let { rd ->
                    dragCurrentRelX = rd.room.planX
                    dragCurrentRelY = rd.room.planY
                    dragOffsetX = event.x - rd.room.planX * w
                    dragOffsetY = event.y - rd.room.planY * h
                }

                return dragRoom != null
            }

            MotionEvent.ACTION_MOVE -> {
                val dr = dragRoom ?: return false
                if (isEditMode) {
                    val dx = abs(event.x - dragStartX)
                    val dy = abs(event.y - dragStartY)
                    if (dx > dragThreshold || dy > dragThreshold) {
                        hasDragged = true
                    }
                    if (hasDragged) {
                        val newX = ((event.x - dragOffsetX) / w).coerceIn(0f, 1f - dr.room.planW)
                        val newY = ((event.y - dragOffsetY) / h).coerceIn(0f, 1f - dr.room.planH)
                        dragCurrentRelX = newX
                        dragCurrentRelY = newY
                        invalidate()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dr = dragRoom ?: return false
                if (isEditMode && hasDragged) {
                    onRoomPositionChanged?.invoke(dr.room, dragCurrentRelX, dragCurrentRelY)
                } else if (!hasDragged) {
                    onRoomClick?.invoke(dr.room)
                }
                dragRoom = null
                hasDragged = false
                return true
            }
        }
        return false
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
}
