package com.example.adproject

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adproject.api.ApiClient
import com.example.adproject.model.AnswerRecord
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class AnswerHistoryActivity : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var historyList: RecyclerView
    private lateinit var loading: ProgressBar
    private lateinit var adapter: AnswerHistoryAdapter

    private val allRecords = mutableListOf<AnswerRecord>()

    private val HISTORY_ENDPOINT = "http://10.0.2.2:8080/student/recommend"
    private val ok = OkHttpClient()
    private val TAG = "AnswerHistory"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 开启 edge-to-edge，让我们手动适配状态栏安全区
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_answer_history)

        // 处理顶部 header 的安全区域（防止打孔/状态栏遮挡）
        val header = findViewById<View>(R.id.header)
        val initialTopPadding = header.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, initialTopPadding + topInset, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        spinner = findViewById(R.id.filterSpinner)
        historyList = findViewById(R.id.historyList)
        loading = findViewById(R.id.loading)

        historyList.layoutManager = LinearLayoutManager(this)
        adapter = AnswerHistoryAdapter(mutableListOf())
        historyList.adapter = adapter

        val opts = listOf("All", "Correct", "Wrong")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, opts)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        fetchHistory()
    }

    /** 第一步：获取历史（只有 id + 正误） */
    private fun fetchHistory() {
        loading.visibility = View.VISIBLE
        val media = "application/json; charset=utf-8".toMediaType()
        val body = "{}".toRequestBody(media)

        val req = Request.Builder()
            .url(HISTORY_ENDPOINT)
            .put(body)  // 后端定义为 PUT
            .build()

        ok.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    loading.visibility = View.GONE
                    Toast.makeText(this@AnswerHistoryActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    val text = it.body?.string().orEmpty()
                    try {
                        val root = JSONObject(text)
                        val arr = root.getJSONObject("data").getJSONArray("records")
                        allRecords.clear()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val qid = obj.optInt("questionId")
                            val correct = obj.optInt("isCorrect", 0) == 1
                            allRecords += AnswerRecord(qid, correct, title = null, imageBase64 = null)
                        }
                        runOnUiThread {
                            applyFilter()                  // 先显示
                            fetchTitlesSequentially(0)     // 逐个补题干/图片
                        }
                    } catch (ex: Exception) {
                        runOnUiThread {
                            loading.visibility = View.GONE
                            Toast.makeText(this@AnswerHistoryActivity, "Parse error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    /** 第二步：逐个请求题干 + 图片 */
    private fun fetchTitlesSequentially(index: Int) {
        if (index >= allRecords.size) {
            loading.visibility = View.GONE
            return
        }
        val rec = allRecords[index]

        ApiClient.dashboardApi.getQuestionDetail(rec.questionId)
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, resp: Response<JsonObject>) {
                    if (resp.isSuccessful && resp.body()?.get("code")?.asInt == 1) {
                        val data = resp.body()!!.getAsJsonObject("data")
                        val title = when {
                            data.has("question") && !data.get("question").isJsonNull -> data.get("question").asString
                            data.has("title")    && !data.get("title").isJsonNull    -> data.get("title").asString
                            else -> "Question #${rec.questionId}"
                        }
                        val img = when {
                            data.has("image") && !data.get("image").isJsonNull -> data.get("image").asString
                            data.has("img")   && !data.get("img").isJsonNull   -> data.get("img").asString
                            else -> null
                        }

                        rec.title = title
                        rec.imageBase64 = img
                        adapter.updateTitleAndImage(rec.questionId, title, img)
                    }
                    fetchTitlesSequentially(index + 1)
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.w(TAG, "title failed qid=${rec.questionId} ${t.message}")
                    fetchTitlesSequentially(index + 1)
                }
            })
    }

    /** 根据筛选刷新显示 */
    private fun applyFilter() {
        val pos = spinner.selectedItemPosition // 0 All, 1 Correct, 2 Wrong
        val filtered = when (pos) {
            1 -> allRecords.filter { it.isCorrect }
            2 -> allRecords.filter { !it.isCorrect }
            else -> allRecords
        }
        adapter.submitList(filtered)
    }
}
