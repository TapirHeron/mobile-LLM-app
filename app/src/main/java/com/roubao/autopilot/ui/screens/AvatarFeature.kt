package com.roubao.autopilot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.roubao.autopilot.data.AppSettings
import com.roubao.autopilot.data.UserInfo
import com.roubao.autopilot.data.UserManager
import com.roubao.autopilot.ui.theme.BaoziTheme
import com.roubao.autopilot.utils.AvatarManager
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import java.io.File

enum class ImagePickerMode {
    GALLERY,
    CAMERA
}

/**
 * 增强的用户菜单，包含头像显示和修改功能
 */
@Composable
fun EnhancedUserMenu(
    user: UserInfo,
    userManager: UserManager,
    onLogout: () -> Unit,
    settings: AppSettings,
    onNavigateToSettings: () -> Unit,
) {
    val colors = BaoziTheme.colors
    var expanded by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var imagePickerMode: ImagePickerMode by remember { mutableStateOf(ImagePickerMode.GALLERY) }
    val context = LocalContext.current
    val avatarManager = remember(context) { AvatarManager(context) }
    
    Box {
        // 用户头像按钮
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .clip(CircleShape)
                .background(BaoziTheme.colors.backgroundCard)
                .border(BorderStroke(2.dp, colors.primary), CircleShape)
        ) {
            if (user.avatarPath != null && user.avatarPath.isNotEmpty()) {
                AsyncImage(
                    model = user.avatarPath,
                    contentDescription = "用户头像",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "用户菜单",
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // 下拉菜单
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(BaoziTheme.colors.backgroundCard)
        ) {
            // 用户信息项
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${user.username} (${user.role.displayName})")
                    }
                },
                onClick = { expanded = false }
            )
            
            Divider(color = colors.surfaceVariant)
            
            // 切换角色
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("切换角色")
                    }
                },
                onClick = {
                    expanded = false
                    showRoleDialog = true
                }
            )
            
            // 修改头像
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("修改头像")
                    }
                },
                onClick = {
                    expanded = false
                    showImagePicker = true
                    imagePickerMode = ImagePickerMode.GALLERY
                }
            )
            
            // API密钥设置
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (settings.currentConfig.apiKey.isNotEmpty()) "API密钥: ${settings.currentConfig.apiKey.take(8)}..." else "设置API密钥",
                            maxLines = 1
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onNavigateToSettings()
                }
            )
            
            Divider(color = colors.surfaceVariant)
            
            // 退出登录
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("退出登录", color = colors.error)
                    }
                },
                onClick = {
                    expanded = false
                    userManager.logout()
                    onLogout()
                }
            )
        }
    }
    
    // 角色选择对话框
    if (showRoleDialog) {
        RoleSelectionDialog(
            currentUserRole = user.role,
            onRoleSelected = { newRole ->
                userManager.updateUserRole(newRole)
                showRoleDialog = false
            },
            onDismiss = { showRoleDialog = false }
        )
    }
    
    // 图片选择器
    if (showImagePicker) {
        ImagePickerDialog(
            mode = imagePickerMode,
            onDismiss = { showImagePicker = false },
            onImageSelected = { uri ->
                // 处理选中的图片
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val avatarPath = when (imagePickerMode) {
                            ImagePickerMode.GALLERY -> avatarManager.selectFromGallery(uri)
                            ImagePickerMode.CAMERA -> avatarManager.captureFromCamera(uri)
                        }
                        
                        if (avatarPath != null) {
                            // 更新用户头像
                            userManager.updateUserAvatar(avatarPath)
                            println("头像更新成功: $avatarPath")
                        } else {
                            println("头像更新失败")
                        }
                    } catch (e: Exception) {
                        println("头像更新异常: ${e.message}")
                    } finally {
                        showImagePicker = false
                    }
                }
            }
        )
    }
}

/**
 * 图片选择对话框
 */
@Composable
fun ImagePickerDialog(
    mode: ImagePickerMode,
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val colors = BaoziTheme.colors
    val context = LocalContext.current
    
    // 状态管理：存储拍照的URI
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    // 创建临时文件用于相机拍照
    val createPhotoUri = {
        try {
            // 使用应用私有目录创建临时文件，避免权限问题
            val avatarDir = File(context.externalCacheDir, "camera_photos")
            if (!avatarDir.exists()) {
                avatarDir.mkdirs()
            }
            val file = File(avatarDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            photoUri = uri
            uri
        } catch (e: Exception) {
            android.util.Log.e("AvatarFeature", "Failed to create temp file", e)
            null
        }
    }
    
    // 图片选择器 launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImageSelected(it) } ?: onDismiss()
    }
    
    // 相机 launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            onImageSelected(photoUri!!)
        } else {
            onDismiss()
        }
    }
    
    // 触发选择器
    LaunchedEffect(mode) {
        when (mode) {
            ImagePickerMode.GALLERY -> {
                galleryLauncher.launch("image/*")
            }
            ImagePickerMode.CAMERA -> {
                createPhotoUri()?.let { uri ->
                    cameraLauncher.launch(uri)
                } ?: run {
                    android.widget.Toast.makeText(context, "无法创建拍照文件", android.widget.Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择图片",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when (mode) {
                        ImagePickerMode.GALLERY -> "正在打开相册..."
                        ImagePickerMode.CAMERA -> "正在启动相机..."
                    },
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
                
                // 加载指示器
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}