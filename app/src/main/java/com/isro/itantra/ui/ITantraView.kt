package com.isro.itantra.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class ITantraView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val background = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foreground = Paint(Paint.ANTI_ALIAS_FLAG)
    private val muted = Paint(Paint.ANTI_ALIAS_FLAG)
    private var screen = ITantraScreen.Onboarding
    private var selectedLanguage = "हिन्दी"
    private var pttHeld = false

    private val green = 0xff2f7d4a.toInt()
    private val amber = 0xffb7791f.toInt()
    private val blue = 0xff2867a7.toInt()
    private val red = 0xffb33a3a.toInt()
    private val ink = 0xff20312a.toInt()
    private val paper = 0xfff7f4ec.toInt()
    private val line = 0xffd7ddd5.toInt()

    init {
        isFocusable = true
        background.color = paper
        foreground.color = ink
        muted.color = 0xff64736b.toInt()
        foreground.typeface = Typeface.create("sans", Typeface.NORMAL)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(paper)
        when (screen) {
            ITantraScreen.Onboarding -> drawOnboarding(canvas)
            ITantraScreen.Home -> drawHome(canvas)
            ITantraScreen.Pairing -> drawPairing(canvas)
            ITantraScreen.Talk -> drawTalk(canvas)
            ITantraScreen.Phone -> drawPhone(canvas)
            ITantraScreen.Alert -> drawAlert(canvas)
            ITantraScreen.Settings -> drawSettings(canvas)
        }
    }

    private fun drawOnboarding(canvas: Canvas) {
        title(canvas, "iTantra", "Offline voice. Nearby when it matters.")
        label(canvas, "भाषा चुनें  •  भाषा निवडा", 24f, 188f, ink, 20f)
        val languages = listOf("हिन्दी", "मराठी", "ગુજરાતી", "ಕನ್ನಡ", "മലയാളം", "தமிழ்", "తెలుగు", "ଓଡ଼ିଆ", "বাংলা", "English")
        languages.forEachIndexed { index, language ->
            val x = 24f + (index % 2) * 172f
            val y = 230f + (index / 2) * 58f
            choice(canvas, language, x, y, language == selectedLanguage)
        }
        button(canvas, "Continue  →", 24f, heightDp() - 86f, widthDp() - 48f, green)
    }

    private fun drawHome(canvas: Canvas) {
        topBar(canvas, "iTantra", "Local radio console")
        statusCard(canvas, "Connected to Sathi-02", "Wi-Fi Direct  •  1 nearby peer", green, 82f)
        label(canvas, "Choose a channel", 24f, 218f, ink, 22f)
        actionCard(canvas, "TALK", "Hold to transmit", "Push-to-talk", blue, 246f)
        actionCard(canvas, "PHONE", "Open duplex voice", "Hands-free conversation", green, 336f)
        actionCard(canvas, "ALERT", "Broadcast priority signal", "Emergency use only", red, 426f)
        bottomNav(canvas, ITantraScreen.Home)
    }

    private fun drawPairing(canvas: Canvas) {
        topBar(canvas, "Pairing", "Nearby devices")
        statusCard(canvas, "Scanning nearby", "Wi-Fi Direct + Bluetooth fallback", amber, 82f)
        peer(canvas, "Sathi-02", "Strong signal  •  12 m", 226f)
        peer(canvas, "Kiran handset", "Good signal  •  28 m", 302f)
        peer(canvas, "Base camp radio", "Bluetooth  •  9 m", 378f)
        button(canvas, "Scan again", 24f, 466f, widthDp() - 48f, blue)
        bottomNav(canvas, ITantraScreen.Pairing)
    }

    private fun drawTalk(canvas: Canvas) {
        topBar(canvas, "Talk", "Sathi-02  •  connected")
        message(canvas, "नमस्कार, आवाज़ साफ़ आ रही है।", "Received  10:42", 96f, false)
        message(canvas, "मी ऐकतोय. पुढे बोला.", "Received  10:43", 168f, false)
        label(canvas, if (pttHeld) "Transmitting" else "Ready to transmit", 24f, 286f, if (pttHeld) blue else amber, 18f)
        ptt(canvas, 24f, 318f)
        bottomNav(canvas, ITantraScreen.Talk)
    }

    private fun drawPhone(canvas: Canvas) {
        topBar(canvas, "Phone mode", "Duplex  •  Sathi-02")
        label(canvas, "Listening and speaking", 24f, 116f, green, 21f)
        circle(canvas, widthDp() / 2f, 242f, 64f, green)
        label(canvas, "MIC", widthDp() / 2f - 21f, 250f, paper, 14f)
        button(canvas, "End phone mode", 24f, 380f, widthDp() - 48f, ink)
        bottomNav(canvas, ITantraScreen.Phone)
    }

    private fun drawAlert(canvas: Canvas) {
        topBar(canvas, "Alert / SOS", "Priority broadcast")
        statusCard(canvas, "Emergency channel", "This will interrupt normal voice traffic", red, 84f)
        label(canvas, "Send a clear signal to every nearby peer.", 24f, 226f, ink, 18f)
        label(canvas, "आपत्कालीन संदेश  •  आपत्कालीन संदेश", 24f, 260f, muted.color, 16f)
        button(canvas, "Send SOS alert", 24f, 326f, widthDp() - 48f, red)
        label(canvas, "Use only when immediate help is needed.", 24f, 420f, muted.color, 14f)
        bottomNav(canvas, ITantraScreen.Alert)
    }

    private fun drawSettings(canvas: Canvas) {
        topBar(canvas, "Settings", "Device and diagnostics")
        row(canvas, "Language", selectedLanguage, 104f)
        row(canvas, "Audio engine", "Ready  •  offline", 166f)
        row(canvas, "Transport", "Wi-Fi Direct preferred", 228f)
        label(canvas, "Demo diagnostics", 24f, 332f, ink, 20f)
        row(canvas, "Round-trip latency", "84 ms", 366f)
        row(canvas, "Audio buffer", "20 ms", 428f)
        bottomNav(canvas, ITantraScreen.Settings)
    }

    private fun title(canvas: Canvas, heading: String, subheading: String) {
        label(canvas, heading, 24f, 86f, ink, 38f)
        label(canvas, subheading, 24f, 122f, muted.color, 16f)
    }

    private fun topBar(canvas: Canvas, heading: String, subheading: String) {
        label(canvas, heading, 24f, 58f, ink, 27f)
        label(canvas, subheading, 24f, 82f, muted.color, 14f)
    }

    private fun statusCard(canvas: Canvas, heading: String, subheading: String, color: Int, y: Float) {
        roundRect(canvas, 24f, y, widthDp() - 48f, 72f, 14f, 0xffe9eee8.toInt())
        circle(canvas, 52f, y + 36f, 10f, color)
        label(canvas, heading, 76f, y + 31f, ink, 16f)
        label(canvas, subheading, 76f, y + 53f, muted.color, 13f)
    }

    private fun actionCard(canvas: Canvas, heading: String, subheading: String, detail: String, color: Int, y: Float) {
        roundRect(canvas, 24f, y, widthDp() - 48f, 74f, 14f, 0xffffffff.toInt())
        circle(canvas, 55f, y + 37f, 18f, color)
        label(canvas, heading, 88f, y + 31f, ink, 17f)
        label(canvas, "$subheading  •  $detail", 88f, y + 54f, muted.color, 13f)
    }

    private fun peer(canvas: Canvas, name: String, detail: String, y: Float) {
        roundRect(canvas, 24f, y, widthDp() - 48f, 58f, 12f, 0xffffffff.toInt())
        circle(canvas, 50f, y + 29f, 8f, green)
        label(canvas, name, 72f, y + 25f, ink, 16f)
        label(canvas, detail, 72f, y + 45f, muted.color, 13f)
        label(canvas, "Connect", widthDp() - 92f, y + 34f, blue, 13f)
    }

    private fun message(canvas: Canvas, text: String, detail: String, y: Float, mine: Boolean) {
        roundRect(canvas, if (mine) 74f else 24f, y, widthDp() - if (mine) 98f else 48f, 54f, 12f, if (mine) 0xffe2edf7.toInt() else 0xffffffff.toInt())
        label(canvas, text, if (mine) 90f else 40f, y + 24f, ink, 15f)
        label(canvas, detail, if (mine) 90f else 40f, y + 44f, muted.color, 12f)
    }

    private fun ptt(canvas: Canvas, x: Float, y: Float) {
        roundRect(canvas, x, y, widthDp() - 48f, 112f, 18f, if (pttHeld) blue else 0xffdce8ef.toInt())
        label(canvas, if (pttHeld) "Release to send" else "Hold to talk", x + 24f, y + 47f, if (pttHeld) paper else ink, 25f)
        label(canvas, "Push-to-talk", x + 24f, y + 79f, if (pttHeld) paper else muted.color, 15f)
    }

    private fun choice(canvas: Canvas, text: String, x: Float, y: Float, selected: Boolean) {
        roundRect(canvas, x, y, 156f, 44f, 10f, if (selected) 0xffd8e9dc.toInt() else 0xffffffff.toInt())
        label(canvas, text, x + 14f, y + 28f, if (selected) green else ink, 15f)
    }

    private fun row(canvas: Canvas, heading: String, value: String, y: Float) {
        label(canvas, heading, 24f, y + 22f, ink, 16f)
        label(canvas, value, 24f, y + 44f, muted.color, 13f)
        line(canvas, 24f, y + 58f, widthDp() - 24f, y + 58f)
    }

    private fun button(canvas: Canvas, text: String, x: Float, y: Float, width: Float, color: Int) {
        roundRect(canvas, x, y, width, 56f, 14f, color)
        label(canvas, text, x + 18f, y + 35f, paper, 16f)
    }

    private fun bottomNav(canvas: Canvas, selected: ITantraScreen) {
        val y = heightDp() - 64f
        line(canvas, 0f, y, widthDp(), y)
        navItem(canvas, "Home", ITantraScreen.Home, 24f, y, selected)
        navItem(canvas, "Pair", ITantraScreen.Pairing, 104f, y, selected)
        navItem(canvas, "Talk", ITantraScreen.Talk, 184f, y, selected)
        navItem(canvas, "Settings", ITantraScreen.Settings, 264f, y, selected)
    }

    private fun navItem(canvas: Canvas, text: String, target: ITantraScreen, x: Float, y: Float, selected: ITantraScreen) {
        label(canvas, text, x, y + 38f, if (target == selected) blue else muted.color, 13f)
    }

    private fun label(canvas: Canvas, text: String, x: Float, y: Float, color: Int, size: Float) {
        foreground.color = color
        foreground.textSize = dp(size)
        foreground.typeface = Typeface.create("sans", Typeface.NORMAL)
        canvas.drawText(text, dp(x), dp(y), foreground)
    }

    private fun line(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        foreground.color = line
        foreground.strokeWidth = dp(1f)
        canvas.drawLine(dp(left), dp(top), dp(right), dp(bottom), foreground)
    }

    private fun circle(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        foreground.color = color
        canvas.drawCircle(dp(x), dp(y), dp(radius), foreground)
    }

    private fun roundRect(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, radius: Float, color: Int) {
        background.color = color
        canvas.drawRoundRect(RectF(dp(x), dp(y), dp(x + width), dp(y + height)), dp(radius), dp(radius), background)
    }

    private fun dp(value: Float): Float = value * density
    private fun widthDp(): Float = width / density
    private fun heightDp(): Float = height / density

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x / density
        val y = event.y / density
        if (event.action == MotionEvent.ACTION_DOWN && screen == ITantraScreen.Talk && y in 318f..430f) {
            pttHeld = true
            invalidate()
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            if (screen == ITantraScreen.Talk && pttHeld) {
                pttHeld = false
                invalidate()
                return true
            }
            if (screen == ITantraScreen.Onboarding && y > heightDp() - 110f) screen = ITantraScreen.Home
            else if (screen == ITantraScreen.Home && y in 246f..320f) screen = ITantraScreen.Talk
            else if (screen == ITantraScreen.Home && y in 336f..410f) screen = ITantraScreen.Phone
            else if (screen == ITantraScreen.Home && y in 426f..500f) screen = ITantraScreen.Alert
            else if (screen == ITantraScreen.Onboarding && y in 230f..520f) chooseLanguage(x, y)
            else if (y > heightDp() - 78f) navigateBottom(x)
            invalidate()
            return true
        }
        return true
    }

    private fun chooseLanguage(x: Float, y: Float) {
        val column = if (x < widthDp() / 2f) 0 else 1
        val row = ((y - 230f) / 58f).roundToInt()
        val languages = listOf("हिन्दी", "मराठी", "ગુજરાતી", "ಕನ್ನಡ", "മലയാളം", "தமிழ்", "తెలుగు", "ଓଡ଼ିଆ", "বাংলা", "English")
        val index = row * 2 + column
        if (index in languages.indices) selectedLanguage = languages[index]
    }

    private fun navigateBottom(x: Float) {
        screen = when {
            x < 88f -> ITantraScreen.Home
            x < 168f -> ITantraScreen.Pairing
            x < 248f -> ITantraScreen.Talk
            else -> ITantraScreen.Settings
        }
    }
}
