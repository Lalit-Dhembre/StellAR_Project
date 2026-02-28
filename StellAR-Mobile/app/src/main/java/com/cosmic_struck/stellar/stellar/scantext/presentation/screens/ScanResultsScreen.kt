package com.cosmic_struck.stellar.stellar.scantext.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cosmic_struck.stellar.common.components.StellarScaffold
import com.cosmic_struck.stellar.stellar.scantext.data.dto.ConceptDTO
import com.cosmic_struck.stellar.stellar.scantext.presentation.components.TopBarScanTextBook
import com.cosmic_struck.stellar.stellar.scantext.presentation.scanScreen.ScanTextViewModel

@Composable
fun ScanResultsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanTextViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    Log.d("SCAN_RESULTS_SCREEN", "pdfResponse: ${state.pdfResponse?.success}")
    Log.d("SCAN_RESULTS_SCREEN", "jobStatus: ${state.jobStatus?.status}")

    StellarScaffold(
        topBar = {
            TopBarScanTextBook(
                title = "Results",
                navigateBack = { onNavigateBack() }
            )
        }
    ) { contentModifier ->
        if (state.isLoading) {
            Box(
                modifier = contentModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (state.pdfResponse != null) {
            val pdfResponse = state.pdfResponse!!
            val document = pdfResponse.document
            val concepts = pdfResponse.concepts ?: emptyList()
            val jobStatus = state.jobStatus

            LazyColumn(
                modifier = contentModifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Document Header ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = document?.title ?: "Untitled Document",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                InfoChip(
                                    label = document?.domain ?: "Unknown",
                                    color = Color(0xFF6C63FF)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                InfoChip(
                                    label = "${document?.page_count ?: 0} pages",
                                    color = Color(0xFF00BCD4)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                InfoChip(
                                    label = "${concepts.size} concepts",
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }

                // ── Async Status Panel (Phase 5) ──
                item {
                    AsyncStatusPanel(
                        quizStatus = jobStatus?.quiz_status ?: "pending",
                        tutorStatus = jobStatus?.tutor_status ?: "pending",
                        summaryStatus = jobStatus?.summary_status ?: "pending",
                        assetsStatus = jobStatus?.assets_status ?: "pending",
                        isPolling = state.isPolling
                    )
                }

                // ── Summary (when ready) ──
                if (jobStatus?.summary != null && jobStatus.summary.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Summary",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = jobStatus.summary,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // ── Concept Chunks ──
                items(concepts) { concept ->
                    ConceptCard(concept = concept)
                }

                // ── Quiz Section (when ready) ──
                if (jobStatus?.quiz != null && jobStatus.quiz.isNotEmpty()) {
                    item {
                        Text(
                            "Quiz",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(jobStatus.quiz) { question ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = question.question,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                question.options.forEachIndexed { index, option ->
                                    val letter = ('A' + index)
                                    Text(
                                        text = "$letter. $option",
                                        color = if (option == question.correct_answer)
                                            Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Tutor Script (when ready) ──
                if (jobStatus?.tutor_script != null && jobStatus.tutor_script.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Tutor Explanation",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = jobStatus.tutor_script,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = contentModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No results available", color = Color.White)
            }
        }
    }
}

@Composable
private fun ConceptCard(concept: ConceptDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Text(
                text = concept.concept_title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // Keywords
            if (!concept.keywords.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    concept.keywords.take(4).forEach { keyword ->
                        InfoChip(label = keyword, color = Color(0xFF9C27B0))
                    }
                }
            }

            // Content preview
            if (concept.content_text.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = concept.content_text,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Assets
            if (!concept.assets.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${concept.assets.size} 3D model(s) available",
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp
                    )
                }
            }

            // Page range
            concept.page_range?.let { range ->
                if (range.size == 2) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Pages ${range[0]}–${range[1]}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AsyncStatusPanel(
    quizStatus: String,
    tutorStatus: String,
    summaryStatus: String,
    assetsStatus: String,
    isPolling: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Content Generation",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isPolling) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF6C63FF)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            StatusRow(icon = Icons.Default.AutoStories, label = "Summary", status = summaryStatus)
            StatusRow(icon = Icons.Default.Quiz, label = "Quiz", status = quizStatus)
            StatusRow(icon = Icons.Default.Hub, label = "Tutor Script", status = tutorStatus)
            StatusRow(icon = Icons.Default.AcUnit, label = "3D Assets", status = assetsStatus)
        }
    }
}

@Composable
private fun StatusRow(icon: ImageVector, label: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        StatusBadge(status)
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "complete" -> Color(0xFF4CAF50) to "Done"
        "error" -> Color(0xFFF44336) to "Error"
        "pending" -> Color.Gray to "Pending"
        "generating" -> Color(0xFFFFC107) to "Generating"
        else -> Color(0xFF2196F3) to "Processing"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}