package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioEdit
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLabScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val allEdits by viewModel.allEdits.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recDuration by viewModel.recordingDuration.collectAsState()

    // AI Cohorts / Sound core response handles
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val parsedPreset by viewModel.parsedPreset.collectAsState()

    // Retrieve synth slider variables from the view model
    val reverbAmt by viewModel.synthReverb.collectAsState()
    val echoDelay by viewModel.synthEcho.collectAsState()
    val pitchShift by viewModel.synthPitch.collectAsState()
    val playbackSpeed by viewModel.synthSpeed.collectAsState()
    val gainFactor by viewModel.synthGain.collectAsState()

    // Drawn Waveform Amplitude Grid (8 steps)
    val waveformSteps = remember { mutableStateListOf(0.5f, -0.2f, 0.8f, -0.6f, 0.4f, 0.1f, -0.7f, 0.3f) }

    // Fluctuating waveform amplitude for spectator dancing meter animation
    var meterPeaks by remember { mutableStateOf(List(16) { Random.nextFloat() * 100f }) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(120)
                meterPeaks = List(16) { Random.nextFloat() * 120f + 10f }
            }
        } else {
            meterPeaks = List(16) { 15f }
        }
    }

    // Name trigger for recording
    var newClipName by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600, easing = FastOutSlowInEasing)) +
                slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = DMSBackground,
            topBar = {
                TopAppBar(
                title = { Text("SOUND STATION", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = DMSTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DMSBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Indicator
            item {
                Text(
                    text = "DRAW SYNTH INTERACTIVE GRAPH",
                    style = MaterialTheme.typography.titleMedium,
                    color = DMSTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap on the modular vertical steps to design custom frequency filters.",
                    fontSize = 11.sp,
                    color = DMSTextSecondary
                )
            }

            // Waveform Interactive Draw Canvas (represented cleanly by 8 adjustable vertical steps)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DMSSurface)
                        .border(1.5.dp, DMSBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    val localBorder = DMSBorder
                    val localSecondary = DMSSecondary
                    // Oscilloscope Grid Lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // Horizontal zero metric line
                        drawLine(
                            color = localBorder.copy(alpha = 0.5f),
                            start = Offset(0f, canvasHeight / 2),
                            end = Offset(canvasWidth, canvasHeight / 2),
                            strokeWidth = 2f
                        )

                        // Vertical grid indicators
                        for (i in 1..7) {
                            val x = canvasWidth / 8 * i
                            drawLine(
                                color = localBorder.copy(alpha = 0.3f),
                                start = Offset(x, 0f),
                                end = Offset(x, canvasHeight),
                                strokeWidth = 1f
                            )
                        }

                        // Connect step paths with neon lines
                        val path = Path()
                        val stepW = canvasWidth / 8
                        val startY = canvasHeight / 2 - (waveformSteps[0] * (canvasHeight / 2))
                        path.moveTo(stepW / 2, startY)

                        for (i in 1..7) {
                            val x = stepW * i + (stepW / 2)
                            val y = canvasHeight / 2 - (waveformSteps[i] * (canvasHeight / 2))
                            path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = localSecondary,
                            style = Stroke(width = 5f)
                        )
                    }

                    // Adjustable knobs/sliders mapping triggers (Tap to raise or lower step weights)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        waveformSteps.forEachIndexed { idx, valStep ->
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Raise Button
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(DMSBorder)
                                        .clickable {
                                            waveformSteps[idx] = (valStep + 0.2f).coerceAtMost(1.0f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, "+", tint = DMSPrimary, modifier = Modifier.size(10.dp))
                                }

                                // Interactive Step metric slider indicator bar
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .weight(1f)
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(DMSBorder),
                                    contentAlignment = if (valStep >= 0) Alignment.BottomCenter else Alignment.TopCenter
                                ) {
                                    val pct = kotlin.math.abs(valStep)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(pct)
                                            .background(if (valStep >= 0) DMSPrimary else DMSTertiary)
                                    )
                                }

                                // Lower Button
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(DMSBorder)
                                        .clickable {
                                            waveformSteps[idx] = (valStep - 0.2f).coerceAtLeast(-1.0f)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, "-", tint = DMSTertiary, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Synth Audio Multipliers Controllers
            item {
                Text(
                    text = "MODULAR SOUND CONSOLE",
                    style = MaterialTheme.typography.titleMedium,
                    color = DMSTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DMSSurface)
                        .border(1.dp, DMSBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reverb Amount Slider
                    SynthKnobSlider(
                        label = "REVERB ENVELOPE",
                        valueStr = "${(reverbAmt * 100).toInt()}% Space",
                        value = reverbAmt,
                        onValueChange = { viewModel.synthReverb.value = it },
                        accent = DMSPrimary
                    )

                    // Echo Delay
                    SynthKnobSlider(
                        label = "FEEDBACK ECHO",
                        valueStr = "${(echoDelay * 500).toInt()} ms Delay",
                        value = echoDelay,
                        onValueChange = { viewModel.synthEcho.value = it },
                        accent = DMSSecondary
                    )

                    // Pitch
                    SynthKnobSlider(
                        label = "PITCH TRANSPOSER",
                        valueStr = "Shift multiplier ${String.format("%.2f", pitchShift)}x",
                        value = (pitchShift - 0.5f) / 1.5f, // scale map [0.5..2.0] to [0..1]
                        onValueChange = { viewModel.synthPitch.value = 0.5f + (it * 1.5f) },
                        accent = DMSTertiary
                    )

                    // Playback Speed
                    SynthKnobSlider(
                        label = "DECK SPEED RATE",
                        valueStr = "Tempo clock ${String.format("%.2f", playbackSpeed)}x",
                        value = (playbackSpeed - 0.5f) / 1.5f, // scale map [0.5..2.0] to [0..1]
                        onValueChange = { viewModel.synthSpeed.value = 0.5f + (it * 1.5f) },
                        accent = DMSPrimary
                    )

                    // Master Gain
                    SynthKnobSlider(
                        label = "SATURATION GAIN",
                        valueStr = "Output amplification ${String.format("%.1f", gainFactor)}x",
                        value = (gainFactor - 1.0f) / 4.0f, // scale map [1..5] to [0..1]
                        onValueChange = { viewModel.synthGain.value = 1.0f + (it * 4.0f) },
                        accent = DMSSecondary
                    )
                }
            }

            // CO-PRODUCER AI CO-PILOT (Apple Music Styled Neural Engine)
            item {
                Text(
                    text = "CO-PRODUCER AI CO-PILOT",
                    style = MaterialTheme.typography.titleMedium,
                    color = DMSTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_producer_deck"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DMSSurface),
                    border = BorderStroke(1.2.dp, DMSPrimary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Co-pilot",
                                    tint = DMSPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Presets & Co-Producer Core",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DMSTextPrimary
                                )
                            }
                            // Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DMSPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "GEMINI FLASH 3.5",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DMSPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Query the AI co-pilot below to suggest detailed synthesizer combinations (which you can sync with one click) or draft premium song lyrics.",
                            fontSize = 11.sp,
                            color = DMSTextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Gen tags
                        Text(
                            text = "QUICK SOUND EMULATORS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = DMSTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "🎹 Ambient Pad" to "Suggest a dreamy, spacious ambient cinematic pad synthesizer preset",
                                "⚡ Cyberpunk Lead" to "Create an aggressive cyberpunk electronic synthesizer lead preset with rich speed and gain distortion",
                                "🖤 Lo-Fi Warm" to "Design a warm lo-fi melody sound with deep delay echo and slow speed rate",
                                "🎸 Arena Rock" to "Suggest rock arena synth lead with highly saturated echo, gain and max speed rate"
                            ).forEach { (label, prompt) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DMSSurfaceVariant)
                                        .border(1.dp, DMSBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.queryAiCoProducer(prompt)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DMSTextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var promptText by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            placeholder = { Text("Ask for custom sound patches, track advice, lyrics...", fontSize = 12.sp, color = DMSTextSecondary) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = TextStyle(fontSize = 13.sp, color = DMSTextPrimary),
                            trailingIcon = {
                                if (promptText.isNotBlank()) {
                                    IconButton(onClick = { promptText = "" }) {
                                        Icon(Icons.Default.Clear, "Clear", tint = DMSTextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DMSPrimary,
                                unfocusedBorderColor = DMSBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (promptText.isNotBlank()) {
                                    viewModel.queryAiCoProducer(promptText)
                                }
                            },
                            enabled = !isAiLoading && promptText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("btn_ai_generate"),
                            colors = ButtonDefaults.buttonColors(containerColor = DMSPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isAiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("NEURAL SYNTH ACTIVE...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(Icons.Default.AutoAwesome, "Ask AI", modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TRANSMIT CO-PRODUCER PROMPT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (isAiLoading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            // Animating audio bars
                            Row(
                                modifier = Modifier.fillMaxWidth().height(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val scale1 by infiniteTransition.animateFloat(
                                    initialValue = 0.3f, targetValue = 1f,
                                    animationSpec = infiniteRepeatable(animation = tween(400, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "p1"
                                )
                                val scale2 by infiniteTransition.animateFloat(
                                    initialValue = 1f, targetValue = 0.3f,
                                    animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 100, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "p2"
                                )
                                val scale3 by infiniteTransition.animateFloat(
                                    initialValue = 0.3f, targetValue = 1f,
                                    animationSpec = infiniteRepeatable(animation = tween(400, delayMillis = 200, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "p3"
                                )
                                Box(modifier = Modifier.width(4.dp).fillMaxHeight(scale1).clip(CircleShape).background(DMSPrimary))
                                Box(modifier = Modifier.width(4.dp).fillMaxHeight(scale2).clip(CircleShape).background(DMSSecondary))
                                Box(modifier = Modifier.width(4.dp).fillMaxHeight(scale3).clip(CircleShape).background(DMSTertiary))
                            }
                        }

                        if (aiResponse.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DMSSurfaceVariant)
                                    .border(1.dp, DMSBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "INTELLIGENT SECTIONS & OUTPUT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = DMSPrimary
                                        )
                                        IconButton(
                                            onClick = { viewModel.clearParsedPreset(); viewModel.queryAiCoProducer("") },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(Icons.Default.Close, "Dismiss", tint = DMSTextSecondary, modifier = Modifier.size(12.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = aiResponse,
                                        fontSize = 12.sp,
                                        color = DMSTextPrimary,
                                        lineHeight = 18.sp
                                    )

                                    parsedPreset?.let { preset ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(color = DMSBorder.copy(alpha = 0.4f), thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "DETECTIONS FOUND:",
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DMSTextSecondary
                                                )
                                                Text(
                                                    text = "RVB: ${(preset["reverb"]!!*100).toInt()}% • ECO: ${(preset["echo"]!!*100).toInt()}% • PTCH: ${String.format("%.2f", preset["pitch"]!!)}x • SPD: ${String.format("%.2f", preset["speed"]!!)}x",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = DMSTextPrimary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                viewModel.applyParsedPreset()
                                                Toast.makeText(context, "Preset applied successfully to synthesiser sliders!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("sync_ai_preset"),
                                            colors = ButtonDefaults.buttonColors(containerColor = DMSPrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Shuffle, "Apply", modifier = Modifier.size(16.dp), tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("APPLY AI PRESET TO CONSOLE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recording & Synthesis Deck
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SAMPLE RECORDING & DECK INTEGRATION", style = MaterialTheme.typography.titleMedium, color = DMSTextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DMSSurfaceVariant)
                        .border(1.dp, DMSBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRecording) "RECORDING SYNTH MASTER ACTIVE" else "STUDIO DEPLOY MODULAR RECORDER",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isRecording) DMSTertiary else DMSPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Spectrometer Meter Bars Display (fluctuates dynamically)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            meterPeaks.forEach { peakHeight ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(peakHeight.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(DMSTertiary, DMSPrimary)
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isRecording) {
                            Text(
                                "REC TIME: ${recDuration}s / 60s max",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = DMSTextPrimary
                            )
                        } else {
                            Text(
                                "Modular deck idle",
                                fontSize = 12.sp,
                                color = DMSTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (!isRecording) {
                                Button(
                                    onClick = { viewModel.startRecording() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DMSError),
                                    modifier = Modifier.testTag("btn_rec_start")
                                ) {
                                    Icon(Icons.Default.FiberManualRecord, "Rec", tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("START CAPTURING", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { showSaveDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = DMSPrimary),
                                    modifier = Modifier.testTag("btn_rec_stop")
                                ) {
                                    Icon(Icons.Default.StopCircle, "Stop", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PAUSE & SAVE CLIP", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Saved Sessions Listing
            item {
                Text("SAVED SOUND PATCHES (" + allEdits.size + ")", style = MaterialTheme.typography.titleMedium, color = DMSTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (allEdits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DMSSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recorded sound synthesis files on device.", fontSize = 11.sp, color = DMSTextSecondary)
                    }
                }
            } else {
                items(allEdits, key = { it.id }) { edit ->
                    SavedSoundRow(
                        edit = edit,
                        onLoad = {
                            viewModel.synthReverb.value = edit.reverbAmt
                            viewModel.synthEcho.value = edit.echoDelay
                            viewModel.synthPitch.value = edit.pitchShift
                            viewModel.synthSpeed.value = edit.playbackSpeed
                            viewModel.synthGain.value = edit.gainFactor

                            // Deserialize waveform step values
                            val listStr = edit.waveformPoints.split(",")
                            if (listStr.size == 8) {
                                for (i in 0 until 8) {
                                    waveformSteps[i] = listStr[i].toFloatOrNull() ?: 0.0f
                                }
                            }
                            Toast.makeText(context, "Patch parameters fully loaded!", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { viewModel.deleteEditSession(edit) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

    // Save dialog popup
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("SAVE CUSTOM CLIPS", color = DMSTextPrimary) },
            text = {
                Column {
                    Text("Register synthesizer parameters under custom patch namespace.", fontSize = 12.sp, color = DMSTextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newClipName,
                        onValueChange = { newClipName = it },
                        placeholder = { Text("e.g. Ambient Deep Reverb", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("input_save_recording_name"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DMSPrimary)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("btn_save_recording_confirm"),
                    onClick = {
                        val pointsStr = waveformSteps.joinToString(",")
                        viewModel.stopRecording(newClipName, pointsStr)
                        newClipName = ""
                        showSaveDialog = false
                        Toast.makeText(context, "Creative Patch Logged!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("COMMIT PATCH", color = DMSPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.stopRecording("", "")
                        showSaveDialog = false
                    }
                ) {
                    Text("DISCARD", color = DMSTextSecondary)
                }
            },
            containerColor = DMSSurface
        )
    }
}

@Composable
fun SynthKnobSlider(
    label: String,
    valueStr: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accent: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DMSTextSecondary)
            Text(valueStr, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, color = accent)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = DMSBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        )
    }
}

@Composable
fun SavedSoundRow(
    edit: AudioEdit,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DMSSurface)
            .border(1.dp, DMSBorder, RoundedCornerShape(10.dp))
            .clickable { onLoad() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DMSPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AudioFile, "Patch File", tint = DMSPrimary, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(edit.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DMSTextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("reverb: ${(edit.reverbAmt * 100).toInt()}%", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = DMSTextSecondary)
                Text("pitch: ${String.format("%.2f", edit.pitchShift)}x", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = DMSTextSecondary)
                Text("gain: ${String.format("%.1f", edit.gainFactor)}x", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = DMSTextSecondary)
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.DeleteOutline, "Delete", tint = DMSError.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
    }
}
