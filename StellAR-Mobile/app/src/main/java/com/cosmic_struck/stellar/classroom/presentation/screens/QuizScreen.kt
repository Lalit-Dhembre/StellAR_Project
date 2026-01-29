package com.cosmic_struck.stellar.classroom.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmic_struck.stellar.R
import com.cosmic_struck.stellar.classroom.domain.model.QuizResult
import com.cosmic_struck.stellar.classroom.presentation.viewmodel.ClassroomViewModel
import com.cosmic_struck.stellar.common.util.Rajdhani
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    backToHome: () -> Unit,
    viewModel: ClassroomViewModel,
    modifier: Modifier = Modifier
) {
    val quizManager = viewModel.initializeQuizManager()
    val quizData = quizManager.quizData
    
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var showResults by remember { mutableStateOf(false) }
    var isReviewMode by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var showExitDialog by remember { mutableStateOf(false) }
    
    // Track user answers locally for proper state management
    val userAnswers = remember { mutableStateMapOf<Int, String>() }
    val hasAnswered = userAnswers.containsKey(currentQuestionIndex)

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "End Quiz?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to end this quiz? Your progress will be lost.",
                    color = Color(0xFFb0b0d0)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        quizManager.reset()
                        userAnswers.clear()
                        backToHome()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350)
                    )
                ) {
                    Text("Yes, End Quiz", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3a3a5e)
                    )
                ) {
                    Text("Continue Quiz", color = Color.White)
                }
            },
            containerColor = Color(0xFF2a2a4e),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFb0b0d0)
        )
    }

    // Timer effect
    LaunchedEffect(showResults, isReviewMode) {
        if (!showResults && !isReviewMode) {
            while (true) {
                delay(1000)
                elapsedTime = (System.currentTimeMillis() - quizManager.startTime) / 1000
            }
        }
    }

    val progress by animateFloatAsState(
        targetValue = (currentQuestionIndex + 1).toFloat() / quizData.size.toFloat(),
        animationSpec = tween(300),
        label = "progress"
    )

    // Calculate result based on local state
    fun calculateResult(): QuizResult {
        val correct = userAnswers.count { (index, answer) ->
            quizData.getOrNull(index)?.correct_answer == answer
        }
        val total = quizData.size
        val accuracy = if (total > 0) (correct * 100) / total else 0
        val timeSeconds = elapsedTime
        val xp = (correct * 10) + ((total - correct) * 5)
        return QuizResult(correct, total, accuracy, timeSeconds.toLong(), xp)
    }

    if (showResults && !isReviewMode) {
        QuizResultScreen(
            result = calculateResult(),
            onNextClick = {
                quizManager.reset()
                userAnswers.clear()
                currentQuestionIndex = 0
                showResults = false
                backToHome()
            },
            onReviewClick = {
                isReviewMode = true
                currentQuestionIndex = 0
            }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color(0xFF1a1a3e),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isReviewMode) "Review: ${currentQuestionIndex + 1} of ${quizData.size}"
                                       else "Question ${currentQuestionIndex + 1} of ${quizData.size}",
                                fontFamily = Rajdhani,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isReviewMode) {
                                isReviewMode = false
                                showResults = true
                            } else {
                                // Show confirmation dialog before exiting quiz
                                showExitDialog = true
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.back),
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        if (!isReviewMode) {
                            // Timer display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Text(
                                    text = "⏱",
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatTime(elapsedTime),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            // Review mode indicator
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF7C4DFF))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Review",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1a1a3e)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color(0xFF1a1a3e))
            ) {
                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (isReviewMode) Color(0xFFffd700) else Color(0xFF7C4DFF),
                    trackColor = Color(0xFF3a3a5e)
                )

                // Quiz content
                if (quizData.isNotEmpty() && currentQuestionIndex < quizData.size) {
                    val currentQuestion = quizData[currentQuestionIndex]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Question number badge
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isReviewMode) Color(0xFFffd700) else Color(0xFF7C4DFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${currentQuestionIndex + 1}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isReviewMode) Color.Black else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Question text
                        Text(
                            text = currentQuestion.question,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Rajdhani,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 30.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Options - different display for review mode
                        if (isReviewMode) {
                            ReviewOptions(
                                options = currentQuestion.options,
                                correctAnswer = currentQuestion.correct_answer,
                                userAnswer = userAnswers[currentQuestionIndex]
                            )
                        } else {
                            QuizOptionsInternal(
                                options = currentQuestion.options,
                                selectedOption = userAnswers[currentQuestionIndex],
                                onSelectAnswer = { answer ->
                                    userAnswers[currentQuestionIndex] = answer
                                    quizManager.selectAnswer(answer)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Navigation buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Previous button (for review mode or when not on first question)
                            if (currentQuestionIndex > 0) {
                                Button(
                                    onClick = { currentQuestionIndex-- },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3a3a5e)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "Previous",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            // Next/Finish button
                            Button(
                                onClick = {
                                    if (currentQuestionIndex < quizData.size - 1) {
                                        currentQuestionIndex++
                                    } else {
                                        if (isReviewMode) {
                                            isReviewMode = false
                                            showResults = true
                                        } else {
                                            showResults = true
                                        }
                                    }
                                },
                                enabled = isReviewMode || hasAnswered,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isReviewMode || hasAnswered) 
                                        Color(0xFF7C4DFF) else Color(0xFF3a3a5e),
                                    disabledContainerColor = Color(0xFF3a3a5e)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = when {
                                        isReviewMode && currentQuestionIndex == quizData.size - 1 -> "Done"
                                        currentQuestionIndex == quizData.size - 1 -> "Finish Quiz"
                                        else -> "Next"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No questions available",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizOptionsInternal(
    options: List<String>,
    selectedOption: String?,
    onSelectAnswer: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedOption == option
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFF4a4a7e) else Color(0xFF2a2a4e))
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Color(0xFF7C4DFF) else Color(0xFF3a3a5e),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectAnswer(option) }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF7C4DFF) else Color(0xFF3a3a5e)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ('A' + index).toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = option,
                        color = Color.White,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C4DFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewOptions(
    options: List<String>,
    correctAnswer: String,
    userAnswer: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isCorrect = option == correctAnswer
            val isUserAnswer = option == userAnswer
            val isWrongUserAnswer = isUserAnswer && !isCorrect
            
            val backgroundColor = when {
                isCorrect -> Color(0xFF1B5E20) // Green for correct
                isWrongUserAnswer -> Color(0xFFB71C1C) // Red for wrong user answer
                else -> Color(0xFF2a2a4e)
            }
            
            val borderColor = when {
                isCorrect -> Color(0xFF4CAF50)
                isWrongUserAnswer -> Color(0xFFEF5350)
                else -> Color(0xFF3a3a5e)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCorrect -> Color(0xFF4CAF50)
                                    isWrongUserAnswer -> Color(0xFFEF5350)
                                    else -> Color(0xFF3a3a5e)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isCorrect -> "✓"
                                isWrongUserAnswer -> "✗"
                                else -> ('A' + index).toString()
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = option,
                        color = Color.White,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )

                    // Label for correct answer and user's choice
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        if (isCorrect) {
                            Text(
                                text = "Correct",
                                color = Color(0xFF81C784),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isUserAnswer) {
                            Text(
                                text = "Your Answer",
                                color = if (isCorrect) Color(0xFF81C784) else Color(0xFFEF9A9A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}

@Composable
fun QuizResultScreen(
    result: QuizResult,
    onNextClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    val scorePercentage = if (result.totalQuestions > 0) 
        (result.correctAnswers.toFloat() / result.totalQuestions.toFloat()) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = scorePercentage,
        animationSpec = tween(1000),
        label = "score_animation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a3e))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Trophy or medal icon based on performance
        val (emoji, message) = when {
            result.accuracy >= 80 -> "🏆" to "Excellent!"
            result.accuracy >= 60 -> "🎉" to "Great Job!"
            result.accuracy >= 40 -> "👍" to "Good Effort!"
            else -> "💪" to "Keep Practicing!"
        }

        Text(
            text = emoji,
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = message,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Circular progress indicator
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color(0xFF2a2a4e))
                .border(
                    width = 8.dp,
                    color = when {
                        result.accuracy >= 80 -> Color(0xFF4CAF50)
                        result.accuracy >= 60 -> Color(0xFFffd700)
                        else -> Color(0xFFFF5722)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${result.correctAnswers}/${result.totalQuestions}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Correct",
                    fontSize = 14.sp,
                    color = Color(0xFFb0b0d0)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = "🎯",
                value = "${result.accuracy}%",
                label = "Accuracy"
            )

            StatItem(
                icon = "⏱",
                value = formatTime(result.timeSeconds.toLong()),
                label = "Time"
            )

            StatItem(
                icon = "⭐",
                value = "+${result.xpEarned}",
                label = "XP Earned"
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Buttons
        Button(
            onClick = onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C4DFF)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Continue",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onReviewClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3a3a5e)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📝",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Review Answers",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )

        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFb0b0d0)
        )
    }
}