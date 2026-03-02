package com.roubao.autopilot.data

import kotlinx.serialization.Serializable
/**
 * 用户角色枚举
 */
enum class UserRole(
    val displayName: String,
    val description: String
) {
    BEGINNER("新手用户", "适合刚接触AI助手的新手，提供详细的操作指导"),
    ADVANCED("高级用户", "适合熟悉AI助手的用户，提供简洁高效的交互体验"),
    DEVELOPER("开发者", "适合开发者和技术人员，提供技术细节和调试信息"),
    BUSINESS("商务人士", "适合商务场景，注重效率和专业性");

    companion object {
        fun fromDisplayName(displayName: String): UserRole? {
            return values().find { it.displayName == displayName }
        }
    }
}

/**
 * 用户信息数据类
 */
@Serializable
data class UserInfo(
    val userId: String,
    val username: String,
    val password: String,
    val email: String? = null,
    val role: UserRole = UserRole.BEGINNER,
    val avatarPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * 登录请求数据
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 注册请求数据
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String? = null,
    val password: String,
    val role: UserRole = UserRole.BEGINNER
)

/**
 * 认证结果
 */
@Serializable
data class AuthResult(
    val success: Boolean,
    val userInfo: UserInfo? = null,
    val errorMessage: String? = null
)

/**
 * 角色提示词模板
 */
data class RolePromptTemplate(
    val role: UserRole,
    val systemPrompt: String,
    val userPromptPrefix: String = ""
)

/**
 * 不同角色的提示词配置
 */
object RolePrompts {
    val TEMPLATES = mapOf(
        UserRole.BEGINNER to RolePromptTemplate(
            role = UserRole.BEGINNER,
            systemPrompt = """
                你是一个友好的AI助手，正在帮助一位新手用户使用手机。
                
                请遵循以下原则：
                1. 使用简单易懂的语言解释每一步操作
                2. 详细说明为什么要这样做
                3. 提供清晰的步骤指引
                4. 耐心回答用户的疑问
                5. 避免使用过于专业的术语
                
                示例回应风格：
                "好的，我来帮您打开微信。首先我会在屏幕上找到微信图标，然后点击它。请您稍等..."
            """.trimIndent()
        ),
        
        UserRole.ADVANCED to RolePromptTemplate(
            role = UserRole.ADVANCED,
            systemPrompt = """
                你是一个高效的AI助手，服务于经验丰富的用户。
                
                请遵循以下原则：
                1. 直接执行用户指令，无需过多解释
                2. 快速准确地完成任务
                3. 只在必要时提供简要说明
                4. 专注于效率和结果
                
                示例回应风格：
                "正在为您打开微信并发送消息..."
            """.trimIndent()
        ),
        
        UserRole.DEVELOPER to RolePromptTemplate(
            role = UserRole.DEVELOPER,
            systemPrompt = """
                你是一个技术型AI助手，服务于开发者用户。
                
                请遵循以下原则：
                1. 提供技术细节和实现原理
                2. 解释使用的API和方法
                3. 在适当时候给出调试建议
                4. 使用专业但清晰的技术语言
                
                示例回应风格：
                "正在调用AccessibilityService API定位微信应用，使用findViewById方法查找发送按钮..."
            """.trimIndent()
        ),
        
        UserRole.BUSINESS to RolePromptTemplate(
            role = UserRole.BUSINESS,
            systemPrompt = """
                你是一个专业的商务助理AI，服务于商务人士。
                
                请遵循以下原则：
                1. 保持专业和正式的语调
                2. 注重效率和时间管理
                3. 提供商务场景下的最佳实践建议
                4. 确保操作的安全性和合规性
                
                示例回应风格：
                "遵照您的指示，正在启动企业微信应用以处理您的商务通讯..."
            """.trimIndent()
        )
    )
    
    /**
     * 根据用户角色获取对应的系统提示词
     */
    fun getSystemPrompt(role: UserRole): String {
        return TEMPLATES[role]?.systemPrompt ?: TEMPLATES[UserRole.BEGINNER]!!.systemPrompt
    }
    
    /**
     * 获取当前用户角色的完整提示词配置
     */
    fun getTemplate(role: UserRole): RolePromptTemplate {
        return TEMPLATES[role] ?: TEMPLATES[UserRole.BEGINNER]!!
    }
}