package com.roubao.autopilot.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roubao.autopilot.data.AuthResult
import com.roubao.autopilot.data.RegisterRequest
import com.roubao.autopilot.data.UserRole
import com.roubao.autopilot.ui.theme.BaoziTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    userManager: com.roubao.autopilot.data.UserManager
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showRegister by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var authResult by remember { mutableStateOf<AuthResult?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BaoziTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo 和标题
            Spacer(modifier = Modifier.height(60.dp))
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BaoziTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤖",
                    fontSize = 40.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (showRegister) "创建账户" else "欢迎回来",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BaoziTheme.colors.textPrimary
            )
            
            Text(
                text = if (showRegister) "加入肉包AI助手社区" else "登录您的账户继续使用",
                fontSize = 16.sp,
                color = BaoziTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 表单区域
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = BaoziTheme.colors.backgroundCard
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 用户名输入
                    InputField(
                        value = username,
                        onValueChange = { username = it },
                        label = "用户名",
                        leadingIcon = Icons.Default.Person,
                        placeholder = "请输入用户名"
                    )
                    
                    // 密码输入
                    var passwordVisible by remember { mutableStateOf(false) }
                    InputField(
                        value = password,
                        onValueChange = { password = it },
                        label = "密码",
                        leadingIcon = Icons.Default.Lock,
                        placeholder = "请输入密码",
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                    tint = BaoziTheme.colors.textSecondary
                                )
                            }
                        }
                    )
                    
                    // 注册时显示角色选择和邮箱
                    if (showRegister) {
                        EmailInputField { email ->
                            // 邮箱输入处理
                        }
                        
                        RoleSelectionField()
                    }
                    
                    // 错误信息显示
                    AnimatedVisibility(authResult?.success == false) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = BaoziTheme.colors.error.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = authResult?.errorMessage ?: "未知错误",
                                color = BaoziTheme.colors.error,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    
                    // 主要操作按钮
                    Button(
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                isLoading = true
                                authResult = null
                                
                                if (showRegister) {
                                    // 注册逻辑
                                    val registerRequest = RegisterRequest(
                                        username = username,
                                        email = null, // 简化处理
                                        password = password,
                                        role = UserRole.STUDENT // 默认角色
                                    )
                                    val result = userManager.register(registerRequest)
                                    authResult = result
                                    if (result.success) {
                                        onLoginSuccess()
                                    }
                                } else {
                                    // 登录逻辑
                                    val result = userManager.login(username, password)
                                    authResult = result
                                    if (result.success) {
                                        onLoginSuccess()
                                    }
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = username.isNotBlank() && password.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = if (showRegister) "创建账户" else "登录",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // 切换登录/注册
                    TextButton(
                        onClick = {
                            showRegister = !showRegister
                            authResult = null // 清除之前的错误信息
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (showRegister) "已有账户？立即登录" else "没有账户？立即注册",
                            color = BaoziTheme.colors.primary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 底部信息
            Text(
                text = "© 2024 肉包AI助手",
                color = BaoziTheme.colors.textHint,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = BaoziTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BaoziTheme.colors.backgroundInput)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = BaoziTheme.colors.textHint,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = BaoziTheme.colors.textPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(BaoziTheme.colors.primary),
                    visualTransformation = visualTransformation,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = BaoziTheme.colors.textHint,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                trailingIcon?.invoke()
            }
        }
    }
}

@Composable
private fun EmailInputField(onEmailChange: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    
    InputField(
        value = email,
        onValueChange = {
            email = it
            onEmailChange(it)
        },
        label = "邮箱（可选）",
        leadingIcon = Icons.Default.Email,
        placeholder = "请输入邮箱地址"
    )
}

@Composable
private fun RoleSelectionField() {
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    
    Column {
        Text(
            text = "选择您的角色",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = BaoziTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = BaoziTheme.colors.backgroundInput
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                UserRole.values().forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (role == selectedRole),
                                onClick = { selectedRole = role }
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (role == selectedRole),
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BaoziTheme.colors.primary
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = role.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = BaoziTheme.colors.textPrimary
                            )
                            
                            Text(
                                text = role.description,
                                fontSize = 12.sp,
                                color = BaoziTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}