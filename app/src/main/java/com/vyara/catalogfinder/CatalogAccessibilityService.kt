package com.vyara.catalogfinder

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class CatalogAccessibilityService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: LinearLayout
    private lateinit var searchPanel: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var searchInput: EditText
    private val handler = Handler(Looper.getMainLooper())
    private var isSearching = false
    private var scrollAttempts = 0
    private val maxScrollAttempts = 18

    override fun onServiceConnected() {
        super.onServiceConnected()
        setupFloatingWidget()
    }

    private fun setupFloatingWidget() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 250
        }

        floatingView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Floating Bubble Button
        val bubbleBtn = TextView(this).apply {
            text = "🔍 ID"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(32, 22, 32, 22)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#25D366"))
                cornerRadius = 60f
                setStroke(3, Color.WHITE)
            }
        }

        // Expanded Search Bar (Extension Popup style)
        searchPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            visibility = View.GONE
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 28f
                setStroke(2, Color.parseColor("#25D366"))
            }
        }

        searchInput = EditText(this).apply {
            hint = "Enter Product ID (e.g. 104)"
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            textSize = 15f
            minWidth = 520
            setSingleLine(true)
        }

        statusText = TextView(this).apply {
            text = "WhatsApp Catalog kholein aur ID search karein"
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(4, 10, 4, 16)
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val btnSearch = Button(this).apply {
            text = "Find & Open"
            setBackgroundColor(Color.parseColor("#25D366"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val query = searchInput.text.toString().trim()
                if (query.isNotEmpty()) {
                    // Keyboard focus hatane ke liye
                    params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    windowManager.updateViewLayout(floatingView, params)
                    startCatalogSearch(query)
                } else {
                    Toast.makeText(context, "Pehle Product ID daalo!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnClose = Button(this).apply {
            text = "Minimize"
            setBackgroundColor(Color.parseColor("#475569"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                isSearching = false
                searchPanel.visibility = View.GONE
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(floatingView, params)
            }
        }

        btnRow.addView(btnClose)
        btnRow.addView(btnSearch)

        searchPanel.addView(searchInput)
        searchPanel.addView(statusText)
        searchPanel.addView(btnRow)

        floatingView.addView(bubbleBtn)
        floatingView.addView(searchPanel)

        // Drag & Tap Handler on Bubble
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        bubbleBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 15 && diffY < 15) {
                        // Toggle Search Box
                        if (searchPanel.visibility == View.GONE) {
                            searchPanel.visibility = View.VISIBLE
                            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            windowManager.updateViewLayout(floatingView, params)
                            searchInput.requestFocus()
                        } else {
                            searchPanel.visibility = View.GONE
                            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            windowManager.updateViewLayout(floatingView, params)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        windowManager.addView(floatingView, params)
    }

    private fun startCatalogSearch(productId: String) {
        isSearching = true
        scrollAttempts = 0
        statusText.text = "Searching '$productId' in catalog..."
        statusText.setTextColor(Color.parseColor("#FACC15"))
        scanAndScroll(productId)
    }

    private fun scanAndScroll(productId: String) {
        if (!isSearching) return

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            statusText.text = "⚠️ Screen read nahi ho rahi! WhatsApp par tap karein."
            isSearching = false
            return
        }

        // Screen par text dhundho (Exact ya partial Product ID match)
        val matchedNode = findProductNode(rootNode, productId)
        if (matchedNode != null) {
            statusText.text = "✅ Product '$productId' Mil Gaya! Opening..."
            statusText.setTextColor(Color.parseColor("#4ADE80"))
            isSearching = false
            clickNodeOrParent(matchedNode)
            return
        }

        // Agar is screen par nahi mila, toh neeche scroll karo (Auto-Scroll)
        if (scrollAttempts < maxScrollAttempts) {
            scrollAttempts++
            statusText.text = "Scrolling down... ($scrollAttempts/$maxScrollAttempts)"
            val scrolled = scrollListDown(rootNode)
            if (scrolled) {
                handler.postDelayed({ scanAndScroll(productId) }, 750)
            } else {
                statusText.text = "❌ Catalog ke end tak '$productId' nahi mila."
                statusText.setTextColor(Color.parseColor("#F87171"))
                isSearching = false
            }
        } else {
            statusText.text = "❌ Product ID '$productId' nahi mila."
            statusText.setTextColor(Color.parseColor("#F87171"))
            isSearching = false
        }
    }

    private fun findProductNode(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        if (node == null) return null

        // Apne hi floating search box ko ignore karo
        if (node.packageName?.toString() == packageName) return null

        val nodeText = node.text?.toString() ?: ""
        val nodeDesc = node.contentDescription?.toString() ?: ""

        if (nodeText.contains(query, ignoreCase = true) || nodeDesc.contains(query, ignoreCase = true)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val found = findProductNode(node.getChild(i), query)
            if (found != null) return found
        }
        return null
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo?) {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
            current = current.parent
        }
    }

    private fun scrollListDown(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isScrollable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        }
        for (i in 0 until node.childCount) {
            if (scrollListDown(node.getChild(i))) return true
        }
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
