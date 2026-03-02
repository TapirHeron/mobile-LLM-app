package com.roubao.autopilot.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.roubao.autopilot.App
import com.roubao.autopilot.agent.AgentState
import com.roubao.autopilot.data.AppSettings
import com.roubao.autopilot.data.UserManager
import com.roubao.autopilot.data.UserRole
import com.roubao.autopilot.ui.theme.BaoziTheme
import com.roubao.autopilot.ui.theme.Primary
import com.roubao.autopilot.ui.theme.Secondary
import com.roubao.autopilot.utils.SpeechRecognitionState
import com.roubao.autopilot.utils.rememberSpeechRecognizer

/**
 * 预设命令
 */
data class PresetCommand(
    val icon: String,
    val title: String,
    val command: String
)

val presetCommands = listOf(
    PresetCommand("🍔", "点汉堡", "帮我点个附近好吃的汉堡"),
    PresetCommand("📕", "发小红书", "帮我发一条小红书，内容是今日份好心情"),
    PresetCommand("📺", "刷B站", "打开B站搜索肉包，找到第一个视频点个赞"),
    PresetCommand("✈️", "旅游攻略", "用小美帮我查一下三亚旅游攻略"),
    PresetCommand("🎵", "听音乐", "打开网易云音乐播放每日推荐"),
    PresetCommand("🛒", "点外卖", "帮我在美团点一份猪脚饭")
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    agentState: AgentState?,
    logs: List<String>,
    onExecute: (String) -> Unit,
    onStop: () -> Unit,
    shizukuAvailable: Boolean,
    currentModel: String = "",
    onRefreshShizuku: () -> Unit = {},
    onShizukuRequired: () -> Unit = {},
    isExecuting: Boolean = false,
    userManager: UserManager,
    onLogout: () -> Unit = {},
    settings: AppSettings,
    onNavigateToSettings: () -> Unit = {},
    // 语音识别相关参数
    speechState: SpeechRecognitionState = SpeechRecognitionState.Idle,
    onSpeechStart: () -> Unit = {},
    onSpeechResult: (String) -> Unit = {},
    onSpeechError: (String) -> Unit = {}
) {
    val colors = BaoziTheme.colors
    var inputText by remember { mutableStateOf("") }
    // 使用 isExecuting 或 agentState?.isRunning 来判断是否运行中
    val isRunning = isExecuting || agentState?.isRunning == true
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    // 检测键盘是否可见
    val imeVisible = WindowInsets.isImeVisible
    
    // 当键盘弹出时，自动清除焦点
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            // 键盘弹出，可以做一些处理
        } else {
            // 键盘收起，清除焦点
            focusManager.clearFocus()
        }
    }
    
    // 语音识别相关状态和函数
    val speechRecognizer = rememberSpeechRecognizer()
    var speechRecognitionState by remember { mutableStateOf<SpeechRecognitionState>(SpeechRecognitionState.Idle) }
    var triggerSpeechRecognition by remember { mutableStateOf(false) }
    
    // 微信式语音输入状态
    var isRecording by remember { mutableStateOf(false) }
    var shouldCancelSend by remember { mutableStateOf(false) }  // 上滑取消标志
    var currentVolume by remember { mutableStateOf(0f) }  // 当前音量
    
    // 权限检查和请求
    fun checkAudioPermission(context: android.content.Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun requestAudioPermission(context: android.content.Context) {
        ActivityCompat.requestPermissions(
            context as android.app.Activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            1001
        )
    }
    
    // 语音识别处理函数
    fun startSpeechRecognition() {
        if (speechRecognitionState is SpeechRecognitionState.Listening) {
            // 如果正在监听，则停止
            println("[语音识别] ⏹️ 用户手动停止")
            speechRecognizer.stopListening()
            speechRecognitionState = SpeechRecognitionState.Idle
        } else {
            // 否则开始监听
            triggerSpeechRecognition = true
        }
    }
    
    // 停止语音识别（供外部调用）
    fun stopSpeechRecognition() {
        println("[语音识别] ⏹️ 调用 stopSpeechRecognition")
        speechRecognizer.stopListening()
        speechRecognitionState = SpeechRecognitionState.Idle
    }
    
    // 开始录音（微信模式 - 长按开始）
    fun startRecording() {
        println("[语音识别] 🎤 开始录音...")
        isRecording = true
        shouldCancelSend = false
        speechRecognitionState = SpeechRecognitionState.Listening
        
        // 设置监听器
        speechRecognizer.setOnReadyForSpeechListener {
            println("[语音识别] ✅ 已准备好录音")
        }
        
        speechRecognizer.setOnVolumeChangedListener { volume ->
            currentVolume = volume
            // println("[语音识别] 音量：$volume")
        }
        
        speechRecognizer.setOnEndOfSpeechListener {
            println("[语音识别] 📝 说话结束")
        }
        
        // 启动识别
        triggerSpeechRecognition = true
    }
    
    // 结束录音并发送（微信模式 - 松开或上滑）
    fun endRecording(send: Boolean) {
        println("[语音识别] ⏹️ 结束录音，send=$send, shouldCancelSend=$shouldCancelSend")
        isRecording = false
        
        if (shouldCancelSend || !send) {
            // 取消发送
            println("[语音识别] ❌ 取消发送")
            speechRecognizer.cancelListening()
            speechRecognitionState = SpeechRecognitionState.Idle
            Toast.makeText(context, "已取消发送", Toast.LENGTH_SHORT).show()
        } else {
            // 正常停止识别，但不立即调用 stopListening()
            // 让 LaunchedEffect 中的 startListening() 自然完成
            println("[语音识别] ✅ 等待识别完成...")
            // 状态会在识别完成后由 LaunchedEffect 更新
        }
        
        currentVolume = 0f
    }
    
    // 监听语音识别触发
    LaunchedEffect(triggerSpeechRecognition) {
        if (triggerSpeechRecognition) {
            triggerSpeechRecognition = false
            
            if (!checkAudioPermission(context)) {
                requestAudioPermission(context)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "请授予录音权限以使用语音输入功能",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@LaunchedEffect
            }
            
            if (!speechRecognizer.hasAudioPermission()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "缺少录音权限，请在设置中开启",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@LaunchedEffect
            }
            
            speechRecognitionState = SpeechRecognitionState.Listening
            onSpeechStart()
            
            println("[语音识别] 🎤 开始启动...")
            try {
                val result = speechRecognizer.startListening()
                println("[语音识别] 识别结果：'$result'")
                
                // 无论是否有结果，都重置录音状态
                isRecording = false
                
                if (result.isNotBlank()) {
                    speechRecognitionState = SpeechRecognitionState.Success(result)
                    onSpeechResult(result)
                    // 自动将识别结果填入输入框
                    inputText = result
                    println("[语音识别] ✅ 成功识别")
                    Toast.makeText(context, "识别成功：$result", Toast.LENGTH_SHORT).show()
                } else {
                    // 用户可能取消了识别或没有说话，重置为空闲状态
                    speechRecognitionState = SpeechRecognitionState.Idle
                    println("[语音识别] ⚠️ 未检测到语音")
                    Toast.makeText(context, "未检测到语音", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isRecording = false
                println("[语音识别] ❌ 异常：${e.message}")
                e.printStackTrace()
                speechRecognitionState = SpeechRecognitionState.Error(e.message ?: "语音识别失败")
                onSpeechError(e.message ?: "语音识别失败")
                Toast.makeText(context, "语音识别失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
            // 注意：不在这里调用 destroy()，只在组件销毁时调用
        }
    }
    
    // 重置语音状态
    fun resetSpeechState() {
        speechRecognitionState = SpeechRecognitionState.Idle
    }

    // 在 Composable 销毁时清理语音识别资源
    DisposableEffect(Unit) {
        onDispose {
            println("[语音识别] 🧹 HomeScreen 销毁，清理语音识别资源")
            speechRecognizer.destroy()
        }
    }

    // 记录上一次的运行状态，用于检测任务结束
    var wasRunning by remember { mutableStateOf(false) }

    // 任务结束时清空输入框
    LaunchedEffect(isRunning) {
        if (wasRunning && !isRunning) {
            // 从运行中变为未运行，说明任务结束
            inputText = ""
        }
        wasRunning = isRunning
    }

    // 自动滚动到底部
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 顶部标题和用户信息
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "肉包",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                        
                        // 显示当前用户信息
                        userManager.currentUser.collectAsState().value?.let { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "欢迎，${user.username}",
                                    fontSize = 14.sp,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.success)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = user.role.displayName,
                                    fontSize = 12.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // 用户操作菜单
                    userManager.currentUser.collectAsState().value?.let { user ->
                        EnhancedUserMenu(
                            user = user,
                            userManager = userManager,
                            onLogout = onLogout,
                            settings = settings,
                            onNavigateToSettings = onNavigateToSettings
                        )
                    }
                }
                
                Text(
                    text = if (shizukuAvailable) "准备就绪，告诉我你想做什么" else "请先连接 Shizuku",
                    fontSize = 14.sp,
                    color = if (shizukuAvailable) colors.textSecondary else colors.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // 内容区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isRunning || logs.isNotEmpty()) {
                // 执行中或有日志时显示日志
                ExecutionLogView(
                    logs = logs,
                    isRunning = isRunning,
                    currentStep = agentState?.currentStep ?: 0,
                    currentModel = currentModel,
                    listState = listState,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 空闲时显示预设命令
                PresetCommandsView(
                    onCommandClick = { command ->
                        if (shizukuAvailable) {
                            inputText = command
                        } else {
                            onShizukuRequired()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 底部输入区域
        InputArea(
            inputText = inputText,
            onInputChange = { inputText = it },
            onExecute = {
                if (inputText.isNotBlank()) {
                    // 收起键盘并清除焦点
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onExecute(inputText)
                }
            },
            onStop = {
                // 停止任务并清空输入框
                inputText = ""
                onStop()
            },
            isRunning = isRunning,
            enabled = shizukuAvailable,
            onInputClick = {
                if (!shizukuAvailable) {
                    onShizukuRequired()
                }
            },
            // 语音识别相关参数
            speechRecognitionState = speechRecognitionState,
            onStartSpeech = { startSpeechRecognition() },
            onStopSpeech = { 
                // 调用停止函数
                stopSpeechRecognition()
            },
            // 微信模式长按说话参数
            isRecording = isRecording,
            onStartRecording = { startRecording() },
            onEndRecording = { send -> endRecording(send) },
            currentVolume = currentVolume
        )
    }
}

@Composable
fun PresetCommandsView(
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BaoziTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "试试这些指令",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        presetCommands.chunked(2).forEach { rowCommands ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCommands.forEach { preset ->
                    PresetCommandCard(
                        preset = preset,
                        onClick = { onCommandClick(preset.command) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 如果是奇数，补一个空白
                if (rowCommands.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PresetCommandCard(
    preset: PresetCommand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BaoziTheme.colors
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = preset.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = preset.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = preset.command,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ExecutionLogView(
    logs: List<String>,
    isRunning: Boolean,
    currentStep: Int,
    currentModel: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 执行状态指示器
        if (isRunning) {
            ExecutingIndicator(currentStep = currentStep, currentModel = currentModel)
        }

        // 日志列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(logs) { log ->
                LogItem(log = log)
            }
        }
    }
}

@Composable
fun ExecutingIndicator(currentStep: Int, currentModel: String = "") {
    val colors = BaoziTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "executing")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.backgroundCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 动画圆点
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(Primary, Secondary, Primary)
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "正在执行 Step $currentStep",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.primary
                        )
                    }
                    // 显示当前模型
                    if (currentModel.isNotEmpty()) {
                        Text(
                            text = currentModel,
                            fontSize = 11.sp,
                            color = colors.textHint,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Primary, Secondary)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun LogItem(log: String) {
    val colors = BaoziTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.backgroundCard
        )
    ) {
        Text(
            text = log,
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onExecute: () -> Unit,
    onStop: () -> Unit,
    isRunning: Boolean,
    enabled: Boolean,
    onInputClick: () -> Unit = {},
    // 语音识别相关参数
    speechRecognitionState: SpeechRecognitionState = SpeechRecognitionState.Idle,
    onStartSpeech: () -> Unit = {},
    onStopSpeech: () -> Unit = {},
    // 微信模式长按说话
    isRecording: Boolean = false,
    onStartRecording: () -> Unit = {},
    onEndRecording: (Boolean) -> Unit = {},
    currentVolume: Float = 0f
) {
    val colors = BaoziTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = colors.backgroundCard,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isRunning) Arrangement.Center else Arrangement.Start
        ) {
            if (isRunning) {
                // 运行中只显示停止按钮
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "停止",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "停止执行",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // 非运行状态显示输入框和发送按钮
                // 输入框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.backgroundInput)
                        .then(
                            if (!enabled) {
                                Modifier.clickable { onInputClick() }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (enabled) {
                        // Shizuku 已连接，显示可编辑的输入框
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = "告诉肉包你想做什么...",
                                            color = colors.textHint,
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        // Shizuku 未连接，显示提示文字
                        Text(
                            text = "请先连接 Shizuku",
                            color = colors.textHint,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))


                // 发送按钮
                IconButton(
                    onClick = onExecute,
                    enabled = enabled && inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && enabled) colors.primary
                            else colors.backgroundInput
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "发送",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSelectionDialog(
    currentUserRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = BaoziTheme.colors
    var selectedRole by remember { mutableStateOf(currentUserRole) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择您的角色",
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "不同的角色会影响AI助手的交互方式和提示词风格",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
                
                UserRole.entries.forEach { role ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRole = role },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRole == role) colors.primary.copy(alpha = 0.1f) 
                                           else colors.backgroundCard
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = role.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = role.description,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRoleSelected(selectedRole)
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BaoziTheme {
        HomeScreen(
            agentState = null,
            logs = emptyList(),
            onExecute = {},
            onStop = {},
            shizukuAvailable = true,
            userManager = UserManager.getInstance(App.getInstance()),
            settings = AppSettings(),
            onSpeechStart = {},
            onSpeechResult = {},
            onSpeechError = {}
        )
    }
}