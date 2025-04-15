package com.mbkm.telgo

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnLogout: Button
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorLayout: LinearLayout
    private lateinit var btnRefresh: Button

    private val lookerEmbedUrl = "https://lookerstudio.google.com/embed/reporting/ccdfc03d-400e-4caa-bd79-982488833438/page/AeBGF"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inisialisasi komponen UI
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        btnLogout = findViewById(R.id.btnLogout)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorLayout = findViewById(R.id.errorLayout)
        btnRefresh = findViewById(R.id.btnRefresh)

        // Set listener navigasi bawah
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        // Tombol logout
        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Tombol refresh
        btnRefresh.setOnClickListener {
            loadLookerDashboard()
        }

        // Setup WebView untuk Looker
        setupWebView()

        // Load dashboard
        loadLookerDashboard()
    }

    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // Tampilkan loading, sembunyikan error
                progressBar.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
                webView.visibility = View.INVISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Sembunyikan loading, tampilkan WebView
                progressBar.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                // Cek apakah error pada main frame (bukan resource)
                if (request?.isForMainFrame == true) {
                    // Tampilkan layout error, sembunyikan WebView
                    progressBar.visibility = View.GONE
                    webView.visibility = View.INVISIBLE
                    errorLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadLookerDashboard() {
        // Tampilkan loading
        progressBar.visibility = View.VISIBLE
        errorLayout.visibility = View.GONE
        webView.visibility = View.INVISIBLE

        // Load URL
        webView.loadUrl(lookerEmbedUrl)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_home -> {
                // We're already in HomeActivity, no need to start a new activity
                return true
            }
            R.id.navigation_services -> { // Sesuaikan dengan ID yang benar di menu Anda
                val intent = Intent(this, ServicesActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.navigation_history -> {
                val intent = Intent(this, LastUpdateActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.navigation_account -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}