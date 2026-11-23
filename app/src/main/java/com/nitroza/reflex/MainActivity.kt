package com.nitroza.reflex

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : Activity() {
    private var bg = Color.rgb(8, 12, 22)
    private var panel = Color.rgb(17, 25, 42)
    private var blue = Color.rgb(35, 145, 255)
    private var red = Color.rgb(255, 65, 82)
    private var themeIndex = 0
    private var delayMin = 1500L
    private var delayMax = 4500L
    private val palettes = listOf(
        intArrayOf(Color.rgb(8,12,22), Color.rgb(17,25,42), Color.rgb(35,145,255), Color.rgb(255,65,82)),
        intArrayOf(Color.rgb(12,10,24), Color.rgb(31,22,48), Color.rgb(164,104,255), Color.rgb(255,75,150)),
        intArrayOf(Color.rgb(7,20,19), Color.rgb(15,42,38), Color.rgb(35,205,150), Color.rgb(255,170,55)),
        intArrayOf(Color.rgb(22,13,9), Color.rgb(48,27,18), Color.rgb(255,155,55), Color.rgb(255,75,55)),
        intArrayOf(Color.rgb(12,15,18), Color.rgb(29,35,42), Color.rgb(180,205,220), Color.rgb(80,190,255)),
        intArrayOf(Color.rgb(20,9,14), Color.rgb(48,18,29), Color.rgb(255,70,110), Color.rgb(255,190,65))
    )
    private val muted = Color.rgb(151, 166, 190)
    private val white = Color.WHITE
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var reactionButton: TextView
    private lateinit var reactionStats: TextView
    private lateinit var cpsButton: TextView
    private lateinit var cpsStats: TextView
    private lateinit var durationLabel: TextView
    private lateinit var cpsSection: LinearLayout
    private lateinit var historyView: TextView
    private var soundEnabled = true
    private var vibrationEnabled = true

    private var mode = Mode.REFLEX
    private var waiting = false
    private var ready = false
    private var reactionStart = 0L
    private var trials = mutableListOf<Long>()
    private var cpsRunning = false
    private var cpsStart = 0L
    private var cpsDuration = 5
    private var targetRounds = 5
    private var cpsCount = 0
    private var bestCps = 0.0

    private enum class Mode { REFLEX, CPS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themeIndex = getPreferences(MODE_PRIVATE).getInt("theme", 0).coerceIn(0, palettes.lastIndex)
        soundEnabled = getPreferences(MODE_PRIVATE).getBoolean("sound", true)
        vibrationEnabled = getPreferences(MODE_PRIVATE).getBoolean("vibration", true)
        targetRounds = getPreferences(MODE_PRIVATE).getInt("rounds", 5).coerceIn(3, 10)
        delayMin = getPreferences(MODE_PRIVATE).getLong("delayMin", 1500L)
        delayMax = getPreferences(MODE_PRIVATE).getLong("delayMax", 4500L)
        bestCps = getPreferences(MODE_PRIVATE).getFloat("bestCps", 0f).toDouble()
        cpsDuration = getPreferences(MODE_PRIVATE).getInt("cpsDuration", 5).coerceIn(1, 60)
        applyPalette()
        buildUi()
        showMode(Mode.REFLEX)
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        val brand = text("NITRO", 13, blue, true).apply { letterSpacing = 0.22f }
        root.addView(brand)
        root.addView(text("ZA REFLEX", 30, white, true).apply {
            letterSpacing = 0.04f
            setPadding(0, dp(2), 0, dp(4))
        })
        root.addView(text("Train your timing. Track your speed.", 14, muted, false))
        root.addView(space(12))
        root.addView(text("PERSONALIZE YOUR LOOK", 11, muted, true))
        val themeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val themeNames = listOf("NEON", "VIOLET", "MINT", "EMBER", "ICE", "CRIMSON")
        themeNames.forEachIndexed { index, name ->
            val swatch = button(name, palettes[index][2]) {
                themeIndex = index
                getPreferences(MODE_PRIVATE).edit().putInt("theme", index).apply()
                recreate()
            }
            themeRow.addView(swatch, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(4) })
        }
        root.addView(themeRow)
        root.addView(space(10))
        val settingsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val soundButton = button(if (soundEnabled) "SOUND: ON" else "SOUND: OFF", panel) {
            soundEnabled = !soundEnabled
            getPreferences(MODE_PRIVATE).edit().putBoolean("sound", soundEnabled).apply()
            recreate()
        }
        val vibrationButton = button(if (vibrationEnabled) "VIBRATION: ON" else "VIBRATION: OFF", panel) {
            vibrationEnabled = !vibrationEnabled
            getPreferences(MODE_PRIVATE).edit().putBoolean("vibration", vibrationEnabled).apply()
            recreate()
        }
        settingsRow.addView(soundButton, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(6) })
        settingsRow.addView(vibrationButton, LinearLayout.LayoutParams(0, dp(42), 1f))
        root.addView(settingsRow)
        root.addView(space(18))

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val reflexTab = button("REFLEX", blue) { showMode(Mode.REFLEX) }
        val cpsTab = button("CPS TEST", red) { showMode(Mode.CPS) }
        tabs.addView(reflexTab, weightParams())
        tabs.addView(spaceView(10, 1), LinearLayout.LayoutParams(dp(10), 1))
        tabs.addView(cpsTab, weightParams())
        root.addView(tabs)
        root.addView(space(18))

        status = text("", 14, muted, false)
        root.addView(status)
        root.addView(space(8))
        root.addView(text("RANDOM START DELAY", 11, muted, true))
        val delayRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(1500L to 4500L, 2000L to 6000L, 3000L to 7000L, 5000L to 10000L).forEachIndexed { index, range ->
            val labels = listOf("1.5–4.5s", "2–6s", "3–7s", "5–10s")
            val b = button(labels[index], panel) {
                delayMin = range.first
                delayMax = range.second
                getPreferences(MODE_PRIVATE).edit().putLong("delayMin", delayMin).putLong("delayMax", delayMax).apply()
                status.text = "Random delay set: ${labels[index]}"
            }
            delayRow.addView(b, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(4) })
        }
        root.addView(delayRow)
        root.addView(space(8))
        root.addView(text("REFLEX SESSION LENGTH", 11, muted, true))
        val roundsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(3, 5, 10).forEach { rounds ->
            val b = button("$rounds ROUNDS", if (rounds == targetRounds) blue else panel) {
                targetRounds = rounds
                getPreferences(MODE_PRIVATE).edit().putInt("rounds", rounds).apply()
                status.text = "Session length set to $rounds rounds"
            }
            roundsRow.addView(b, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(5) })
        }
        root.addView(roundsRow)
        root.addView(space(8))

        reactionButton = text("TAP TO START", 22, white, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(34), dp(12), dp(34))
            background = rounded(blue, 18)
            setOnClickListener { onReactionTap() }
        }
        root.addView(reactionButton, LinearLayout.LayoutParams(-1, dp(190)))
        root.addView(space(14))
        reactionStats = text("No attempts yet", 14, muted, false)
        root.addView(card(reactionStats))
        root.addView(space(22))

        cpsSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            root.addView(text("CLICK SPEED", 12, red, true).apply { letterSpacing = 0.15f })
            root.addView(space(8))
            durationLabel = text("Duration: 5 seconds", 14, muted, false)
            root.addView(durationLabel)
            root.addView(space(8))

            val durations = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            listOf(1, 5, 10, 30, 60).forEach { seconds ->
                val b = button("${seconds}s", if (seconds == cpsDuration) red else panel) {
                    if (!cpsRunning) {
                        cpsDuration = seconds
                        getPreferences(MODE_PRIVATE).edit().putInt("cpsDuration", seconds).apply()
                        durationLabel.text = "Duration: $cpsDuration seconds"
                        updateCpsStats()
                    }
                }
                durations.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f).apply {
                    marginEnd = dp(5)
                })
            }
            root.addView(durations)
            root.addView(space(10))

            cpsButton = text("TAP TO START", 20, white, true).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(26), dp(12), dp(26))
                background = rounded(red, 18)
                setOnClickListener { onCpsTap() }
            }
            root.addView(cpsButton, LinearLayout.LayoutParams(-1, dp(142)))
            root.addView(space(12))
            cpsStats = text("Ready when you are", 14, muted, false)
            root.addView(card(cpsStats))
        }
        root.addView(cpsSection)
        root.addView(space(20))
        root.addView(text("HOW IT WORKS", 12, blue, true).apply { letterSpacing = 0.15f })
        root.addView(space(6))
        root.addView(text(
            "Reflex: wait for the panel to turn green, then tap as quickly as you can. " +
                "Early taps are false starts and won't count. Choose 3, 5 or 10 rounds for a session summary.\n\n" +
                "CPS: tap as many times as possible before the timer ends. Choose a test duration above.",
            13, muted, false
        ))
        root.addView(space(12))
        root.addView(text(
            "Timing uses Android's monotonic clock. Screen refresh, touch sampling and device latency can affect results.",
            11, muted, false
        ))
        root.addView(space(18))
        root.addView(text("SESSION HISTORY", 12, blue, true))
        root.addView(space(6))
        historyView = text(loadHistory(), 13, muted, false)
        root.addView(card(historyView))
        root.addView(space(8))
        root.addView(button("CLEAR HISTORY", panel) {
            getPreferences(MODE_PRIVATE).edit().remove("history").apply()
            historyView.text = "No saved sessions yet"
        })
    }

    private fun applyPalette() {
        val p = palettes[themeIndex]
        bg = p[0]; panel = p[1]; blue = p[2]; red = p[3]
    }

    private fun showMode(newMode: Mode) {
        mode = newMode
        val isReflex = mode == Mode.REFLEX
        status.text = if (isReflex) "REACTION TIME  •  $targetRounds ROUNDS" else "CLICKS PER SECOND  •  $cpsDuration SECOND TEST"
        reactionButton.visibility = if (isReflex) View.VISIBLE else View.GONE
        reactionStats.visibility = if (isReflex) View.VISIBLE else View.GONE
        cpsSection.visibility = if (isReflex) View.GONE else View.VISIBLE
        if (!isReflex) {
            waiting = false
            ready = false
            reactionButton.text = "TAP TO START"
        }
    }

    private fun onReactionTap() {
        if (mode != Mode.REFLEX) return
        when {
            waiting -> {
                handler.removeCallbacksAndMessages(null)
                waiting = false
                ready = false
                reactionButton.text = "FALSE START"
                reactionButton.setBackgroundColor(red)
                status.text = "Too early — wait for green"
                feedback(false)
                handler.postDelayed({ reactionButton.text = "TAP TO TRY AGAIN"; reactionButton.background = rounded(blue, 18) }, 900)
            }
            ready -> {
                val elapsed = (SystemClock.elapsedRealtimeNanos() - reactionStart) / 1_000_000L
                ready = false
                trials.add(elapsed)
                feedback(true)
                if (trials.size >= targetRounds) {
                    val sorted = trials.sorted()
                    val average = trials.average().roundToInt()
                    val median = sorted[sorted.size / 2]
                    reactionStats.text = "BEST   ${sorted.first()} ms\nAVERAGE   $average ms\nMEDIAN   $median ms\nROUNDS   $targetRounds / $targetRounds"
                    saveHistory("REFLEX  •  Best ${sorted.first()} ms  •  Avg $average ms")
                    feedback(true)
                    reactionButton.text = "RUN AGAIN"
                    status.text = "SESSION COMPLETE"
                } else {
                    reactionStats.text = "Last: $elapsed ms     Round: ${trials.size}/$targetRounds"
                    reactionButton.text = "$elapsed ms  •  NEXT ROUND"
                    status.text = "Good hit — keep going"
                }
                reactionButton.background = rounded(blue, 18)
            }
            else -> startReactionRound()
        }
    }

    private fun startReactionRound() {
        if (trials.size >= targetRounds) trials.clear()
        waiting = true
        ready = false
        reactionButton.text = "WAIT FOR GREEN..."
        reactionButton.background = rounded(red, 18)
        status.text = "Stay ready — don't tap early"
        val delay = Random.nextLong(delayMin, delayMax + 1L)
        handler.postDelayed({
            if (waiting && mode == Mode.REFLEX) {
                waiting = false
                ready = true
                reactionStart = SystemClock.elapsedRealtimeNanos()
                reactionButton.text = "TAP NOW!"
                reactionButton.background = rounded(Color.rgb(28, 190, 116), 18)
                feedback(true)
            }
        }, delay)
    }

    private fun onCpsTap() {
        if (mode != Mode.CPS) return
        if (!cpsRunning) {
            cpsRunning = true
            cpsCount = 1
            cpsStart = SystemClock.elapsedRealtimeNanos()
            cpsButton.text = "TAP!  •  1"
            status.text = "GO! Keep tapping"
            handler.postDelayed({ finishCps() }, cpsDuration * 1000L)
        } else {
            cpsCount++
            cpsButton.text = "TAP!  •  $cpsCount"
        }
    }

    private fun finishCps() {
        if (!cpsRunning) return
        val elapsed = (SystemClock.elapsedRealtimeNanos() - cpsStart) / 1_000_000_000.0
        val score = cpsCount / elapsed
        bestCps = maxOf(bestCps, score)
        getPreferences(MODE_PRIVATE).edit().putFloat("bestCps", bestCps.toFloat()).apply()
        saveHistory("CPS  •  ${String.format(Locale.US, "%.2f", score)}  •  $cpsDuration sec  •  $cpsCount taps")
        feedback(true)
        cpsRunning = false
        cpsButton.text = String.format(Locale.US, "%.2f CPS  •  AGAIN", score)
        status.text = "TEST COMPLETE"
        updateCpsStats(score)
    }

    private fun feedback(success: Boolean) {
        if (soundEnabled) {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
                tone.startTone(if (success) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_NACK, 85)
                handler.postDelayed({ tone.release() }, 150)
            } catch (_: Exception) { }
        }
        if (vibrationEnabled) {
            try {
                @Suppress("DEPRECATION")
                (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(if (success) 28L else 90L)
            } catch (_: Exception) { }
        }
    }

    private fun saveHistory(entry: String) {
        val prefs = getPreferences(MODE_PRIVATE)
        val old = prefs.getString("history", "") ?: ""
        val updated = (listOf(entry) + old.split("\n").filter { it.isNotBlank() }).take(12).joinToString("\n")
        prefs.edit().putString("history", updated).apply()
        if (::historyView.isInitialized) historyView.text = updated
    }

    private fun loadHistory(): String =
        getPreferences(MODE_PRIVATE).getString("history", null) ?: "No saved sessions yet"

    private fun updateCpsStats(last: Double? = null) {
        val lastText = last?.let { String.format(Locale.US, "%.2f CPS", it) } ?: "—"
        cpsStats.text = "LAST   $lastText\nPERSONAL BEST   ${String.format(Locale.US, "%.2f", bestCps)} CPS"
    }

    private fun card(child: View): LinearLayout = LinearLayout(this).apply {
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = rounded(panel, 14)
        addView(child)
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setLineSpacing(dp(3).toFloat(), 1f)
        }

    private fun button(label: String, color: Int, action: () -> Unit): TextView =
        text(label, 12, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(color, 12)
            setPadding(dp(8), dp(12), dp(8), dp(12))
            setOnClickListener { action() }
        }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun spaceView(width: Int, height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(width), dp(height))
    }
    private fun weightParams() = LinearLayout.LayoutParams(0, dp(46), 1f)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
