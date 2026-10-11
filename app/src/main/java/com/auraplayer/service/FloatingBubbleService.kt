package com.auraplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.auraplayer.MainActivity
import com.auraplayer.util.AppLog

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isExpanded = false

    companion object {
        private const val TAG = "FloatingBubbleService"
        const val ACTION_START = "ACTION_START_BUBBLE"
        const val ACTION_STOP = "ACTION_STOP_BUBBLE"
        const val CHANNEL_ID = "dave_bubble_channel"
        var isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        val notif = createNotification()
        startForeground(9982, notif)
        setupFloatingView()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DaVE Burbuja Flotante",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DaVE Burbuja Activa")
            .setContentText("Reproductor flotante en pantalla")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        // Programmatic container layout for clean compatibility without XML inflation
        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Circular Bubble View
        val bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_media_play)
            setBackgroundColor(Color.parseColor("#1E1B4B"))
            setPadding(24, 24, 24, 24)
            layoutParams = FrameLayout.LayoutParams(140, 140)
        }

        // Expanded Panel View
        val expandedPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(20, 16, 20, 16)
            visibility = View.GONE
            gravity = Gravity.CENTER_VERTICAL

            val title = TextView(context).apply {
                text = "🎵 DaVE Player"
                setTextColor(Color.parseColor("#00F0FF"))
                textSize = 14f
                setPadding(10, 0, 20, 0)
                setOnClickListener {
                    val appIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(appIntent)
                }
            }

            val btnClose = TextView(context).apply {
                text = "✖"
                setTextColor(Color.parseColor("#EF4444"))
                textSize = 18f
                setPadding(15, 0, 10, 0)
                setOnClickListener {
                    stopSelf()
                }
            }

            addView(title)
            addView(btnClose)
        }

        container.addView(expandedPanel)
        container.addView(bubble)

        // Drag & Touch Logic
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(container, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        isExpanded = !isExpanded
                        expandedPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = container
        try {
            windowManager?.addView(floatingView, params)
        } catch (e: Exception) {
            AppLog.e(TAG, "No se pudo añadir la vista flotante al WindowManager", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (e: Exception) {
                AppLog.d(TAG, "No se pudo quitar la vista flotante al destruir", e)
            }
            floatingView = null
        }
    }
}
