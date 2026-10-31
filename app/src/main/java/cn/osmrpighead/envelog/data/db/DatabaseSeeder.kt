package cn.osmrpighead.envelog.data.db

/**
 * 由构建变体提供数据库初始数据；release 没有实现，因此集合为空。
 */
fun interface DatabaseSeeder {
    suspend fun seedIfNeeded()
}
