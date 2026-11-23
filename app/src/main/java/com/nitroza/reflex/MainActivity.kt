package com.nitroza.reflex

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 12, 22)
    private val panel = Color.rgb(17, 25, 42)
    private val blue = Color.rgb(35, 145, 255)
    private val red = Color.rgb(255, 65, 82)
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

    private var mode = Mode.REFLEX
    private var waiting = false
    private var ready = false
    private var reactionStart = 0L
    private var trials = mutableListOf<Long>()
    private var cpsRunning = false
    private var cpsStart = 0L
    private var cpsDuration = 5
    private var cpsCount = 0
    private var bestCps = 0.0

    private enum class Mode { REFLEX, CPS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        root.addView(space(22))

        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val reflexTab = button("REFLEX", blue) { showMode(Mode.REFLEX) }
        val cpsTab = button("CPS TEST", red) { showMode(Mode.CPS) }
        tabs.addView(reflexTab, weightParams())
        tabs.addView(spaceView(10), LinearLayout.LayoutParams(dp(10), 1))
        tabs.addView(cpsTab, weightParams())
        root.addView(tabs)
        root.addView(space(18))

        status = text("", 14, muted, false)
        root.addView(status)
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

        root.addView(text("CLICK SPEED", 12, red, true).apply { letterSpacing = 0.15f })
        root.addView(space(8))
        durationLabel = text("Duration: 5 seconds", 14, muted, false)
        root.addView(durationLabel)
        root.addView(space(8))

        val durations = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(1, 5, 10, 30, 60).forEach { seconds ->
            val b = button("${seconds}s", if (seconds == cpsDuration) red else panel) {
                if (!cpsRunning) {
                    cpsDuration = seconds
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
        root.addView(space(20))
        root.addView(text("HOW IT WORKS", 12, blue, true).apply { letterSpacing = 0.15f })
        root.addView(space(6))
        root.addView(text(
            "Reflex: wait for the panel to turn green, then tap as quickly as you can. " +
                "Early taps are false starts and won't count. Complete five rounds for your stats.\n\n" +
                "CPS: tap as many times as possible before the timer ends. Choose a test duration above.",
            13, muted, false
        ))
        root.addView(space(12))
        root.addView(text(
            "Timing uses Android's monotonic clock. Screen refresh, touch sampling and device latency can affect results.",
            11, muted, false
        ))
    }

    private fun showMode(newMode: Mode) {
        mode = newMode
        val isReflex = mode == Mode.REFLEX
        status.text = if (isReflex) "REACTION TIME  •  5 ROUNDS" else "CLICKS PER SECOND  •  $cpsDuration SECOND TEST"
        reactionButton.visibility = if (isReflex) View.VISIBLE else View.GONE
        reactionStats.visibility = if (isReflex) View.VISIBLE else View.GONE
        durationLabel.visibility = if (isReflex) View.GONE else View.VISIBLE
        cpsButton.visibility = if (isReflex) View.GONE else View.VISIBLE
        cpsStats.visibility = if (isReflex) View.GONE else View.VISIBLE
        root.getChildAt(7).visibility = if (isReflex) View.GONE else View.VISIBLE
        root.getChildAt(8).visibility = if (isReflex) View.GONE else View.VISIBLE
        root.getChildAt(9).visibility = if (isReflex) View.GONE else View.VISIBLE
        root.getChildAt(10).visibility = if (isReflex) View.GONE else View.VISIBLE
        root.getChildAt(11).visibility = if (isReflex) View.GONE else View.VISIBLE
        if (!isReflex) {
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
                handler.postDelayed({ reactionButton.text = "TAP TO TRY AGAIN"; reactionButton.background = rounded(blue, 18) }, 900)
            }
            ready -> {
                val elapsed = (SystemClock.elapsedRealtimeNanos() - reactionStart) / 1_000_000L
                ready = false
                trials.add(elapsed)
                if (trials.size >= 5) {
                    val sorted = trials.sorted()
                    val average = trials.average().roundToInt()
                    val median = sorted[sorted.size / 2]
                    reactionStats.text = "BEST   ${sorted.first()} ms\nAVERAGE   $average ms\nMEDIAN   $median ms\nROUNDS   5 / 5"
                    reactionButton.text = "RUN AGAIN"
                    status.text = "SESSION COMPLETE"
                } else {
                    reactionStats.text = "Last: $elapsed ms     Round: ${trials.size}/5"
                    reactionButton.text = "$elapsed ms  •  NEXT ROUND"
                    status.text = "Good hit — keep going"
                }
                reactionButton.background = rounded(blue, 18)
            }
            else -> startReactionRound()
        }
    }

    private fun startReactionRound() {
        if (trials.size >= 5) trials.clear()
        waiting = true
        ready = false
        reactionButton.text = "WAIT FOR GREEN..."
        reactionButton.background = rounded(red, 18)
        status.text = "Stay ready — don't tap early"
        val delay = Random.nextLong(1500L, 4501L)
        handler.postDelayed({
            if (waiting && mode == Mode.REFLEX) {
                waiting = false
                ready = true
                reactionStart = SystemClock.elapsedRealtimeNanos()
                reactionButton.text = "TAP NOW!"
                reactionButton.background = rounded(Color.rgb(28, 190, 116), 18)
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
        cpsRunning = false
        cpsButton.text = String.format(Locale.US, "%.2f CPS  •  AGAIN", score)
        status.text = "TEST COMPLETE"
        updateCpsStats(score)
    }

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

    private fun spaceView(width: Int, height: Int) = View(this)
    private fun weightParams() = LinearLayout.LayoutParams(0, dp(46), 1f)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
