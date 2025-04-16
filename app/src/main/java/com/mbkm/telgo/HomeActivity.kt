package com.mbkm.telgo

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
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

    // Save WebView state
    private var webViewState: Bundle? = null

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

        // Restore WebView state if exists, otherwise load the dashboard
        if (savedInstanceState != null) {
            webViewState = savedInstanceState.getBundle("webViewState")
        }

        if (webViewState != null) {
            webView.restoreState(webViewState!!)
        } else {
            // Load dashboard
            loadLookerDashboard()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save WebView state
        val webViewBundle = Bundle()
        webView.saveState(webViewBundle)
        outState.putBundle("webViewState", webViewBundle)
    }

    private fun injectScrollFixJavaScript() {
        val javascript = """
            javascript:(function() {
                // Make all elements scrollable
                var css = document.createElement('style');
                css.type = 'text/css';
                css.innerHTML = '* { -webkit-overflow-scrolling: touch !important; } ' +
                               'body { overflow: auto !important; } ' +
                               '.looker-table-container { overflow: auto !important; max-height: none !important; } ' +
                               'table { overflow: auto !important; }';
                document.head.appendChild(css);
                
                // Find tables and make them scrollable
                var tables = document.querySelectorAll('table');
                for(var i=0; i<tables.length; i++) {
                    var table = tables[i];
                    var wrapper = document.createElement('div');
                    wrapper.style.overflow = 'auto';
                    wrapper.style.width = '100%';
                    table.parentNode.insertBefore(wrapper, table);
                    wrapper.appendChild(table);
                }
                
                // Tell the page it's in an app webview
                document.documentElement.classList.add('in-app-webview');
            })()
        """.trimIndent()

        webView.evaluateJavascript(javascript, null)
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

            // Enable scrolling
            setSupportZoom(true)
            builtInZoomControls = true

            // Important for scrolling to work properly
            blockNetworkImage = false
            loadsImagesAutomatically = true

            // Cache settings to improve performance
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        // Enable hardware acceleration for better performance
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // Override the WebView touch handling to ensure scrolling works
        webView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                    // Return false to indicate the event was not consumed
                    // and should be passed to the WebView for scrolling
                    if (!v.hasFocus()) {
                        v.requestFocus()
                    }
                    false
                }
                else -> false
            }
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

                // Inject JavaScript to fix scrolling issues
                injectScrollFixJavaScript()

                // Set scrollbar visibility based on current orientation
                val currentOrientation = resources.configuration.orientation
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    webView.isVerticalScrollBarEnabled = true
                    webView.isHorizontalScrollBarEnabled = true
                } else {
                    webView.isVerticalScrollBarEnabled = true
                    webView.isHorizontalScrollBarEnabled = false
                }
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