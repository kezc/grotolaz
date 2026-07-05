package com.wojtek.holds.components.climbingwall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wojtek.holds.database.Problem
import com.wojtek.holds.database.ProblemRepository
import com.wojtek.holds.utils.toBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

@Composable
fun SaveDialog(
    onDismissRequest: () -> Unit,
    problemRepository: ProblemRepository,
    problem: Problem,
    climbingWallState: ClimbingWallState
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val focusManager = LocalFocusManager.current

    fun getImage(): ImageBitmap {
        val configuration = climbingWallState.configuration
        val wallPainter = climbingWallState.wallPainter
        val emptyPainter = climbingWallState.emptyPainter
        val selectedHoldIds = climbingWallState.selectedHoldIds

        if (configuration == null || wallPainter == null) {
            return ImageBitmap(10, 10)
        }

        return generateRouteImageBitmap(
            configuration = configuration,
            wallImagePainter = wallPainter,
            selectedHoldIds = selectedHoldIds,
            density = density,
            layoutDirection = layoutDirection,
            darkenNonSelected = true,
            showEmptyWall = true,
            emptyWallImagePainter = emptyPainter
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var name by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                val imageDeferred = remember { coroutineScope.async { getImage() } }
                val image: ImageBitmap? by produceState(null) { value = imageDeferred.await() }

                var existingProblem by remember { mutableStateOf<Problem?>(null) }
                val trimmedName = name.trim()
                LaunchedEffect(trimmedName, problemRepository) {
                    existingProblem = if (trimmedName.isEmpty()) {
                        null
                    } else {
                        try {
                            problemRepository.findByName(trimmedName)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                }
                val hasDuplicateName = existingProblem != null

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Zapisz boulder",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Zamknij",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Image Preview Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (image != null) {
                        Image(
                            bitmap = image!!,
                            contentDescription = "Podgląd drogi",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Generowanie podglądu...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Name Input Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa drogi") },
                    placeholder = { Text("np. Mój pierwszy projekt, Dyno") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                AnimatedVisibility(hasDuplicateName) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val isDark = isSystemInDarkTheme()
                    val warningContainerColor = if (isDark) Color(0xFF4F3A00) else Color(0xFFFFF3CD)
                    val warningContentColor = if (isDark) Color(0xFFFFE082) else Color(0xFF664D03)
                    val warningBorderColor = if (isDark) Color(0xFF684E00) else Color(0xFFFFECB5)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(warningContainerColor)
                            .border(1.dp, warningBorderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Ostrzeżenie",
                            tint = warningContentColor
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Droga o tej nazwie już istnieje.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = warningContentColor
                            )
                            Text(
                                text = "Zapisanie nadpisze istniejący boulder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = warningContentColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Anuluj")
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val imagePng = withContext(Dispatchers.Default) { imageDeferred.await().toBase64() }
                                val currentExisting = existingProblem
                                val finalProblem = if (currentExisting != null) {
                                    problem.copy(
                                        id = currentExisting.id,
                                        name = name.trim(),
                                        createdAt = currentExisting.createdAt,
                                        updatedAt = Clock.System.now().epochSeconds,
                                        imageBase64 = imagePng
                                    )
                                } else {
                                    problem.copy(
                                        name = name.trim(),
                                        imageBase64 = imagePng
                                    )
                                }
                                problemRepository.save(finalProblem)
                                onDismissRequest()
                            }
                        },
                        enabled = name.isNotBlank() && !isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (hasDuplicateName) "Nadpisz" else "Zapisz")
                        }
                    }
                }
            }
        }
    }
}