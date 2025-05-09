package com.mbkm.telgo

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FirebaseFirestore

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



    private fun generateDescription(projectTitle: String, contractNumber: String, executor: String): String {
        val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
        return "Pada hari ini, $currentDate, telah dilakukan survey bersama terhadap pekerjaan \"$projectTitle\" " +
                "yang dilaksanakan oleh $executor yang terikat Perjanjian Pemborongan \"$contractNumber\" " +
                "dengan hasil sebagai berikut:"
    }

    private suspend fun generateStyledPdf() {
        withContext(Dispatchers.IO) {
            try {
                // Ambil input dari pengguna
                val projectTitle = findViewById<EditText>(R.id.inputProjectTitle).text.toString()
                val contractNumber = findViewById<EditText>(R.id.inputContractNumber).text.toString()
                val executor = findViewById<Spinner>(R.id.inputExecutor).selectedItem.toString()
                val location = findViewById<EditText>(R.id.inputLocation).text.toString()
                // Hasil deskripsi otomatis
                val description = generateDescription(projectTitle, contractNumber, executor)

                // Tampilkan deskripsi otomatis di field deskripsi
                findViewById<EditText>(R.id.inputDescription).setText(description)

                val actual1 = findViewById<EditText>(R.id.inputAktual1).text.toString()
                val remark1 = findViewById<EditText>(R.id.inputKeterangan1).text.toString()
                val actual2 = findViewById<EditText>(R.id.inputAktual2).text.toString()
                val remark2 = findViewById<EditText>(R.id.inputKeterangan2).text.toString()
                val actual3 = findViewById<EditText>(R.id.inputAktual3).text.toString()
                val remark3 = findViewById<EditText>(R.id.inputKeterangan3).text.toString()
                val actual4 = findViewById<EditText>(R.id.inputAktual4).text.toString()
                val remark4 = findViewById<EditText>(R.id.inputKeterangan4).text.toString()
                val actual5 = findViewById<EditText>(R.id.inputAktual5).text.toString()
                val remark5 = findViewById<EditText>(R.id.inputKeterangan5).text.toString()
                val actual6 = findViewById<EditText>(R.id.inputAktual6).text.toString()
                val remark6 = findViewById<EditText>(R.id.inputKeterangan6).text.toString()
                val actual7 = findViewById<EditText>(R.id.inputAktual7).text.toString()
                val remark7 = findViewById<EditText>(R.id.inputKeterangan7).text.toString()
                val actual8 = findViewById<EditText>(R.id.inputAktual8).text.toString()
                val remark8 = findViewById<EditText>(R.id.inputKeterangan8).text.toString()
                val actual9 = findViewById<EditText>(R.id.inputAktual9).text.toString()
                val remark9 = findViewById<EditText>(R.id.inputKeterangan9).text.toString()
                val actual10 = findViewById<EditText>(R.id.inputAktual10).text.toString()
                val remark10 = findViewById<EditText>(R.id.inputKeterangan10).text.toString()
                val actual11 = findViewById<EditText>(R.id.inputAktual11).text.toString()
                val remark11 = findViewById<EditText>(R.id.inputKeterangan11).text.toString()
                val actual12 = findViewById<EditText>(R.id.inputAktual12).text.toString()
                val remark12 = findViewById<EditText>(R.id.inputKeterangan12).text.toString()
                val actual13 = findViewById<EditText>(R.id.inputAktual13).text.toString()
                val remark13 = findViewById<EditText>(R.id.inputKeterangan13).text.toString()
                val actual14 = findViewById<EditText>(R.id.inputAktual14).text.toString()
                val remark14 = findViewById<EditText>(R.id.inputKeterangan14).text.toString()
                val actual15 = findViewById<EditText>(R.id.inputAktual15).text.toString()
                val remark15 = findViewById<EditText>(R.id.inputKeterangan15).text.toString()
                val actual16 = findViewById<EditText>(R.id.inputAktual16).text.toString()
                val remark16 = findViewById<EditText>(R.id.inputKeterangan16).text.toString()
                val actual17 = findViewById<EditText>(R.id.inputAktual17).text.toString()
                val remark17 = findViewById<EditText>(R.id.inputKeterangan17).text.toString()
                val actual18 = findViewById<EditText>(R.id.inputAktual18).text.toString()
                val remark18 = findViewById<EditText>(R.id.inputKeterangan18).text.toString()
                val actual19 = findViewById<EditText>(R.id.inputAktual19).text.toString()
                val remark19 = findViewById<EditText>(R.id.inputKeterangan19).text.toString()

                val document = PdfDocument()
                var pageCount = 1

                // Konstanta halaman
                val pageWidth = 595f
                val pageHeight = 842f
                val marginX = 50f
                val marginTop = 50f
                val marginBottom = 50f
                val maxX = pageWidth - marginX

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

                // Fungsi untuk membuat halaman baru
                fun createPage(): PdfDocument.Page {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), pageCount++).create()
                    return document.startPage(pageInfo)
                }

                var page = createPage()
                var canvas = page.canvas
                var y = marginTop

                // Header halaman
                // Header halaman dengan logo dinamis
                fun drawHeader(executor: String) {
                    val centerX = (marginX + maxX) / 2

                    // Tambahkan logo berdasarkan pelaksana
                    val zteLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_zte) // Logo ZTE
                    val huaweiLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_huawei) // Logo Huawei
                    val telkomLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_telkom) // Logo Telkom

                    // Ukuran logo
                    val logoWidth = 80 // Lebar logo
                    val logoHeight = 50 // Tinggi logo
                    val topMargin = marginTop // Margin atas untuk logo

                    // Gambar logo pelaksana di pojok kiri atas
                    when (executor) {
                        "PT. ZTE INDONESIA" -> {
                            val scaledZteLogo = Bitmap.createScaledBitmap(zteLogo, logoWidth, logoHeight, false)
                            canvas.drawBitmap(scaledZteLogo, marginX, topMargin, null)
                        }
                        "PT Huawei Tech Investment" -> {
                            val scaledHuaweiLogo = Bitmap.createScaledBitmap(huaweiLogo, logoWidth, logoHeight, false)
                            canvas.drawBitmap(scaledHuaweiLogo, marginX, topMargin, null)
                        }
                    }

                    // Gambar logo Telkom di pojok kanan atas
                    val scaledTelkomLogo = Bitmap.createScaledBitmap(telkomLogo, logoWidth, logoHeight, false)
                    canvas.drawBitmap(scaledTelkomLogo, maxX - logoWidth - marginX, topMargin, null)

                    // Tambahkan jarak di bawah logo
                    val logoBottomY = topMargin + logoHeight + 20f

                    // Teks header
                    canvas.drawText("BERITA ACARA", centerX, logoBottomY, titlePaint)
                    canvas.drawText("SURVEY LOKASI", centerX, logoBottomY + 20f, titlePaint)
                    canvas.drawLine(marginX, logoBottomY + 30f, maxX, logoBottomY + 30f, paint)
                    y = logoBottomY + 40f // Perbarui posisi vertikal
                }

                // Footer halaman
                fun drawFooter() {
                    canvas.drawText("Halaman ${pageCount - 1}", marginX, pageHeight - 20f, paint)

                }


                // Tambahkan teks di bawah tabel halaman terakhir
                fun drawClosingStatement() {
                    val closingText = "Demikian Berita Acara Hasil Survey ini dibuat berdasarkan kenyataan di lapangan untuk dijadikan pedoman pelaksanaan selanjutnya."
                    val closingMaxWidth = maxX - marginX * 2
                    val closingLines = wrapText(closingText, closingMaxWidth, paint)

                    // Format tanggal hari ini
                    val currentDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

                    // Tinggi minimum untuk menambahkan teks
                    val closingHeight = 18f * closingLines.size + 10f + 20f // Tambahkan ruang untuk tanggal
                    if (y + closingHeight > pageHeight - marginBottom) {
                        drawFooter()
                        document.finishPage(page)
                        page = createPage()
                        canvas = page.canvas
                        y = marginTop
                    }

                    y += 20f // Jarak 2 baris dari tabel terakhir
                    for (line in closingLines) {
                        canvas.drawText(line, marginX, y, paint)
                        y += 18f
                    }

                    // Tulis tanggal dengan bold dan align right
                    val boldPaint = Paint(paint).apply {
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(currentDate, maxX, y + 10f, boldPaint) // Posisi align right
                }


                // Tambahkan tanda tangan di halaman terakhir
                fun drawSignaturesWithFormattedTitles(canvas: Canvas, region: String, yStart: Float, paint: Paint, boldPaint: Paint) {
                    val marginX = 50f
                    val boxWidth = (595 - (marginX * 2)) / 3 // Lebar kotak tanda tangan
                    val signatureBoxHeight = 150f // Tinggi kotak tanda tangan
                    var y = yStart

                    // Paint untuk menggambar garis luar kotak (stroke)
                    val boxPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.STROKE // Hanya menggambar garis luar
                        strokeWidth = 2f
                    }

                    // Fungsi untuk menggambar teks nama perusahaan dengan format khusus (2 atau 3 baris)
                    fun drawFormattedTitle(canvas: Canvas, lines: List<String>, x: Float, y: Float, maxWidth: Float, boldPaint: Paint): Float {
                        val lineHeight = boldPaint.textSize + 4f // Tinggi setiap baris
                        var currentY = y

                        for (line in lines) {
                            canvas.drawText(line, x, currentY, boldPaint)
                            currentY += lineHeight
                        }

                        return currentY // Kembalikan posisi Y setelah teks terakhir
                    }

                    // Fungsi untuk menggambar kotak tanda tangan
                    fun drawSignatureBox(
                        lines: List<String>,
                        name: String,
                        nik: String,
                        signature: Drawable?,
                        x: Float,
                        y: Float
                    ) {
                        // Gambar kotak
                        val rect = RectF(x, y, x + boxWidth, y + signatureBoxHeight)
                        canvas.drawRect(rect, boxPaint)

                        // Tulis nama perusahaan dengan format 2 atau 3 baris
                        val titleY = drawFormattedTitle(canvas, lines, x + 10f, y + 20f, boxWidth - 20f, boldPaint)

                        // Gambar tanda tangan di tengah kotak
                        val signatureY = titleY + 10f
                        if (signature != null) {
                            val bitmap = (signature as BitmapDrawable).bitmap
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 50, false)
                            canvas.drawBitmap(scaledBitmap, x + (boxWidth / 2 - 50f), signatureY, null)
                        }

                        // Tulis nama dan NIK di bawah tanda tangan
                        val nameY = y + signatureBoxHeight - 40f // Posisi tetap sejajar
                        canvas.drawText("($name)", x + 10f, nameY, paint)
                        canvas.drawText("NIK: $nik", x + 10f, nameY + 20f, paint)
                    }

                    // Ambil input untuk tanda tangan
                    val zteName = findViewById<EditText>(R.id.etZteName).text.toString()
                    val zteNik = findViewById<EditText>(R.id.etZteNik).text.toString()
                    val zteSignature = findViewById<ImageView>(R.id.imgZteSignature).drawable

                    val tifName = findViewById<EditText>(R.id.etTifName).text.toString()
                    val tifNik = findViewById<EditText>(R.id.etTifNik).text.toString()
                    val tifSignature = findViewById<ImageView>(R.id.imgTifSignature).drawable

                    val telkomName = findViewById<EditText>(R.id.etTelkomName).text.toString()
                    val telkomNik = findViewById<EditText>(R.id.etTelkomNik).text.toString()
                    val telkomSignature = findViewById<ImageView>(R.id.imgTelkomSignature).drawable

                    val tselNopName = findViewById<EditText>(R.id.etTselNopName).text.toString()
                    val tselNopNik = findViewById<EditText>(R.id.etTselNopNik).text.toString()
                    val tselNopSignature = findViewById<ImageView>(R.id.imgTselNopSignature).drawable

                    val tselRtpdsName = findViewById<EditText>(R.id.etTselRtpdsName).text.toString()
                    val tselRtpdsNik = findViewById<EditText>(R.id.etTselRtpdsNik).text.toString()
                    val tselRtpdsSignature = findViewById<ImageView>(R.id.imgTselRtpdsSignature).drawable

                    val tselRtpeName = findViewById<EditText>(R.id.etTselRtpeNfName).text.toString()
                    val tselRtpeNik = findViewById<EditText>(R.id.etTselRtpeNfNik).text.toString()
                    val tselRtpeSignature = findViewById<ImageView>(R.id.imgTselRtpeNfSignature).drawable

                    // Baris pertama (ZTE, TIF, TELKOM)
                    drawSignatureBox(
                        listOf("PT. ZTE INDONESIA", "TIM SURVEY"),
                        zteName, zteNik, zteSignature, marginX, y
                    )
                    drawSignatureBox(
                        listOf("PT. TIF", "TIM SURVEY"),
                        tifName, tifNik, tifSignature, marginX + boxWidth, y
                    )
                    drawSignatureBox(
                        listOf("PT. TELKOM", "MGR NDPS TR1"),
                        telkomName, telkomNik, telkomSignature, marginX + (2 * boxWidth), y
                    )

                    // Baris kedua (NOP, RTPDS, RTPE)
                    y += signatureBoxHeight + 20f
                    drawSignatureBox(
                        listOf("PT. TELKOMSEL", "MGR NOP", region),
                        tselNopName, tselNopNik, tselNopSignature, marginX, y
                    )
                    drawSignatureBox(
                        listOf("PT. TELKOMSEL", "MGR RTPDS", region),
                        tselRtpdsName, tselRtpdsNik, tselRtpdsSignature, marginX + boxWidth, y
                    )
                    drawSignatureBox(
                        listOf("PT. TELKOMSEL", "MGR RTPE", region),
                        tselRtpeName, tselRtpeNik, tselRtpeSignature, marginX + (2 * boxWidth), y
                    )
                }



                drawHeader(executor)

// Informasi Proyek
                val labelX = marginX
                val colonX = 180f
                val valueX = 200f
                val infoMaxWidth = maxX - valueX

                fun drawInfo(label: String, value: String, isBold: Boolean = false) {
                    val valuePaint = if (isBold) boldPaint else paint
                    canvas.drawText(label, labelX, y, paint)
                    canvas.drawText(":", colonX, y, paint)
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

                y += 10f // Jarak sebelum tabel

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
                    canvas.drawLine(x, y, x, y + rowHeight, tablePaint)
                }
                y += rowHeight


                // Baris 1
                val itemMaxWidth1 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth1 = colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines1 = wrapText("Propose OLT", itemMaxWidth1, cellPaint) // Text wrapping untuk kolom ITEM
                val remarkLines1 = wrapText(remark1, remarkMaxWidth1, cellPaint) // Text wrapping untuk kolom KETERANGAN

// Hitung tinggi baris secara dinamis berdasarkan jumlah baris di kolom ITEM dan KETERANGAN
                val dynamicRowHeight1 = maxOf(
                    40f, // Tinggi minimum baris
                    20f + (itemLines1.size * 15f), // Tinggi berdasarkan jumlah baris di kolom ITEM
                    20f + (remarkLines1.size * 15f) // Tinggi berdasarkan jumlah baris di kolom KETERANGAN
                )

// Cek apakah baris ini muat di halaman saat ini
                if (y + dynamicRowHeight1 > pageHeight - marginBottom) {
                    drawFooter() // Tambahkan footer sebelum berpindah halaman
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

// Gambarkan tabel untuk baris 1
                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight1, tablePaint)
                canvas.drawText("1", colX[0] + 15f, y + 20f, cellPaint)

// Gambarkan teks di kolom ITEM
                var itemY1 = y + 18f
                for (line in itemLines1) {
                    canvas.drawText(line, colX[1] + 5f, itemY1, cellPaint)
                    itemY1 += 15f
                }

// Gambarkan teks di kolom SATUAN dan AKTUAL
                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual1, colX[3] + 10f, y + 20f, cellPaint)

// Gambarkan teks di kolom KETERANGAN
                var remarkY1 = y + 18f
                for (line in remarkLines1) {
                    canvas.drawText(line, colX[4] + 5f, remarkY1, cellPaint)
                    remarkY1 += 15f
                }

// Tambahkan garis vertikal untuk memisahkan kolom
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight1, tablePaint)
                }

// Perbarui posisi vertikal untuk baris berikutnya
                y += dynamicRowHeight1

                // Baris 2
                val itemMaxWidth2 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth2 = colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines2 = wrapText("Panjang Bundlecore Uplink (Dari Metro ke FTM-Rack ET)", itemMaxWidth2, cellPaint) // Text wrapping untuk kolom ITEM
                val remarkLines2 = wrapText(remark2, remarkMaxWidth2, cellPaint) // Text wrapping untuk kolom KETERANGAN

// Hitung tinggi baris secara dinamis berdasarkan jumlah baris di kolom ITEM dan KETERANGAN
                val dynamicRowHeight2 = maxOf(
                    40f, // Tinggi minimum baris
                    20f + (itemLines2.size * 15f), // Tinggi berdasarkan jumlah baris di kolom ITEM
                    20f + (remarkLines2.size * 15f) // Tinggi berdasarkan jumlah baris di kolom KETERANGAN
                )

// Cek apakah baris ini muat di halaman saat ini
                if (y + dynamicRowHeight2 > pageHeight - marginBottom) {
                    drawFooter() // Tambahkan footer sebelum berpindah halaman
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

// Gambarkan tabel untuk baris 2
                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight2, tablePaint)
                canvas.drawText("2", colX[0] + 15f, y + 20f, cellPaint)

// Gambarkan teks di kolom ITEM
                var itemY2 = y + 18f
                for (line in itemLines2) {
                    canvas.drawText(line, colX[1] + 5f, itemY2, cellPaint)
                    itemY2 += 15f
                }

// Gambarkan teks di kolom SATUAN dan AKTUAL
                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual2, colX[3] + 10f, y + 20f, cellPaint)

// Gambarkan teks di kolom KETERANGAN
                var remarkY2 = y + 18f
                for (line in remarkLines2) {
                    canvas.drawText(line, colX[4] + 5f, remarkY2, cellPaint)
                    remarkY2 += 15f
                }

// Tambahkan garis vertikal untuk memisahkan kolom
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight2, tablePaint)
                }

// Perbarui posisi vertikal untuk baris berikutnya
                y += dynamicRowHeight2

                // Baris 3
                val itemMaxWidth3 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth3 = colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines3 = wrapText("Panjang Bundlecore Uplink (Dari Rack ET ke OLT)", itemMaxWidth3, cellPaint) // Text wrapping untuk kolom ITEM
                val remarkLines3 = wrapText(remark3, remarkMaxWidth3, cellPaint) // Text wrapping untuk kolom KETERANGAN

// Hitung tinggi baris secara dinamis berdasarkan jumlah baris di kolom ITEM dan KETERANGAN
                val dynamicRowHeight3 = maxOf(
                    40f, // Tinggi minimum baris
                    20f + (itemLines3.size * 15f), // Tinggi berdasarkan jumlah baris di kolom ITEM
                    20f + (remarkLines3.size * 15f) // Tinggi berdasarkan jumlah baris di kolom KETERANGAN
                )

// Cek apakah baris ini muat di halaman saat ini
                if (y + dynamicRowHeight3 > pageHeight - marginBottom) {
                    drawFooter() // Tambahkan footer sebelum berpindah halaman
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

// Gambarkan tabel untuk baris 3
                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight3, tablePaint)
                canvas.drawText("3", colX[0] + 15f, y + 20f, cellPaint)

// Gambarkan teks di kolom ITEM
                var itemY3 = y + 18f
                for (line in itemLines3) {
                    canvas.drawText(line, colX[1] + 5f, itemY3, cellPaint)
                    itemY3 += 15f
                }

// Gambarkan teks di kolom SATUAN dan AKTUAL
                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual3, colX[3] + 10f, y + 20f, cellPaint)

// Gambarkan teks di kolom KETERANGAN
                var remarkY3 = y + 18f
                for (line in remarkLines3) {
                    canvas.drawText(line, colX[4] + 5f, remarkY3, cellPaint)
                    remarkY3 += 15f
                }

// Tambahkan garis vertikal untuk memisahkan kolom
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight3, tablePaint)
                }

// Perbarui posisi vertikal untuk baris berikutnya
                y += dynamicRowHeight3

                // Baris 4
                val itemMaxWidth4 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth4 = colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines4 = wrapText("Panjang Bundlecore Downlink (Dari Rack EA ke OLT)", itemMaxWidth4, cellPaint) // Text wrapping untuk kolom ITEM
                val remarkLines4 = wrapText(remark4, remarkMaxWidth4, cellPaint) // Text wrapping untuk kolom KETERANGAN

// Hitung tinggi baris secara dinamis berdasarkan jumlah baris di kolom ITEM dan KETERANGAN
                val dynamicRowHeight4 = maxOf(
                    40f, // Tinggi minimum baris
                    20f + (itemLines4.size * 15f), // Tinggi berdasarkan jumlah baris di kolom ITEM
                    20f + (remarkLines4.size * 15f) // Tinggi berdasarkan jumlah baris di kolom KETERANGAN
                )

// Cek apakah baris ini muat di halaman saat ini
                if (y + dynamicRowHeight4 > pageHeight - marginBottom) {
                    drawFooter() // Tambahkan footer sebelum berpindah halaman
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

// Gambarkan tabel untuk baris 4
                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight4, tablePaint)
                canvas.drawText("4", colX[0] + 15f, y + 20f, cellPaint)

// Gambarkan teks di kolom ITEM
                var itemY4 = y + 18f
                for (line in itemLines4) {
                    canvas.drawText(line, colX[1] + 5f, itemY4, cellPaint)
                    itemY4 += 15f
                }

// Gambarkan teks di kolom SATUAN dan AKTUAL
                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual4, colX[3] + 10f, y + 20f, cellPaint)

// Gambarkan teks di kolom KETERANGAN
                var remarkY4 = y + 18f
                for (line in remarkLines4) {
                    canvas.drawText(line, colX[4] + 5f, remarkY4, cellPaint)
                    remarkY4 += 15f
                }

// Tambahkan garis vertikal untuk memisahkan kolom
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight4, tablePaint)
                }

// Perbarui posisi vertikal untuk baris berikutnya
                y += dynamicRowHeight4

                // Baris 5
                val itemMaxWidth5 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth5 = colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines5 = wrapText("Pengecekan ground bar ruangan dan Panjang kabel grounding ke ground bar ruangan(kabel-25mm)", itemMaxWidth5, cellPaint) // Text wrapping untuk kolom ITEM
                val remarkLines5 = wrapText(remark5, remarkMaxWidth5, cellPaint) // Text wrapping untuk kolom KETERANGAN

                val dynamicRowHeight5 = maxOf(
                    40f,
                    20f + (itemLines5.size * 15f),
                    20f + (remarkLines5.size * 15f)
                )

                if (y + dynamicRowHeight5 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight5, tablePaint)
                canvas.drawText("5", colX[0] + 15f, y + 20f, cellPaint)

                var itemY5 = y + 18f
                for (line in itemLines5) {
                    canvas.drawText(line, colX[1] + 5f, itemY5, cellPaint)
                    itemY5 += 15f
                }

                canvas.drawText("meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual5, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY5 = y + 18f
                for (line in remarkLines5) {
                    canvas.drawText(line, colX[4] + 5f, remarkY5, cellPaint)
                    remarkY5 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight5, tablePaint)
                }

                y += dynamicRowHeight5

// Baris 6
                val itemMaxWidth6 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth6 = colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines6 = wrapText("Panjang Kabel Power (25mm) Dari OLT ke DCPDB Eksisting/New (Untuk 2 source)", itemMaxWidth6, cellPaint) // Text wrapping untuk kolom ITEM
                val remarkLines6 = wrapText(remark6, remarkMaxWidth6, cellPaint) // Text wrapping untuk kolom KETERANGAN

                val dynamicRowHeight6 = maxOf(
                    40f,
                    20f + (itemLines6.size * 15f),
                    20f + (remarkLines6.size * 15f)
                )

                if (y + dynamicRowHeight6 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight6, tablePaint)
                canvas.drawText("6", colX[0] + 15f, y + 20f, cellPaint)

                var itemY6 = y + 18f
                for (line in itemLines6) {
                    canvas.drawText(line, colX[1] + 5f, itemY6, cellPaint)
                    itemY6 += 15f
                }

                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual6, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY6 = y + 18f
                for (line in remarkLines6) {
                    canvas.drawText(line, colX[4] + 5f, remarkY6, cellPaint)
                    remarkY6 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight6, tablePaint)
                }

                y += dynamicRowHeight6

                // Baris 7
                val itemMaxWidth7 = colX[2] - colX[1] - 10f
                val remarkMaxWidth7 = colX[5] - colX[4] - 10f

                val itemLines7 = wrapText("Panjang Kabel Power (35mm). Kebutuhan yang diperlukan jika tidak ada DCPDB Eksisting / Menggunakan DCPDB New", itemMaxWidth7, cellPaint)
                val remarkLines7 = wrapText(remark7, remarkMaxWidth7, cellPaint)

                val dynamicRowHeight7 = maxOf(
                    40f,
                    20f + (itemLines7.size * 15f),
                    20f + (remarkLines7.size * 15f)
                )

                if (y + dynamicRowHeight7 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight7, tablePaint)
                canvas.drawText("7", colX[0] + 15f, y + 20f, cellPaint)

                var itemY7 = y + 18f
                for (line in itemLines7) {
                    canvas.drawText(line, colX[1] + 5f, itemY7, cellPaint)
                    itemY7 += 15f
                }

                canvas.drawText("meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual7, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY7 = y + 18f
                for (line in remarkLines7) {
                    canvas.drawText(line, colX[4] + 5f, remarkY7, cellPaint)
                    remarkY7 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight7, tablePaint)
                }

                y += dynamicRowHeight7

// Baris 8
                val itemMaxWidth8 = colX[2] - colX[1] - 10f
                val remarkMaxWidth8 = colX[5] - colX[4] - 10f

                val itemLines8 = wrapText("Kebutuhan catuan daya di Recti", itemMaxWidth8, cellPaint)
                val remarkLines8 = wrapText(remark8, remarkMaxWidth8, cellPaint)

                val dynamicRowHeight8 = maxOf(
                    40f,
                    20f + (itemLines8.size * 15f),
                    20f + (remarkLines8.size * 15f)
                )

                if (y + dynamicRowHeight8 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight8, tablePaint)
                canvas.drawText("8", colX[0] + 15f, y + 20f, cellPaint)

                var itemY8 = y + 18f
                for (line in itemLines8) {
                    canvas.drawText(line, colX[1] + 5f, itemY8, cellPaint)
                    itemY8 += 15f
                }

                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual8, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY8 = y + 18f
                for (line in remarkLines8) {
                    canvas.drawText(line, colX[4] + 5f, remarkY8, cellPaint)
                    remarkY8 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight8, tablePaint)
                }

                y += dynamicRowHeight8

// Baris 9
                val itemMaxWidth9 = colX[2] - colX[1] - 10f
                val remarkMaxWidth9 = colX[5] - colX[4] - 10f

                val itemLines9 = wrapText("Kebutuhan DCPDB New, jika dibutuhkan dan Propose DCPDBnya", itemMaxWidth9, cellPaint)
                val remarkLines9 = wrapText(remark9, remarkMaxWidth9, cellPaint)

                val dynamicRowHeight9 = maxOf(
                    40f,
                    20f + (itemLines9.size * 15f),
                    20f + (remarkLines9.size * 15f)
                )

                if (y + dynamicRowHeight9 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight9, tablePaint)
                canvas.drawText("9", colX[0] + 15f, y + 20f, cellPaint)

                var itemY9 = y + 18f
                for (line in itemLines9) {
                    canvas.drawText(line, colX[1] + 5f, itemY9, cellPaint)
                    itemY9 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual9, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY9 = y + 18f
                for (line in remarkLines9) {
                    canvas.drawText(line, colX[4] + 5f, remarkY9, cellPaint)
                    remarkY9 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight9, tablePaint)
                }

                y += dynamicRowHeight9

                // Baris 10
                val itemMaxWidth10 = colX[2] - colX[1] - 10f
                val remarkMaxWidth10 = colX[5] - colX[4] - 10f

                val itemLines10 = wrapText("Kebutuhan Tray @3m (pcs) dari Tray Eksisting ke OLT-turunan Dan Rack FTM-EA kalau diperlukan", itemMaxWidth10, cellPaint)
                val remarkLines10 = wrapText(remark10, remarkMaxWidth10, cellPaint)

                val dynamicRowHeight10 = maxOf(
                    40f,
                    20f + (itemLines10.size * 15f),
                    20f + (remarkLines10.size * 15f)
                )

                if (y + dynamicRowHeight10 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight10, tablePaint)
                canvas.drawText("10", colX[0] + 15f, y + 20f, cellPaint)

                var itemY10 = y + 18f
                for (line in itemLines10) {
                    canvas.drawText(line, colX[1] + 5f, itemY10, cellPaint)
                    itemY10 += 15f
                }

                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual10, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY10 = y + 18f
                for (line in remarkLines10) {
                    canvas.drawText(line, colX[4] + 5f, remarkY10, cellPaint)
                    remarkY10 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight10, tablePaint)
                }

                y += dynamicRowHeight10

// Baris 11
                val itemMaxWidth11 = colX[2] - colX[1] - 10f
                val remarkMaxWidth11 = colX[5] - colX[4] - 10f

                val itemLines11 = wrapText("Kebutuhan MCB 63A-Schneider", itemMaxWidth11, cellPaint)
                val remarkLines11 = wrapText(remark11, remarkMaxWidth11, cellPaint)

                val dynamicRowHeight11 = maxOf(
                    40f,
                    20f + (itemLines11.size * 15f),
                    20f + (remarkLines11.size * 15f)
                )

                if (y + dynamicRowHeight11 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight11, tablePaint)
                canvas.drawText("11", colX[0] + 15f, y + 20f, cellPaint)

                var itemY11 = y + 18f
                for (line in itemLines11) {
                    canvas.drawText(line, colX[1] + 5f, itemY11, cellPaint)
                    itemY11 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual11, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY11 = y + 18f
                for (line in remarkLines11) {
                    canvas.drawText(line, colX[4] + 5f, remarkY11, cellPaint)
                    remarkY11 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight11, tablePaint)
                }

                y += dynamicRowHeight11

// Baris 12
                val itemMaxWidth12 = colX[2] - colX[1] - 10f
                val remarkMaxWidth12 = colX[5] - colX[4] - 10f

                val itemLines12 = wrapText("Space 2 pcs FTB untuk di install di rack EA", itemMaxWidth12, cellPaint)
                val remarkLines12 = wrapText(remark12, remarkMaxWidth12, cellPaint)

                val dynamicRowHeight12 = maxOf(
                    40f,
                    20f + (itemLines12.size * 15f),
                    20f + (remarkLines12.size * 15f)
                )

                if (y + dynamicRowHeight12 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight12, tablePaint)
                canvas.drawText("12", colX[0] + 15f, y + 20f, cellPaint)

                var itemY12 = y + 18f
                for (line in itemLines12) {
                    canvas.drawText(line, colX[1] + 5f, itemY12, cellPaint)
                    itemY12 += 15f
                }

                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual12, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY12 = y + 18f
                for (line in remarkLines12) {
                    canvas.drawText(line, colX[4] + 5f, remarkY12, cellPaint)
                    remarkY12 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight12, tablePaint)
                }

                y += dynamicRowHeight12

// Baris 13
                val itemMaxWidth13 = colX[2] - colX[1] - 10f
                val remarkMaxWidth13 = colX[5] - colX[4] - 10f

                val itemLines13 = wrapText("FTB yang kita gunakan FTB Type TDS/MDT tidak bisa mengikuti FTB Eksisting jika ada yang beda", itemMaxWidth13, cellPaint)
                val remarkLines13 = wrapText(remark13, remarkMaxWidth13, cellPaint)

                val dynamicRowHeight13 = maxOf(
                    40f,
                    20f + (itemLines13.size * 15f),
                    20f + (remarkLines13.size * 15f)
                )

                if (y + dynamicRowHeight13 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight13, tablePaint)
                canvas.drawText("13", colX[0] + 15f, y + 20f, cellPaint)

                var itemY13 = y + 18f
                for (line in itemLines13) {
                    canvas.drawText(line, colX[1] + 5f, itemY13, cellPaint)
                    itemY13 += 15f
                }

                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual13, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY13 = y + 18f
                for (line in remarkLines13) {
                    canvas.drawText(line, colX[4] + 5f, remarkY13, cellPaint)
                    remarkY13 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight13, tablePaint)
                }

                y += dynamicRowHeight13

                // Baris 14
                val itemMaxWidth14 = colX[2] - colX[1] - 10f
                val remarkMaxWidth14 = colX[5] - colX[4] - 10f

                val itemLines14 = wrapText("Kebutuhan Rack EA", itemMaxWidth14, cellPaint)
                val remarkLines14 = wrapText(remark14, remarkMaxWidth14, cellPaint)

                val dynamicRowHeight14 = maxOf(
                    40f,
                    20f + (itemLines14.size * 15f),
                    20f + (remarkLines14.size * 15f)
                )

                if (y + dynamicRowHeight14 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight14, tablePaint)
                canvas.drawText("14", colX[0] + 15f, y + 20f, cellPaint)

                var itemY14 = y + 18f
                for (line in itemLines14) {
                    canvas.drawText(line, colX[1] + 5f, itemY14, cellPaint)
                    itemY14 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual14, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY14 = y + 18f
                for (line in remarkLines14) {
                    canvas.drawText(line, colX[4] + 5f, remarkY14, cellPaint)
                    remarkY14 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight14, tablePaint)
                }

                y += dynamicRowHeight14

// Baris 15
                val itemMaxWidth15 = colX[2] - colX[1] - 10f
                val remarkMaxWidth15 = colX[5] - colX[4] - 10f

                val itemLines15 = wrapText("Alokasi Port Metro", itemMaxWidth15, cellPaint)
                val remarkLines15 = wrapText(remark15, remarkMaxWidth15, cellPaint)

                val dynamicRowHeight15 = maxOf(
                    40f,
                    20f + (itemLines15.size * 15f),
                    20f + (remarkLines15.size * 15f)
                )

                if (y + dynamicRowHeight15 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight15, tablePaint)
                canvas.drawText("15", colX[0] + 15f, y + 20f, cellPaint)

                var itemY15 = y + 18f
                for (line in itemLines15) {
                    canvas.drawText(line, colX[1] + 5f, itemY15, cellPaint)
                    itemY15 += 15f
                }

                canvas.drawText("Port", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual15, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY15 = y + 18f
                for (line in remarkLines15) {
                    canvas.drawText(line, colX[4] + 5f, remarkY15, cellPaint)
                    remarkY15 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight15, tablePaint)
                }

                y += dynamicRowHeight15

// Baris 16
                val itemMaxWidth16 = colX[2] - colX[1] - 10f
                val remarkMaxWidth16 = colX[5] - colX[4] - 10f

                val itemLines16 = wrapText("Kebutuhan SFP", itemMaxWidth16, cellPaint)
                val remarkLines16 = wrapText(remark16, remarkMaxWidth16, cellPaint)

                val dynamicRowHeight16 = maxOf(
                    40f,
                    20f + (itemLines16.size * 15f),
                    20f + (remarkLines16.size * 15f)
                )

                if (y + dynamicRowHeight16 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight16, tablePaint)
                canvas.drawText("16", colX[0] + 15f, y + 20f, cellPaint)

                var itemY16 = y + 18f
                for (line in itemLines16) {
                    canvas.drawText(line, colX[1] + 5f, itemY16, cellPaint)
                    itemY16 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual16, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY16 = y + 18f
                for (line in remarkLines16) {
                    canvas.drawText(line, colX[4] + 5f, remarkY16, cellPaint)
                    remarkY16 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight16, tablePaint)
                }

                y += dynamicRowHeight16

// Baris 17
                val itemMaxWidth17 = colX[2] - colX[1] - 10f
                val remarkMaxWidth17 = colX[5] - colX[4] - 10f

                val itemLines17 = wrapText("Alokasi Core di FTB Eksisting (di Rack ET)", itemMaxWidth17, cellPaint)
                val remarkLines17 = wrapText(remark17, remarkMaxWidth17, cellPaint)

                val dynamicRowHeight17 = maxOf(
                    40f,
                    20f + (itemLines17.size * 15f),
                    20f + (remarkLines17.size * 15f)
                )

                if (y + dynamicRowHeight17 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight17, tablePaint)
                canvas.drawText("17", colX[0] + 15f, y + 20f, cellPaint)

                var itemY17 = y + 18f
                for (line in itemLines17) {
                    canvas.drawText(line, colX[1] + 5f, itemY17, cellPaint)
                    itemY17 += 15f
                }

                canvas.drawText("Core", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual17, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY17 = y + 18f
                for (line in remarkLines17) {
                    canvas.drawText(line, colX[4] + 5f, remarkY17, cellPaint)
                    remarkY17 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight17, tablePaint)
                }

                y += dynamicRowHeight17

                // Baris 18
                val itemMaxWidth18 = colX[2] - colX[1] - 10f
                val remarkMaxWidth18 = colX[5] - colX[4] - 10f

                val itemLines18 = wrapText("Kondisi Penerangan di ruangan OLT", itemMaxWidth18, cellPaint)
                val remarkLines18 = wrapText(remark18, remarkMaxWidth18, cellPaint)

                val dynamicRowHeight18 = maxOf(
                    40f,
                    20f + (itemLines18.size * 15f),
                    20f + (remarkLines18.size * 15f)
                )

                if (y + dynamicRowHeight18 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight18, tablePaint)
                canvas.drawText("18", colX[0] + 15f, y + 20f, cellPaint)

                var itemY18 = y + 18f
                for (line in itemLines18) {
                    canvas.drawText(line, colX[1] + 5f, itemY18, cellPaint)
                    itemY18 += 15f
                }

                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual18, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY18 = y + 18f
                for (line in remarkLines18) {
                    canvas.drawText(line, colX[4] + 5f, remarkY18, cellPaint)
                    remarkY18 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight18, tablePaint)
                }

                y += dynamicRowHeight18

// Baris 19
                val itemMaxWidth19 = colX[2] - colX[1] - 10f
                val remarkMaxWidth19 = colX[5] - colX[4] - 10f

                val itemLines19 = wrapText("CME – Kebutuhan Air Conditioner", itemMaxWidth19, cellPaint)
                val remarkLines19 = wrapText(remark19, remarkMaxWidth19, cellPaint)

                val dynamicRowHeight19 = maxOf(
                    40f,
                    20f + (itemLines19.size * 15f),
                    20f + (remarkLines19.size * 15f)
                )

                if (y + dynamicRowHeight19 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight19, tablePaint)
                canvas.drawText("19", colX[0] + 15f, y + 20f, cellPaint)

                var itemY19 = y + 18f
                for (line in itemLines19) {
                    canvas.drawText(line, colX[1] + 5f, itemY19, cellPaint)
                    itemY19 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual19, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY19 = y + 18f
                for (line in remarkLines19) {
                    canvas.drawText(line, colX[4] + 5f, remarkY19, cellPaint)
                    remarkY19 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight19, tablePaint)
                }

                y += dynamicRowHeight19

                // Setelah semua baris tabel selesai
                drawClosingStatement() // Tambahkan teks penutup di bawah tabel terakhir
                // Panggil drawSignatures di halaman terakhir
                val region = findViewById<EditText>(R.id.etTselRegion).text.toString()
                drawSignaturesWithFormattedTitles(canvas, region, y + 30f, paint, boldPaint)


                drawFooter() // Tambahkan footer di halaman terakhir
                document.finishPage(page)

                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

// Cek nama file yang belum digunakan
                var fileIndex = 1
                var file: File
                do {
                    val fileName = "SurveyLokasi$fileIndex.pdf"
                    file = File(downloadsDir, fileName)
                    fileIndex++
                } while (file.exists())

// Simpan dokumen
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