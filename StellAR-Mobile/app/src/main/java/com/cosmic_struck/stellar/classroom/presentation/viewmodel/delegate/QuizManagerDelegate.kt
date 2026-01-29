package com.cosmic_struck.stellar.classroom.presentation.viewmodel.delegate

import com.cosmic_struck.stellar.classroom.data.dto.Quiz
import com.cosmic_struck.stellar.classroom.domain.model.QuizResult
import javax.inject.Inject

class QuizManagerDelegate constructor(
    val quizData: List<Quiz>
) {
    var currentIndex = 0
    var totalQuestion = quizData.size
    var selectedAnswers = mutableMapOf<Int,String>()
    var startTime = System.currentTimeMillis()

    fun getCurrentQuestion() : Quiz{
        return quizData[currentIndex]
    }
    fun selectAnswer(answer: String) {
        selectedAnswers[currentIndex] = answer
    }

    fun hasAnswered(): Boolean {
        return selectedAnswers.containsKey(currentIndex)
    }

    fun getSelectedAnswer(): String? {
        return selectedAnswers[currentIndex]
    }

    fun nextQuestion(): Boolean {
        return ++currentIndex < quizData.size
    }

    fun calculateResult(): QuizResult {
        val correct = selectedAnswers.count { (index, answer) ->
            quizData[index].correct_answer == answer
        }
        val total = quizData.size
        val accuracy = (correct * 100) / total
        val timeSeconds = (System.currentTimeMillis() - startTime) / 1000
        val xp = (correct * 10) + ((total - correct) * 5)

        return QuizResult(correct, total, accuracy, timeSeconds, xp)
    }

    fun reset() {
        currentIndex = 0
        selectedAnswers.clear()
        startTime = System.currentTimeMillis()
    }

}