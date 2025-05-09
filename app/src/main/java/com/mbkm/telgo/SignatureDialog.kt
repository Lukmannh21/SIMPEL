package com.mbkm.telgo

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast

class SignatureDialog(context: Context, private val onSignatureDone: (Bitmap) -> Unit) : Dialog(context) {

    private lateinit var signaturePad: SignatureView
    private lateinit var btnClear: Button
    private lateinit var btnDone: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_signature)

        signaturePad = findViewById(R.id.signatureView)
        btnClear = findViewById(R.id.btnClear)
        btnDone = findViewById(R.id.btnDone)

        btnClear.setOnClickListener {
            signaturePad.clear()
        }

        btnDone.setOnClickListener {
            if (signaturePad.isEmpty()) {
                Toast.makeText(context, "Please provide a signature", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bitmap = signaturePad.getBitmap()
            onSignatureDone(bitmap)
            dismiss()
        }
    }
}

class SignatureView(context: Context) : View(context) {

    private var path = Path()
    private var paint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8f
        isAntiAlias = true
    }

    private var paths = ArrayList<Path>()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDrawing = false

    // Track if signature is empty
    private var hasSignature = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all stored paths
        for (p in paths) {
            canvas.drawPath(p, paint)
        }

        // Draw current path
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                lastTouchX = x
                lastTouchY = y
                isDrawing = true
                hasSignature = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDrawing) return false

                // Calculate the distance between the current point and the last point
                val dx = Math.abs(x - lastTouchX)
                val dy = Math.abs(y - lastTouchY)

                // If the distance is significant enough, draw a curve to the new point
                if (dx >= 3 || dy >= 3) {
                    path.quadTo(lastTouchX, lastTouchY, (x + lastTouchX) / 2, (y + lastTouchY) / 2)
                    lastTouchX = x
                    lastTouchY = y
                }

                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDrawing) return false

                // Finish the path drawing and store it
                path.lineTo(x, y)

                // Store the path
                paths.add(path)

                // Reset the path for next drawing
                path = Path()
                isDrawing = false

                invalidate()
                return true
            }
        }

        return false
    }

    fun clear() {
        paths.clear()
        path = Path()
        hasSignature = false
        invalidate()
    }

    fun isEmpty(): Boolean {
        return !hasSignature
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw white background
        canvas.drawColor(Color.WHITE)

        // Draw all paths
        for (p in paths) {
            canvas.drawPath(p, paint)
        }

        // Draw current path
        canvas.drawPath(path, paint)

        return bitmap
    }
}