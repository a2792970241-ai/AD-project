package com.example.adproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class DashboardActivity : AppCompatActivity() {

    private lateinit var chartView: LineChart
    private lateinit var last7Days: TextView
    private var accuracyRates: List<Float> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 底部导航
        val exerciseButton = findViewById<Button>(R.id.exerciseButton)
        val dashboardButton = findViewById<Button>(R.id.dashboardButton)
        val classButton = findViewById<Button>(R.id.classButton)
        val homeButton = findViewById<Button>(R.id.homeButton)

        setSelectedButton(dashboardButton)

        exerciseButton.setOnClickListener {
            setSelectedButton(exerciseButton)
            startActivity(Intent(this, ExerciseActivity::class.java))
        }
        dashboardButton.setOnClickListener {
            setSelectedButton(dashboardButton)
            // 保持在当前页
        }
        classButton.setOnClickListener {
            setSelectedButton(classButton)
            startActivity(Intent(this, ClassActivity::class.java))
        }
        homeButton.setOnClickListener {
            setSelectedButton(homeButton)
            startActivity(Intent(this, HomeActivity::class.java))
        }

        // ====== 新增：Answer History 跳转（保持你现有功能不变）======
        findViewById<Button>(R.id.answerHistoryButton).setOnClickListener {
            startActivity(Intent(this, AnswerHistoryActivity::class.java))
        }
        // ====== 已有：Recommended Practice 跳转 ====================
        findViewById<Button>(R.id.recommendedPracticeButton).setOnClickListener {
            startActivity(Intent(this, RecommendedActivity::class.java))
        }
        // =========================================================

        // 图表初始化
        chartView = findViewById(R.id.chartView)
        setupChart()

        last7Days = findViewById(R.id.last7Days)
        last7Days.setOnClickListener { showDayOptions() }

        // 发起请求
        fetchAccuracyRatesFromServer()
    }

    private fun setupChart() {
        chartView.description.isEnabled = false
        chartView.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chartView.xAxis.granularity = 1f
        chartView.axisRight.isEnabled = false
        chartView.axisLeft.axisMinimum = 0f
        chartView.axisLeft.axisMaximum = 100f  // 百分比
    }

    private fun updateChartForDays(days: Int) {
        val recentRates = accuracyRates.take(days).reversed()

        Log.d("ChartUpdate", "展示最近 $days 天数据: $recentRates")

        if (recentRates.isEmpty()) {
            Toast.makeText(this, "暂无图表数据", Toast.LENGTH_SHORT).show()
            chartView.clear()
            return
        }

        val entries = recentRates.mapIndexed { index, value ->
            Entry(index.toFloat(), value * 100)  // 显示百分比
        }

        val dataSet = LineDataSet(entries, "Accuracy").apply {
            color = getColor(R.color.black)
            valueTextColor = getColor(R.color.black)
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(true)
            setDrawCircles(true)
        }

        chartView.data = LineData(dataSet)
        chartView.invalidate()
    }

    private fun showDayOptions() {
        val options = arrayOf("Last 3 Days", "Last 5 Days", "Last 7 Days")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select Duration")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    last7Days.text = "Last 3 Days ▼"
                    updateChartForDays(3)
                }
                1 -> {
                    last7Days.text = "Last 5 Days ▼"
                    updateChartForDays(5)
                }
                2 -> {
                    last7Days.text = "Last 7 Days ▼"
                    updateChartForDays(7)
                }
            }
        }
        builder.show()
    }

    private fun fetchAccuracyRatesFromServer() {
        val client = OkHttpClient()
        val request = Request.Builder()
            // 模拟器访问本机服务必须用 10.0.2.2
            .url("http://10.0.2.2:8080/student/dashboard")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "请求失败，使用测试数据", Toast.LENGTH_SHORT).show()
                    Log.e("Dashboard", "请求失败", e)
                    // fallback 测试数据（7 天）
                    accuracyRates = listOf(0.5f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
                    updateChartForDays(7)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string()
                Log.d("Dashboard", "响应内容：$jsonString")

                try {
                    val json = JSONObject(jsonString!!)
                    val dataObj = json.getJSONObject("data")
                    val ratesArray = dataObj.getJSONArray("accuracyRates")

                    val result = mutableListOf<Float>()
                    for (i in 0 until ratesArray.length()) {
                        result.add(ratesArray.getDouble(i).toFloat())
                    }

                    runOnUiThread {
                        accuracyRates = result
                        Log.d("Dashboard", "解析成功：$accuracyRates")
                        updateChartForDays(7)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@DashboardActivity, "解析失败，使用测试数据", Toast.LENGTH_SHORT).show()
                        Log.e("Dashboard", "解析失败", e)
                        accuracyRates = listOf(0.6f, 0.7f, 0.3f, 0.9f, 0.5f, 0.8f, 1.0f)
                        updateChartForDays(7)
                    }
                }
            }
        })
    }

    private fun setSelectedButton(selectedButton: Button) {
        findViewById<Button>(R.id.exerciseButton).isSelected = false
        findViewById<Button>(R.id.dashboardButton).isSelected = false
        findViewById<Button>(R.id.classButton).isSelected = false
        findViewById<Button>(R.id.homeButton).isSelected = false

        selectedButton.isSelected = true
    }
}
