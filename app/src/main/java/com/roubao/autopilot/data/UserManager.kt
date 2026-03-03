package com.roubao.autopilot.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.UUID

/**
 * 用户管理器
 * 处理用户注册、登录、登出等认证相关功能
 */
class UserManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    // 加密存储用户敏感信息
    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                "user_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to create encrypted prefs", e)
            prefs
        }
    }
    
    // 当前用户状态流
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser
    
    // 是否已登录状态流
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
    
    init {
        // 应用启动时检查是否有已登录用户
        checkExistingSession()
    }
    
    /**
     * 检查是否存在有效的登录会话
     */
    private fun checkExistingSession() {
        val savedUserId = prefs.getString("current_user_id", null)
        if (savedUserId != null) {
            val userInfoJson = securePrefs.getString("user_$savedUserId", null)
            if (userInfoJson != null) {
                try {
                    // 这里应该使用JSON解析，简化处理
                    val userInfo = deserializeUserInfo(userInfoJson)
                    if (userInfo != null && userInfo.isActive) {
                        _currentUser.value = userInfo
                        _isLoggedIn.value = true
                        Log.d("UserManager", "Restored session for user: ${userInfo.username}")
                    } else {
                        // 用户无效，清理会话
                        clearSession()
                    }
                } catch (e: Exception) {
                    Log.e("UserManager", "Failed to restore session", e)
                    clearSession()
                }
            }
        }
    }
    
    /**
     * 用户注册
     */
    fun register(request: RegisterRequest): AuthResult {
        return try {
            // 检查用户名是否已存在
            if (isUsernameExists(request.username)) {
                return AuthResult(
                    success = false,
                    errorMessage = "用户名已存在"
                )
            }
            
            // 生成用户ID
            val userId = UUID.randomUUID().toString()
            
            // 生成API密钥（简化版本，实际应更复杂）

            val passwordHash = generatePasswordHash(request.password, "default_salt")
            // 创建用户信息
            val userInfo = UserInfo(
                userId = userId,
                username = request.username,
                email = request.email,
                role = request.role,
                password = passwordHash
            )
            
            // 保存用户信息
            saveUserInfo(userInfo)
            
            // 自动登录
            setCurrentUser(userInfo)
            
            AuthResult(
                success = true,
                userInfo = userInfo
            )
        } catch (e: Exception) {
            Log.e("UserManager", "Registration failed", e)
            AuthResult(
                success = false,
                errorMessage = "注册失败: ${e.message}"
            )
        }
    }
    
    /**
     * 用户登录
     */
    fun login(username: String, password: String): AuthResult {
        return try {
            val userInfo = getUserByUsername(username)
            
            if (userInfo == null) {
                return AuthResult(
                    success = false,
                    errorMessage = "用户不存在"
                )
            }
            
            // 验证密码（这里简化处理，实际应该使用哈希验证）
            val expectedPassword = generatePasswordHash(password, "default_salt") // 简化版本
            if (userInfo.password != expectedPassword) { // 临时密码，实际应该验证哈希
                return AuthResult(
                    success = false,
                    errorMessage = "密码错误"
                )
            }
            
            // 更新最后登录时间
            val updatedUserInfo = userInfo.copy(
                lastLoginAt = System.currentTimeMillis()
            )
            
            // 保存更新后的用户信息
            saveUserInfo(updatedUserInfo)
            
            // 设置当前用户
            setCurrentUser(updatedUserInfo)
            
            AuthResult(
                success = true,
                userInfo = updatedUserInfo
            )
        } catch (e: Exception) {
            Log.e("UserManager", "Login failed", e)
            AuthResult(
                success = false,
                errorMessage = "登录失败: ${e.message}"
            )
        }
    }
    
    /**
     * 用户登出
     */
    fun logout() {
        clearSession()
        Log.d("UserManager", "User logged out")
    }
    
    /**
     * 更新用户角色
     */
    fun updateUserRole(newRole: UserRole): Boolean {
        val currentUser = _currentUser.value ?: return false
        
        return try {
            val updatedUser = currentUser.copy(role = newRole)
            saveUserInfo(updatedUser)
            setCurrentUser(updatedUser)
            true
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to update user role", e)
            false
        }
    }
    
    /**
     * 更新用户头像路径
     */
    fun updateUserAvatar(avatarPath: String?): Boolean {
        val currentUser = _currentUser.value ?: return false
        
        return try {
            val updatedUser = currentUser.copy(avatarPath = avatarPath)
            saveUserInfo(updatedUser)
            setCurrentUser(updatedUser)
            
            // 同时保存到 SharedPreferences 用于快速访问
            prefs.edit().putString("current_user_avatar", avatarPath).apply()
            
            Log.d("UserManager", "Avatar updated: $avatarPath")
            true
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to update user avatar", e)
            false
        }
    }
    
    /**
     * 获取当前用户头像路径
     */
    fun getCurrentUserAvatar(): String? {
        return _currentUser.value?.avatarPath ?: prefs.getString("current_user_avatar", null)
    }
    
    /**
     * 清理用户头像
     */
    fun clearUserAvatar(): Boolean {
        val currentUser = _currentUser.value ?: return false
        
        return try {
            val updatedUser = currentUser.copy(avatarPath = null)
            saveUserInfo(updatedUser)
            setCurrentUser(updatedUser)
            
            // 清理 SharedPreferences 中的头像路径
            prefs.edit().remove("current_user_avatar").apply()
            
            Log.d("UserManager", "Avatar cleared")
            true
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to clear user avatar", e)
            false
        }
    }

    
    /**
     * 获取当前用户的系统提示词
     */
    fun getCurrentUserSystemPrompt(): String {
        val user = _currentUser.value
        return if (user != null) {
            RolePrompts.getSystemPrompt(user.role)
        } else {
            RolePrompts.getSystemPrompt(UserRole.STUDENT)
        }
    }
    
    /**
     * 获取当前用户的角色提示词模板
     */
    fun getCurrentUserPromptTemplate(): RolePromptTemplate {
        val user = _currentUser.value
        return if (user != null) {
            RolePrompts.getTemplate(user.role)
        } else {
            RolePrompts.getTemplate(UserRole.STUDENT)
        }
    }
    
    // 私有辅助方法
    
    private fun setCurrentUser(userInfo: UserInfo) {
        _currentUser.value = userInfo
        _isLoggedIn.value = true
        prefs.edit().putString("current_user_id", userInfo.userId).apply()
    }
    
    private fun clearSession() {
        _currentUser.value = null
        _isLoggedIn.value = false
        prefs.edit().remove("current_user_id").apply()
    }
    
    private fun isUsernameExists(username: String): Boolean {
        return prefs.getStringSet("usernames", emptySet())?.contains(username) == true
    }
    
    private fun saveUserInfo(userInfo: UserInfo) {
        // 保存用户名列表
        val usernames = prefs.getStringSet("usernames", mutableSetOf())?.toMutableSet()
        usernames?.add(userInfo.username)
        prefs.edit().putStringSet("usernames", usernames).apply()
        
        // 保存用户信息（序列化为字符串，简化处理）
        val userInfoJson = serializeUserInfo(userInfo)
        securePrefs.edit().putString("user_${userInfo.userId}", userInfoJson).apply()
    }
    
    private fun getUserByUsername(username: String): UserInfo? {
        val usernames = prefs.getStringSet("usernames", emptySet()) ?: return null
        if (!usernames.contains(username)) return null
        
        // 查找对应的用户ID
        for (key in securePrefs.all.keys) {
            if (key.startsWith("user_")) {
                val userInfoJson = securePrefs.getString(key, null)
                if (userInfoJson != null) {
                    val userInfo = deserializeUserInfo(userInfoJson)
                    if (userInfo?.username == username) {
                        return userInfo
                    }
                }
            }
        }
        return null
    }

    private fun serializeUserInfo(userInfo: UserInfo): String {
        return "${userInfo.userId}|${userInfo.username}|${userInfo.email ?: ""}|${userInfo.role.name}|" +
               "${userInfo.createdAt}|${userInfo.lastLoginAt}|${userInfo.isActive}|${userInfo.avatarPath ?: ""}|" +
                userInfo.password
    }
    
    private fun deserializeUserInfo(data: String): UserInfo? {
        return try {
            val parts = data.split("|")
            if (parts.size >= 9) {
                UserInfo(
                    userId = parts[0],
                    username = parts[1],
                    email = if (parts[2].isNotEmpty()) parts[2] else null,
                    role = UserRole.valueOf(parts[3]),
                    createdAt = parts[4].toLong(),
                    lastLoginAt = parts[5].toLong(),
                    isActive = parts[6].toBoolean(),
                    avatarPath = if (parts[7].isNotEmpty()) parts[7] else null,
                    password = parts[8]
                )
            } else if (parts.size >= 8) {
                // 兼容旧版本数据格式
                UserInfo(
                    userId = parts[0],
                    username = parts[1],
                    email = if (parts[2].isNotEmpty()) parts[2] else null,
                    role = UserRole.valueOf(parts[3]),
                    createdAt = parts[4].toLong(),
                    lastLoginAt = parts[5].toLong(),
                    isActive = parts[6].toBoolean(),
                    avatarPath = null,
                    password = parts[7]
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserManager", "Failed to deserialize user info", e)
            null
        }
    }

    private fun generatePasswordHash(password: String, salt: String): String {
        return hashString("$password$salt")
    }
    
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: UserManager? = null
        
        fun getInstance(context: Context): UserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}