package com.example.adproject

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adproject.api.ApiClient
import com.example.adproject.model.RecommendedPractice
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecommendedActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var adapter: RecommendedAdapter

    // 作为 Adapter 的 backing list
    private val items = mutableListOf<RecommendedPractice>()
    private val ui = Handler(Looper.getMainLooper())
    private var isLoading = false
    private var currentRetry = 0
    private val maxRetry = 4
    private val delayMs = 800L

    // 记录已展示过的题目，避免重复
    private val seenIds = mutableSetOf<Int>()

    private val TAG = "RECO"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recommend)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        recycler = findViewById(R.id.recycler)
        progress = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = RecommendedAdapter(items) { item ->
            val itn = Intent(this, ExerciseActivity::class.java)
            itn.putExtra("practice_id", item.id)
            itn.putExtra("practice_title", item.title)
            startActivity(itn)
        }
        recycler.adapter = adapter

        // 底部导航
        findViewById<Button>(R.id.exerciseButton).setOnClickListener {
            startActivity(Intent(this, ExerciseActivity::class.java))
        }
        findViewById<Button>(R.id.dashboardButton).apply {
            isSelected = true
            setOnClickListener { startActivity(Intent(this@RecommendedActivity, DashboardActivity::class.java)) }
        }
        findViewById<Button>(R.id.classButton).setOnClickListener {
            startActivity(Intent(this, ClassActivity::class.java))
        }
        findViewById<Button>(R.id.homeButton).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        seenIds.clear()
        loadRecommendations()
    }

    /** 触发推荐 -> 拉取推荐 ID 列表 */
    private fun loadRecommendations() {
        if (isLoading) return
        isLoading = true
        currentRetry = 0
        showLoading(true)
        errorText.visibility = View.GONE

        ApiClient.dashboardApi.triggerRecommend().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val code = response.body()?.get("code")?.asInt ?: 0
                if (!response.isSuccessful || code != 1) {
                    fail("触发推荐失败：HTTP ${response.code()}")
                    return
                }
                fetchRecommendIdsWithRetry()
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                fail("网络错误（触发推荐）：${t.message}")
            }
        })
    }

    private fun fetchRecommendIdsWithRetry() {
        ApiClient.dashboardApi.getRecommendIds().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (!response.isSuccessful) {
                    if (currentRetry < maxRetry) return scheduleRetry()
                    return fail("获取推荐失败：HTTP ${response.code()}")
                }
                val root = response.body()
                val code = root?.get("code")?.asInt ?: 0
                if (code != 1) {
                    if (currentRetry < maxRetry) return scheduleRetry()
                    return fail(root?.get("msg")?.asString ?: "获取推荐失败")
                }

                val rawIds = extractIds(root)
                val ids = rawIds.distinct().filter { seenIds.add(it) }  // 前端兜底去重
                Log.d(TAG, "推荐ID（原始）: $rawIds")
                Log.d(TAG, "推荐ID（去重后）: $ids")

                if (ids.isEmpty()) {
                    if (currentRetry < maxRetry) return scheduleRetry()
                    return fail("当前没有可用的推荐题目")
                }

                // 只清空一次，通过适配器清空（不要同时清 items 和 adapter 两次）
                adapter.clear()
                fetchQuestionOneByOne(ids, 0)
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                if (currentRetry < maxRetry) return scheduleRetry()
                fail("网络错误（获取推荐）：${t.message}")
            }
        })
    }

    private fun scheduleRetry() {
        currentRetry++
        ui.postDelayed({ fetchRecommendIdsWithRetry() }, delayMs)
    }

    private fun extractIds(root: JsonObject?): List<Int> {
        val data = root?.getAsJsonObject("data") ?: return emptyList()
        val arr: JsonArray? = when {
            data.has("questionIds") -> data.getAsJsonArray("questionIds")
            data.has("ids") -> data.getAsJsonArray("ids")
            else -> null
        }
        val list = mutableListOf<Int>()
        if (arr != null) for (e in arr) list += e.asInt
        return list
    }

    /** 逐个请求题目详情，拿题干 + base64 图片，边到边显示 */
    private fun fetchQuestionOneByOne(ids: List<Int>, index: Int) {
        if (index >= ids.size) {
            showLoading(false)
            isLoading = false
            Toast.makeText(this, "已获取到 ${items.size} 条推荐", Toast.LENGTH_SHORT).show()
            return
        }

        val id = ids[index]
        ApiClient.dashboardApi.getQuestionDetail(id).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, resp: Response<JsonObject>) {
                if (resp.isSuccessful && resp.body()?.get("code")?.asInt == 1) {
                    val data = resp.body()!!.getAsJsonObject("data")
                    val title = when {
                        data.has("question") && !data.get("question").isJsonNull -> data.get("question").asString
                        data.has("title")    && !data.get("title").isJsonNull    -> data.get("title").asString
                        else -> "Question #$id"
                    }
                    val b64 = if (data.has("image") && !data.get("image").isJsonNull)
                        data.get("image").asString else null

                    val item = RecommendedPractice(
                        id = id,
                        title = title,
                        subject = "—",
                        grade = "—",
                        questions = 10,
                        difficulty = "Medium",
                        imageBase64 = b64
                    )
                    // ⚠️ 只通过适配器添加，不要再 items.add
                    adapter.addItem(item)
                }
                fetchQuestionOneByOne(ids, index + 1)
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                val item = RecommendedPractice(
                    id = id,
                    title = "Question #$id",
                    subject = "—",
                    grade = "—",
                    questions = 10,
                    difficulty = "Medium",
                    imageBase64 = null
                )
                // ⚠️ 只通过适配器添加，不要再 items.add
                adapter.addItem(item)
                fetchQuestionOneByOne(ids, index + 1)
            }
        })
    }

    private fun showLoading(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        recycler.alpha = if (show) 0.4f else 1f
    }

    private fun fail(msg: String) {
        isLoading = false
        showLoading(false)
        errorText.text = msg
        errorText.visibility = View.VISIBLE
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
