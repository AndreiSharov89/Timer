package com.example.timer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private var mainThreadHandler: Handler? = null
    private var editText: EditText? = null
    private var btnStart: Button? = null
    private var btnReset: Button? = null
    private var btnPause: Button? = null
    private var tvTimer: TextView? = null
    var pause: Boolean = false
    var secondsCount: Long = 0
    var remainingTime: Long = 0
    var timerRunnable: Runnable? = null
    private var lastTickTime: Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mainThreadHandler = Handler(Looper.getMainLooper())
        editText = findViewById(R.id.inputText)
        btnStart = findViewById(R.id.btnStart)
        btnReset = findViewById(R.id.btnReset)
        btnPause = findViewById(R.id.btnPause)
        (btnReset as View).visibility = View.GONE
        tvTimer = findViewById(R.id.tvTimer)

        editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                hideKeyboard()
                btnStart?.performClick() // Запустить таймер
                true
            } else {
                false
            }
        }

        btnStart?.setOnClickListener {
            if (pause) {
                pause = !pause
                btnPause?.text = if (pause) "Continue" else "Pause"
            }
            hideKeyboard()
            secondsCount =
                (editText?.text?.toString()?.takeIf { it.isNotBlank() }?.toLong() ?: 0L) * 1000
            if (secondsCount <= 0) {
                showMessage("Не вверный ввод числа")
            } else {
                startTimer(secondsCount)
                btnStart?.isEnabled = false
                (btnReset as View).visibility = View.VISIBLE
                (btnPause as View).visibility = View.VISIBLE
            }
        }

        btnReset?.setOnClickListener {
            pause = false
            btnPause?.text = "Pause"

            timerRunnable?.let {
                mainThreadHandler?.removeCallbacks(it)
            }

            lastTickTime = 0L
            remainingTime = 0L
            timerRunnable = null
            tvTimer?.text = ""
            (btnReset as View).visibility = View.GONE
            (btnPause as View).visibility = View.GONE
            btnStart?.isEnabled = true
        }

        btnPause?.setOnClickListener {
            pause = !pause
            btnPause?.text = if (pause) "Continue" else "Pause"
            if (!pause) {
                lastTickTime = System.currentTimeMillis()
                btnStart?.isEnabled = false
            } else {
                btnStart?.isEnabled = true
            }
        }
    }

    fun startTimer(duration: Long) {
        remainingTime = duration
        updateTimer()
    }


    fun updateTimer() {
        lastTickTime = System.currentTimeMillis()

        timerRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                if (!pause) {
                    val delta = currentTime - lastTickTime
                    remainingTime -= delta
                    lastTickTime = currentTime
                }
                if (remainingTime <= 0 && !pause) {
                    remainingTime = 0L
                    tvTimer?.text = "Конец!"
                    btnStart?.isEnabled = true
                    (btnReset as View).visibility = View.GONE
                    (btnPause as View).visibility = View.GONE
                    showMessage("Время вышло!")
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500)
                    }
                } else {
                    val minutes = remainingTime / 60_000
                    val seconds = (remainingTime % 60_000) / 1_000
                    val millis = remainingTime % 1_000
                    tvTimer?.text = String.format("%d:%02d.%03d", minutes, seconds, millis)
                    mainThreadHandler?.postDelayed(this, DELAY)
                }
            }
        }

        mainThreadHandler?.post(timerRunnable!!)
    }


    fun showMessage(message: String) {
        val rootView = findViewById<View>(android.R.id.content)?.rootView
        if (rootView != null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm =
                getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("remainingTime", remainingTime)
        outState.putBoolean("isPaused", pause)
        outState.putBoolean("isRunning", timerRunnable != null)
        outState.putString("timerText", tvTimer?.text?.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        remainingTime = savedInstanceState.getLong("remainingTime", 0)
        pause = savedInstanceState.getBoolean("isPaused", false)
        val wasRunning = savedInstanceState.getBoolean("isRunning", false)
        tvTimer?.text = savedInstanceState.getString("timerText", "")

        if (wasRunning && remainingTime > 0) {
            btnStart?.isEnabled = false
            (btnReset as View).visibility = View.VISIBLE
            (btnPause as View).visibility = View.VISIBLE
            btnPause?.text = if (pause) "Continue" else "Pause"
            updateTimer()
        }
    }

    companion object {
        private const val DELAY = 50L
    }

}