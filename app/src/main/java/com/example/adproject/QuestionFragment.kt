package com.example.adproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class QuestionFragment : Fragment() {

    private var question: String? = null
    private var imageRes: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            question = it.getString("question")
            imageRes = it.getInt("imageRes")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_question, container, false)

        val questionText = view.findViewById<TextView>(R.id.questionText)
        val questionImage = view.findViewById<ImageView>(R.id.questionImage)
        val confirmBtn = view.findViewById<Button>(R.id.confirmButton)

        questionText.text = question
        questionImage.setImageResource(imageRes)

        confirmBtn.setOnClickListener {
            // 关闭 Fragment 并恢复主界面
            parentFragmentManager.popBackStack()
            (activity as? ExerciseActivity)?.showMainUI()
        }

        return view
    }

    companion object {
        fun newInstance(question: String, imageRes: Int) = QuestionFragment().apply {
            arguments = Bundle().apply {
                putString("question", question)
                putInt("imageRes", imageRes)
            }
        }
    }
}
