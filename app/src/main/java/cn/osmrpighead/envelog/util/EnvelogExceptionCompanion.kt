package cn.osmrpighead.envelog.util

open class EnvelogExceptionCompanion<E : Throwable>(private val type: Class<E>) {
    fun <T> catch(function: () -> T): Result<T> {
        return try {
            Result.success(function())
        } catch (t: Throwable) {
            if (type.isInstance(t)) Result.failure(t)
            else throw t
        }
    }
}