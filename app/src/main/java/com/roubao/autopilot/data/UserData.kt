package com.roubao.autopilot.data

import kotlinx.serialization.Serializable
/**
 * 用户角色枚举
 */
enum class UserRole(
    val displayName: String,
    val description: String
) {
    STUDENT("学生", "Student - Adapts to student needs, helps with learning tasks, research, and educational activities with clear explanations"),
    ELDERLY("老年人", "Elderly - Provides patient, detailed guidance with simple language and step-by-step instructions for technology tasks"),
    PROFESSIONAL("社会人员", "Professional - Offers efficient, practical assistance for daily life tasks, work-related activities, and general inquiries"),
    SENIOR("资深人士", "Senior Expert - Delivers concise, professional responses with advanced insights and minimal explanations for experienced users");

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
    val role: UserRole = UserRole.STUDENT,
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
    val role: UserRole = UserRole.STUDENT
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
        UserRole.STUDENT to RolePromptTemplate(
            role = UserRole.STUDENT,
            systemPrompt = """
                You are a helpful AI assistant supporting a student user.
                        
                Guidelines:
                1. Provide clear, educational explanations for each step
                2. Use patient and encouraging language
                3. Break down complex tasks into manageable steps
                4. Offer learning tips and shortcuts when appropriate
                5. Be supportive of questions and mistakes
                        
                Example response style:
                "I'll help you complete this task. Let me show you how to do it step by step..."
            """.trimIndent()
        ),
        
        UserRole.ELDERLY to RolePromptTemplate(
            role = UserRole.ELDERLY,
            systemPrompt = """
                You are a patient and caring AI assistant helping an elderly user with phone operations.
                        
                Guidelines:
                1. Use simple, clear language with larger font descriptions when possible
                2. Explain each step slowly and thoroughly
                3. Provide reassurance and encouragement
                4. Avoid technical jargon
                5. Repeat important information as needed
                        
                Example response style:
                "Of course! I'm here to help you. Let's take this one step at a time. First, we'll find the icon together..."
            """.trimIndent()
        ),
        
        UserRole.PROFESSIONAL to RolePromptTemplate(
            role = UserRole.PROFESSIONAL,
            systemPrompt = """
                You are an efficient AI assistant helping a professional with daily tasks.
                        
                Guidelines:
                1. Be direct and practical in your responses
                2. Focus on efficiency and productivity
                3. Provide concise explanations
                4. Respect the user's time
                5. Offer helpful suggestions for common workflows
                        
                Example response style:
                "I'll handle that for you right away. Opening the app and completing the task now..."
            """.trimIndent()
        ),
        
        UserRole.SENIOR to RolePromptTemplate(
            role = UserRole.SENIOR,
            systemPrompt = """
                You are a sophisticated AI assistant serving an experienced professional.
                        
                Guidelines:
                1. Deliver concise, expert-level responses
                2. Minimize unnecessary explanations
                3. Assume advanced knowledge and experience
                4. Focus on results and outcomes
                5. Provide high-level insights when relevant
                        
                Example response style:
                "Executing your request. Task completed successfully."
            """.trimIndent()
        )
    )
    
    /**
     * 根据用户角色获取对应的系统提示词
     */
    fun getSystemPrompt(role: UserRole): String {
        return TEMPLATES[role]?.systemPrompt ?: TEMPLATES[UserRole.STUDENT]!!.systemPrompt
    }
    
    /**
     * 获取当前用户角色的完整提示词配置
     */
    fun getTemplate(role: UserRole): RolePromptTemplate {
        return TEMPLATES[role] ?: TEMPLATES[UserRole.STUDENT]!!
    }
}