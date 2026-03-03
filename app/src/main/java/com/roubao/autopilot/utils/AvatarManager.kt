package com.roubao.autopilot.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 头像管理器
 * 处理头像的选择、裁剪、保存等功能
 */
class AvatarManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AvatarManager"
        private const val AVATAR_DIR = "avatars"
    }
    
    /**
     * 从相册选择图片
     */
    suspend fun selectFromGallery(imageUri: Uri): String? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
            
            saveAvatar(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to select avatar from gallery", e)
            null
        }
    }
    
    /**
     * 拍照获取头像
     */
    suspend fun captureFromCamera(imageUri: Uri): String? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
            
            // 对图片进行适当压缩和裁剪
            val processedBitmap = processAvatarBitmap(bitmap)
            saveAvatar(processedBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture avatar from camera", e)
            null
        }
    }
    
    /**
     * 处理头像位图 - 压缩和标准化尺寸
     */
    private fun processAvatarBitmap(bitmap: Bitmap): Bitmap {
        val targetSize = 512 // 目标尺寸
        val targetRatio = 1.0f // 正方形比例
        
        // 计算缩放比例
        val scale = minOf(
            targetSize.toFloat() / bitmap.width,
            targetSize.toFloat() / bitmap.height
        )
        
        // 计算裁剪区域
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        
        val cropX = maxOf(0, (scaledWidth - targetSize) / 2)
        val cropY = maxOf(0, (scaledHeight - targetSize) / 2)
        
        // 创建目标位图
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        
        // 绘制裁剪后的图片
        val srcRect = android.graphics.Rect(cropX, cropY, cropX + targetSize, cropY + targetSize)
        val dstRect = android.graphics.Rect(0, 0, targetSize, targetSize)
        canvas.drawBitmap(bitmap, srcRect, dstRect, null)
        
        return result
    }
    
    /**
     * 保存头像到本地存储
     */
    suspend fun saveAvatar(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            // 确保使用正确的目录
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val avatarDir = File(picturesDir, AVATAR_DIR)
            if (!avatarDir.exists()) {
                avatarDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "avatar_$timestamp.jpg"
            val avatarFile = File(avatarDir, fileName)
            
            FileOutputStream(avatarFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }
            
            // 保存头像路径到SharedPreferences
            saveCurrentAvatarPath(avatarFile.absolutePath)
            
            // 清理旧头像文件
            cleanupOldAvatars(avatarFile.absolutePath)
            
            avatarFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save avatar", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied when saving avatar", e)
            null
        }
    }
    
    /**
     * 删除旧头像文件
     */
    fun deleteOldAvatar(avatarPath: String?) {
        if (avatarPath != null) {
            try {
                val file = File(avatarPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete old avatar", e)
            }
        }
    }
    
    /**
     * 获取默认头像路径
     */
    fun getDefaultAvatarPath(): String? {
        return null // 返回null表示使用默认头像
    }
    
    /**
     * 获取当前用户头像路径
     */
    fun getCurrentAvatarPath(): String? {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("current_avatar_path", null)
    }
    
    /**
     * 保存当前用户头像路径
     */
    fun saveCurrentAvatarPath(avatarPath: String?) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("current_avatar_path", avatarPath).apply()
    }
    
    /**
     * 清理旧头像文件
     */
    fun cleanupOldAvatars(keepPath: String?) {
        try {
            val avatarDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), AVATAR_DIR)
            if (avatarDir.exists()) {
                avatarDir.listFiles()?.forEach { file ->
                    if (file.absolutePath != keepPath && file.name.startsWith("avatar_")) {
                        file.delete()
                        Log.d(TAG, "Deleted old avatar: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old avatars", e)
        }
    }
}

/**
 * 头像选择状态
 */
sealed class AvatarSelectionState {
    object Idle : AvatarSelectionState()
    object Selecting : AvatarSelectionState()
    data class Selected(val imagePath: String) : AvatarSelectionState()
    data class Error(val message: String) : AvatarSelectionState()
}

/**
 * Composable中使用的头像选择器
 */
@Composable
fun rememberAvatarPicker(
    onAvatarSelected: (String) -> Unit,
    onAvatarError: (String) -> Unit = {}
): AvatarPickerState {
    val context = LocalContext.current
    val avatarManager = remember { AvatarManager(context) }
    
    var selectionState by remember { mutableStateOf<AvatarSelectionState>(AvatarSelectionState.Idle) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectionState = AvatarSelectionState.Selecting
            if (uri != null) {
                // 在协程中处理图片选择
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val imagePath = avatarManager.selectFromGallery(uri)
                        if (imagePath != null) {
                            selectionState = AvatarSelectionState.Selected(imagePath)
                            onAvatarSelected(imagePath)
                        } else {
                            selectionState = AvatarSelectionState.Error("选择头像失败")
                            onAvatarError("选择头像失败")
                        }
                    } catch (e: Exception) {
                        selectionState = AvatarSelectionState.Error(e.message ?: "未知错误")
                        onAvatarError(e.message ?: "未知错误")
                    }
                }
            } else {
                selectionState = AvatarSelectionState.Idle
            }
        }
    )
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                // 处理拍照结果
                selectionState = AvatarSelectionState.Selecting
                // 这里应该处理拍照后的图片，但现在暂时设置为空闲状态
                selectionState = AvatarSelectionState.Idle
            } else {
                selectionState = AvatarSelectionState.Idle
            }
        }
    )
    
    // 预先创建拍照所需的临时文件
    val photoFile = try {
        val avatarDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "avatars")
        if (!avatarDir.exists()) {
            avatarDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        File(avatarDir, "temp_photo_$timestamp.jpg")
    } catch (e: Exception) {
        Log.e("AvatarPicker", "Failed to create temp file", e)
        null
    }
    
    return remember {
        AvatarPickerState(
            selectionState = selectionState,
            pickFromGallery = {
                selectionState = AvatarSelectionState.Selecting
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            pickFromCamera = {
                if (photoFile != null) {
                    selectionState = AvatarSelectionState.Selecting
                    val photoUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    cameraLauncher.launch(photoUri)
                } else {
                    // 如果临时文件创建失败，直接设置为错误状态
                    selectionState = AvatarSelectionState.Error("无法创建拍照临时文件")
                    onAvatarError("无法创建拍照临时文件")
                }
            }
        )
    }
}

/**
 * 头像选择器状态
 */
data class AvatarPickerState(
    val selectionState: AvatarSelectionState,
    val pickFromGallery: () -> Unit,
    val pickFromCamera: () -> Unit
)