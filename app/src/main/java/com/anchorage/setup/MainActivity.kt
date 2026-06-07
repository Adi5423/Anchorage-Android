package com.anchorage.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import android.util.Base64

class MainActivity : Activity() {

    private lateinit var mainContainer: RelativeLayout
    private lateinit var homeView: LinearLayout
    private lateinit var variablesView: LinearLayout
    private lateinit var sidebarOverlay: LinearLayout
    
    private lateinit var statusTextHeader: TextView
    private lateinit var statusTextBody: TextView
    private lateinit var actionButton: Button
    private lateinit var varListContainer: LinearLayout
    private lateinit var prefs: SharedPreferences

    private val cryptoKey = "AnchorageShellEcosystemKeyShared"
    
    // Dynamic UI State
    private var isDarkMode = false
    private var currentThemeColor = Color.parseColor("#455A64")
    private var bgColor = 0
    private var cardColor = 0
    private var textColorMain = 0
    private var textColorSub = 0

    private val sBox = intArrayOf(
        0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76
    )

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.rgb(r, g, b)
    }

    private fun createRoundedBg(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun createCircleBg(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun resolveDynamicColors() {
        val baseBg = if (isDarkMode) Color.parseColor("#121212") else Color.parseColor("#F4F6F8")
        val baseCard = if (isDarkMode) Color.parseColor("#1E1E1E") else Color.WHITE
        
        // Tints the background slightly with the active theme color for a cohesive blend
        bgColor = blendColors(baseBg, currentThemeColor, if (isDarkMode) 0.15f else 0.05f)
        cardColor = blendColors(baseCard, currentThemeColor, if (isDarkMode) 0.10f else 0.03f)
        
        textColorMain = if (isDarkMode) Color.WHITE else Color.BLACK
        textColorSub = if (isDarkMode) Color.parseColor("#B0B0B0") else Color.parseColor("#555555")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("AnchoragePrefs", Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean("dark_mode", false)
        currentThemeColor = prefs.getInt("theme_color", Color.parseColor("#455A64"))
        
        resolveDynamicColors()

        mainContainer = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(bgColor)
        }

        setupHomeLayout()
        setupVariablesLayout()
        setupSidebarOverlay()

        mainContainer.addView(homeView)
        mainContainer.addView(variablesView)
        mainContainer.addView(sidebarOverlay)
        
        setContentView(mainContainer)
    }
    override fun onResume() {
        super.onResume()
        checkPermissionsAndState()
    }

    private fun setupHomeLayout() {
        homeView = LinearLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        // --- TOP HEADER ---
        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 64)
        }

        val menuButton = TextView(this).apply {
            text = "☰"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = createCircleBg(currentThemeColor)
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setOnClickListener { openSidebar() }
        }

        val logoApp = TextView(this).apply {
            text = "A"
            textSize = 28f
            setTypeface(null, Typeface.BOLD_ITALIC)
            setTextColor(textColorMain)
            setPadding(32, 0, 16, 0)
        }

        val titleView = TextView(this).apply {
            text = "System Setup Pipeline"
            textSize = 18f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTextColor(textColorMain)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        headerBar.addView(menuButton)
        headerBar.addView(logoApp)
        headerBar.addView(titleView)
        homeView.addView(headerBar)

        // --- STATUS CARD ---
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedBg(cardColor, 32f)
            setPadding(48, 48, 48, 48)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 48)
            }
        }

        statusTextHeader = TextView(this).apply {
            text = "Evaluating pipeline..."
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
        }
        
        statusTextBody = TextView(this).apply {
            text = "Checking storage access."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(textColorSub)
            setPadding(0, 8, 0, 0)
        }

        statusCard.addView(statusTextHeader)
        statusCard.addView(statusTextBody)
        homeView.addView(statusCard)

        // --- ACTION BUTTON ---
        actionButton = Button(this).apply {
            text = "INITIALIZE ENGINE"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = createRoundedBg(currentThemeColor, 64f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 160).apply {
                setMargins(0, 0, 0, 64)
            }
            isEnabled = false
        }
        homeView.addView(actionButton)

        // --- DARK MODE & THEME SETTINGS ---
        val settingsControls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(0, 32, 0, 32)
        }

        val themeLabel = TextView(this).apply {
            text = "Appearance & Theme:"
            textSize = 14f
            setTextColor(textColorSub)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val darkModeToggle = Button(this).apply {
            text = if (isDarkMode) "☀️ Light Mode" else "🌙 Dark Mode"
            textSize = 12f
            setTextColor(textColorMain)
            background = createRoundedBg(cardColor, 32f)
            setPadding(32, 16, 32, 16)
            setOnClickListener {
                prefs.edit().putBoolean("dark_mode", !isDarkMode).apply()
                recreate()
            }
        }

        settingsControls.addView(themeLabel)
        settingsControls.addView(darkModeToggle)
        homeView.addView(settingsControls)

        val themeGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(0, 16, 0, 0)
        }

        val themes = mapOf("Default" to "#455A64", "Teal" to "#00796B", "Indigo" to "#1A237E", "Charcoal" to "#212121")
        for ((name, colorStr) in themes) {
            val themeItem = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val colorCircle = View(this).apply {
                background = createCircleBg(Color.parseColor(colorStr))
                layoutParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(0,0,0,16) }
                setOnClickListener { 
                    prefs.edit().putInt("theme_color", Color.parseColor(colorStr)).apply()
                    recreate() 
                }
            }

            val tLabel = TextView(this).apply {
                text = name
                textSize = 12f
                setTextColor(textColorSub)
            }

            themeItem.addView(colorCircle)
            themeItem.addView(tLabel)
            themeGrid.addView(themeItem)
        }
        homeView.addView(themeGrid)
    }

    private fun setupSidebarOverlay() {
        sidebarOverlay = LinearLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            elevation = 10f
        }

        val menuPanel = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((resources.displayMetrics.widthPixels * 0.75).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            background = createRoundedBg(bgColor, 0f)
            setPadding(48, 120, 48, 48)
        }

        val anchorIcon = TextView(this).apply {
            text = "⚓"
            textSize = 48f
            gravity = Gravity.CENTER
            setTextColor(currentThemeColor)
            setPadding(0, 0, 0, 16)
        }

        val menuTitle = TextView(this).apply {
            text = "Anchorage Menu\n"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(textColorMain)
            setPadding(0, 0, 0, 48)
        }
        menuPanel.addView(anchorIcon)
        menuPanel.addView(menuTitle)

        // Casual Variables Button (No longer a huge highlighted block)
        val viewVarsBtn = TextView(this).apply {
            text = "View Global Variables"
            textSize = 16f
            setTextColor(textColorMain)
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 48)
            background = createRoundedBg(cardColor, 24f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 32, 0, 32)
            }
            setOnClickListener { openVariablesPage() }
        }
        menuPanel.addView(viewVarsBtn)

        val dimBackground = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#B3000000")) // 70% opacity black
            setOnClickListener { closeSidebar() }
        }

        sidebarOverlay.addView(menuPanel)
        sidebarOverlay.addView(dimBackground)
    }

    private fun setupVariablesLayout() {
        variablesView = LinearLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
            setBackgroundColor(bgColor)
            visibility = View.GONE
        }

        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 48)
        }

        val backBtn = TextView(this).apply {
            text = "←"
            textSize = 28f
            setTextColor(textColorMain)
            setPadding(0, 0, 32, 0)
            setOnClickListener { returnToHome() }
        }

        val title = TextView(this).apply {
            text = "Live Environment"
            textSize = 22f
            setTextColor(textColorMain)
        }

        headerBar.addView(backBtn)
        headerBar.addView(title)
        variablesView.addView(headerBar)

        val scrollPane = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        varListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        scrollPane.addView(varListContainer)
        variablesView.addView(scrollPane)
    }
    private fun openSidebar() { sidebarOverlay.visibility = View.VISIBLE }
    private fun closeSidebar() { sidebarOverlay.visibility = View.GONE }
    private fun openVariablesPage() {
        closeSidebar()
        homeView.visibility = View.GONE
        variablesView.visibility = View.VISIBLE
        refreshVariablesDisplay()
    }
    private fun returnToHome() {
        variablesView.visibility = View.GONE
        homeView.visibility = View.VISIBLE
    }

    private fun checkPermissionsAndState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                evaluateEcosystemState()
            } else {
                statusTextHeader.text = "Permission Required"
                statusTextBody.text = "All Files Access is needed for registry."
                statusTextHeader.setTextColor(if (isDarkMode) Color.parseColor("#EF5350") else Color.parseColor("#C62828"))
                
                actionButton.text = "GRANT SYSTEM PERMISSIONS"
                actionButton.isEnabled = true
                actionButton.setOnClickListener {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun evaluateEcosystemState() {
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        val envFile = File(rootPath, "anchorage.env")

        if (envFile.exists()) {
            statusTextHeader.text = "System Status: ACTIVE & CONFIRMED"
            statusTextHeader.setTextColor(if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#2E7D32"))
            statusTextBody.text = "Infrastructure files are actively bound."
            
            actionButton.text = "REFRESH CORE BOOTSTRAP"
            actionButton.isEnabled = true
            actionButton.setOnClickListener { runEcosystemBootstrap() }
        } else {
            statusTextHeader.text = "System Status: UNINITIALIZED"
            statusTextHeader.setTextColor(if (isDarkMode) Color.parseColor("#EF5350") else Color.parseColor("#C62828"))
            statusTextBody.text = "Ready to map structural environment."
            
            actionButton.text = "INITIALIZE CORE LAYER"
            actionButton.isEnabled = true
            actionButton.setOnClickListener { runEcosystemBootstrap() }
        }
    }

    private fun runEcosystemBootstrap() {
        try {
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            val anchorageDir = File(rootPath, ".anchorage")
            val defaultPackDir = File(rootPath, ".defaultPack")

            listOf(anchorageDir, defaultPackDir, File(anchorageDir, "cache"), File(anchorageDir, "temp"), File(anchorageDir, "logs")).forEach {
                if (!it.exists()) it.mkdirs()
            }

            val envData = "DEFAULT_PACK=$rootPath/.defaultPack"
            val envFile = File(rootPath, "anchorage.env")
            envFile.writeText(encryptData(envData))

            evaluateEcosystemState()
            Toast.makeText(this, "Ecosystem Synced Natively!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            statusTextBody.text = "Error: ${e.message}"
        }
    }

    private fun refreshVariablesDisplay() {
        varListContainer.removeAllViews()
        val rootPath = Environment.getExternalStorageDirectory().absolutePath
        val envFile = File(rootPath, "anchorage.env")
        
        if (!envFile.exists()) {
            val emptyTxt = TextView(this).apply { text = "No variables registered."; setTextColor(textColorSub) }
            varListContainer.addView(emptyTxt)
            return
        }
        
        try {
            val decrypted = decryptData(envFile.readText().trim())
            val lines = decrypted.split("\n")
            
            for (line in lines) {
                if (line.contains("=")) {
                    val parts = line.split("=", limit = 2)
                    
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        background = createRoundedBg(cardColor, 24f)
                        setPadding(48, 48, 48, 48)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, 32)
                        }
                    }
                    
                    val keyTxt = TextView(this).apply {
                        text = parts[0].trim()
                        textSize = 16f
                        setTextColor(if (isDarkMode) Color.parseColor("#81C784") else Color.parseColor("#2E7D32"))
                        setTypeface(null, Typeface.BOLD)
                    }
                    val valTxt = TextView(this).apply {
                        text = parts[1].trim()
                        textSize = 14f
                        setTextColor(textColorMain)
                        setPadding(0, 16, 0, 0)
                    }
                    
                    card.addView(keyTxt)
                    card.addView(valTxt)
                    varListContainer.addView(card)
                }
            }
        } catch (e: Exception) {
            val errTxt = TextView(this).apply { text = "Decryption Error"; setTextColor(Color.RED) }
            varListContainer.addView(errTxt)
        }
    }

    private fun encryptData(plainText: String): String {
        val out = plainText.toByteArray(Charsets.UTF_8)
        val keyBytes = cryptoKey.toByteArray()
        for (i in out.indices) { out[i] = (out[i].toInt() xor keyBytes[i % keyBytes.size].toInt() xor sBox[i % sBox.size]).toByte() }
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decryptData(cipherText: String): String {
        val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
        val keyBytes = cryptoKey.toByteArray()
        for (i in decoded.indices) { decoded[i] = (decoded[i].toInt() xor keyBytes[i % keyBytes.size].toInt() xor sBox[i % sBox.size]).toByte() }
        return String(decoded, Charsets.UTF_8)
    }
}
