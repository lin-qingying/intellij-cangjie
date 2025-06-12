package cn.cangnova.repository

import cn.cangnova.model.AdminUser
import cn.cangnova.model.CreateUserRequest
import cn.cangnova.model.UpdateUserRequest

/**
 * 管理员用户仓库接口
 */
interface AdminUserRepository {
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象，如果不存在则返回null
     */
    suspend fun findByUsername(username: String): AdminUser?
    
    /**
     * 创建用户
     * @param request 创建用户请求
     * @return 创建的用户对象
     */
    suspend fun createUser(request: CreateUserRequest): AdminUser
    
    /**
     * 更新用户
     * @param username 用户名
     * @param request 更新用户请求
     * @return 更新后的用户对象，如果用户不存在则返回null
     */
    suspend fun updateUser(username: String, request: UpdateUserRequest): AdminUser?
    
    /**
     * 删除用户
     * @param username 用户名
     * @return 是否删除成功
     */
    suspend fun deleteUser(username: String): Boolean
    
    /**
     * 获取所有用户
     * @param page 页码
     * @param pageSize 每页大小
     * @return 用户列表
     */
    suspend fun getAllUsers(page: Int = 1, pageSize: Int = 20): List<AdminUser>
    
    /**
     * 获取用户总数
     * @return 用户总数
     */
    suspend fun getUserCount(): Int
    
    /**
     * 验证用户凭证
     * @param username 用户名
     * @param password 密码
     * @return 如果验证成功则返回用户对象，否则返回null
     */
    suspend fun validateCredentials(username: String, password: String): AdminUser?
    
    /**
     * 更新用户最后登录时间
     * @param username 用户名
     * @return 是否更新成功
     */
    suspend fun updateLastLoginTime(username: String): Boolean
    
    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否已存在
     */
    suspend fun usernameExists(username: String): Boolean
    
    /**
     * 生成密码哈希
     * @param password 明文密码
     * @return 密码哈希
     */
    fun hashPassword(password: String): String
    
    /**
     * 验证密码
     * @param password 明文密码
     * @param hash 密码哈希
     * @return 是否匹配
     */
    fun verifyPassword(password: String, hash: String): Boolean
} 