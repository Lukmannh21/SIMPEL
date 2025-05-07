package com.mbkm.telgo

import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class BASurveyBigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basurvey_big)

        val btnGeneratePdf = findViewById<Button>(R.id.btnGeneratePdf)
        btnGeneratePdf.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                generateStyledPdf()
            }
        }
    }

    private suspend fun generateStyledPdf() {
        withContext(Dispatchers.IO) {
            try {
                // Ambil input dari pengguna
                val projectTitle = findViewById<EditText>(R.id.inputProjectTitle).text.toString()
                val contractNumber = findViewById<EditText>(R.id.inputContractNumber).text.toString()
                val executor = findViewById<EditText>(R.id.inputExecutor).text.toString()
                val location = findViewById<EditText>(R.id.inputLocation).text.toString()
                val description = findViewById<EditText>(R.id.inputDescription).text.toString()
                val actual1 = findViewById<EditText>(R.id.inputAktual1).text.toString()
                val remark1 = findViewById<EditText>(R.id.inputKeterangan1).text.toString()
                val actual2 = findViewById<EditText>(R.id.inputAktual2).text.toString()
                val remark2 = findViewById<EditText>(R.id.inputKeterangan2).text.toString()

                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                val marginX = 50f
                val maxX = 545f

                // Paints
                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    textAlign = Paint.Align.LEFT
                }

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }

                val boldPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.LEFT
                }

                val cellPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    textAlign = Paint.Align.LEFT
                }

                val tablePaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                // Header
                val centerX = (marginX + maxX) / 2
                canvas.drawText("BERITA ACARA", centerX, 50f, titlePaint) // Baris pertama
                canvas.drawText("SURVEY LOKASI", centerX, 70f, titlePaint) // Baris kedua
                canvas.drawLine(marginX, 80f, maxX, 80f, paint)

                // Informasi Proyek
                var y = 100f
                val labelX = marginX
                val colonX = 180f
                val valueX = 200f
                val infoMaxWidth = maxX - valueX // Panjang maksimum untuk teks nilai

                fun drawInfo(label: String, value: String, isBold: Boolean = false) {
                    canvas.drawText(label, labelX, y, paint)
                    canvas.drawText(":", colonX, y, paint)
                    val valuePaint = if (isBold) boldPaint else paint
                    val lines = wrapText(value, infoMaxWidth, valuePaint)
                    for (line in lines) {
                        canvas.drawText(line, valueX, y, valuePaint)
                        y += 18f
                    }
                }

                drawInfo("Proyek", projectTitle, isBold = true)
                drawInfo("Nomor Kontrak KHS", contractNumber)
                drawInfo("Pelaksana", executor)
                drawInfo("Lokasi", location, isBold = true)

                // Garis pembatas di bawah lokasi
                canvas.drawLine(marginX, y + 5f, maxX, y + 5f, paint)
                y += 20f // Tambahkan jarak setelah garis pembatas

                // Deskripsi
                val descMaxWidth = maxX - marginX * 2
                val descLines = wrapText(description, descMaxWidth, paint)
                for (line in descLines) {
                    canvas.drawText(line, marginX, y, paint)
                    y += 18f
                }

                y += 10f // Jarak lebih kecil sebelum tabel

                // Tabel
                val rowHeight = 40f
                val colX = floatArrayOf(marginX, 90f, 250f, 360f, 430f, maxX)

                // Header tabel
                canvas.drawRect(colX[0], y, colX[5], y + rowHeight, tablePaint)
                canvas.drawText("NO", colX[0] + 15f, y + 25f, boldPaint)
                canvas.drawText("ITEM", colX[1] + 10f, y + 25f, boldPaint)
                canvas.drawText("SATUAN", colX[2] + 10f, y + 25f, boldPaint)
                canvas.drawText("AKTUAL", colX[3] + 10f, y + 25f, boldPaint)
                canvas.drawText("KETERANGAN", colX[4] + 10f, y + 25f, boldPaint)

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + rowHeight, tablePaint) // Garis vertikal untuk header
                }

                y += rowHeight

                // Baris 1
                val remarkMaxWidth1 = colX[5] - colX[4] - 10f
                val remarkLines1 = wrapText(remark1, remarkMaxWidth1, cellPaint)
                val dynamicRowHeight1 = maxOf(40f, 20f + (remarkLines1.size * 15f))

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight1, tablePaint)
                canvas.drawText("1", colX[0] + 15f, y + 20f, cellPaint)
                canvas.drawText("Propose OLT", colX[1] + 10f, y + 20f, cellPaint)
                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual1, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY1 = y + 18f
                for (line in remarkLines1) {
                    canvas.drawText(line, colX[4] + 5f, remarkY1, cellPaint)
                    remarkY1 += 15f
                }

                // Tambahkan garis vertikal untuk baris 1
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight1, tablePaint)
                }

                y += dynamicRowHeight1

                // Baris 2
                val remarkMaxWidth2 = colX[5] - colX[4] - 10f
                val remarkLines2 = wrapText(remark2, remarkMaxWidth2, cellPaint)
                val dynamicRowHeight2 = maxOf(40f, 20f + (remarkLines2.size * 15f))

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight2, tablePaint)
                canvas.drawText("2", colX[0] + 15f, y + 20f, cellPaint)
                canvas.drawText("Panjang Bundlecore Uplink", colX[1] + 10f, y + 20f, cellPaint)
                canvas.drawText("(Dari Metro ke FTM-Rack ET)", colX[1] + 10f, y + 35f, cellPaint)
                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual2, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY2 = y + 18f
                for (line in remarkLines2) {
                    canvas.drawText(line, colX[4] + 5f, remarkY2, cellPaint)
                    remarkY2 += 15f
                }

                // Tambahkan garis vertikal untuk baris 2
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight2, tablePaint)
                }

                y += dynamicRowHeight2

                document.finishPage(page)

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                val file = File(downloadsDir, "SurveyLokasi.pdf")
                document.writeTo(FileOutputStream(file))
                document.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BASurveyBigActivity, "PDF berhasil disimpan di: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BASurveyBigActivity, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}