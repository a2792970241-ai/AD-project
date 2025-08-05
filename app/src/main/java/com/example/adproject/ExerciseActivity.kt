package com.example.adproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ExerciseActivity : AppCompatActivity() {

    private lateinit var questionList: ListView
    private lateinit var questions: List<Pair<String, Int>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_exercise)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 筛选按钮
        val gradeButton = findViewById<Button>(R.id.gradeButton)
        val subjectButton = findViewById<Button>(R.id.subjectButton)
        val categoryButton = findViewById<Button>(R.id.categoryButton)
        val topicButton = findViewById<Button>(R.id.topicButton)

        // 卡片列表
        questionList = findViewById(R.id.questionList)

        // 模拟数据
        questions = listOf(
            Pair("Which of these states is farthest north?", R.drawable.us_map),
            Pair("Identify the question that Tom and Justin's experiment can ...", R.drawable.catapult),
            Pair("Identify the question that Kathleen and Bryant's ...", R.drawable.skiing),
            Pair("What is the probability that a goat produced by this cross will ...", R.drawable.punnett_square),
            Pair("Compare the average kinetic energies of the particles in each ...", R.drawable.particle_samples)
        )

        // 自定义适配器
        val adapter = QuestionAdapter(this, questions)
        questionList.adapter = adapter

        // 📌 点击题目时加载 Fragment
        questionList.setOnItemClickListener { _, _, position, _ ->
            val question = questions[position].first
            val imageRes = questions[position].second

            // ✅ 隐藏主界面元素
            findViewById<View>(R.id.searchCard).visibility = View.GONE
            findViewById<View>(R.id.filterCard).visibility = View.GONE
            questionList.visibility = View.GONE
            findViewById<View>(R.id.fragmentContainer).visibility = View.VISIBLE

            // 加载 Fragment
            val fragment = QuestionFragment.newInstance(question, imageRes)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        // 筛选弹窗
        val filterOptions = arrayOf("Option 1", "Option 2", "Option 3", "Option 4")
        val selectedItems = BooleanArray(filterOptions.size)

        gradeButton.setOnClickListener {
            showMultiChoiceDialog("Grade", filterOptions, selectedItems)
        }
        subjectButton.setOnClickListener {
            showMultiChoiceDialog("Subject", filterOptions, selectedItems)
        }
        categoryButton.setOnClickListener {
            showMultiChoiceDialog("Category", filterOptions, selectedItems)
        }
        topicButton.setOnClickListener {
            showMultiChoiceDialog("Topic", filterOptions, selectedItems)
        }

        // 底部导航栏
        val exerciseButton = findViewById<Button>(R.id.exerciseButton)
        val dashboardButton = findViewById<Button>(R.id.dashboardButton)
        val classButton = findViewById<Button>(R.id.classButton)
        val homeButton = findViewById<Button>(R.id.homeButton)

        // 默认选中 Exercise
        setSelectedButton(exerciseButton)

        exerciseButton.setOnClickListener {
            setSelectedButton(exerciseButton)
            // 保持当前页面
        }
        dashboardButton.setOnClickListener {
            setSelectedButton(dashboardButton)
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        classButton.setOnClickListener {
            setSelectedButton(classButton)
            startActivity(Intent(this, ClassActivity::class.java))
        }
        homeButton.setOnClickListener {
            setSelectedButton(homeButton)
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    private fun showMultiChoiceDialog(title: String, options: Array<String>, selectedItems: BooleanArray) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMultiChoiceItems(options, selectedItems) { _, which, isChecked ->
                selectedItems[which] = isChecked
            }
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setSelectedButton(selectedButton: Button) {
        findViewById<Button>(R.id.exerciseButton).isSelected = false
        findViewById<Button>(R.id.dashboardButton).isSelected = false
        findViewById<Button>(R.id.classButton).isSelected = false
        findViewById<Button>(R.id.homeButton).isSelected = false

        selectedButton.isSelected = true
    }

    // ✅ 提供外部调用方法，用于恢复主界面
    fun showMainUI() {
        findViewById<View>(R.id.searchCard).visibility = View.VISIBLE
        findViewById<View>(R.id.filterCard).visibility = View.VISIBLE
        questionList.visibility = View.VISIBLE
        findViewById<View>(R.id.fragmentContainer).visibility = View.GONE
    }
}
