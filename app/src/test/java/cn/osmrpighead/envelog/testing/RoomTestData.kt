package cn.osmrpighead.envelog.testing

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenseAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenseEntity
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenseId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * JVM 单元测试共用的数据库数据与内存库构造入口。
 *
 * 所有日期都落在 2026-10 这个固定月份上，[today] 固定为该月 20 日，
 * 使「当前预算区间」与「已结束预算」的边界不受运行时刻影响。
 */
object RoomTestData {
    val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    val month: YearMonth = YearMonth.of(2026, 10)
    val monthStart: LocalDate = LocalDate.of(2026, 10, 1)
    val monthEnd: LocalDate = LocalDate.of(2026, 11, 1)
    val today: LocalDate = LocalDate.of(2026, 10, 20)

    val envelopeA: EnvelopeId = envelopeId(0xA1)
    val envelopeB: EnvelopeId = envelopeId(0xA2)

    val budgetA: BudgetId = budgetId(0xD1)
    val budgetB: BudgetId = budgetId(0xD2)
    val budgetC: BudgetId = budgetId(0xD3)

    fun uuid(value: Long): UUID = UUID(0L, value)

    fun envelopeId(value: Long): EnvelopeId = EnvelopeId(uuid(value))
    fun budgetId(value: Long): BudgetId = BudgetId(uuid(value))
    fun expenseId(value: Long): ExpenseId = ExpenseId(uuid(value))

    fun createDatabase(): EnvelogDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        EnvelogDatabase::class.java,
    ).build()

    fun at(date: LocalDate, zone: ZoneId = RoomTestData.zone): Instant =
        date.atTime(LocalTime.NOON).atZone(zone).toInstant()

    fun envelope(
        id: EnvelopeId = envelopeA,
        name: String = "信封",
        isBudgeted: Boolean = true,
        archivedAt: Instant? = null,
    ): EnvelopeEntity = EnvelopeEntity(
        id = id,
        name = name,
        isBudgeted = isBudgeted,
        archivedAt = archivedAt,
    )

    fun budget(
        id: BudgetId = budgetA,
        envelopeId: EnvelopeId = envelopeA,
        start: LocalDate = monthStart,
        end: LocalDate = monthEnd,
        amount: Long = 100_000,
        attributedMonth: YearMonth = month,
        deletedAt: Instant? = null,
    ): BudgetEntity = BudgetEntity(
        id = id,
        envelopeId = envelopeId,
        dateSpan = HalfOpenDateSpan(start, end),
        attributedMonth = attributedMonth,
        amount = amount,
        deletedAt = deletedAt,
    )

    fun expense(
        id: ExpenseId = expenseId(0xC1),
        date: LocalDate = monthStart.plusDays(4),
        deletedAt: Instant? = null,
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        happenedAt = at(date),
        happenedDate = date,
        happenedZone = zone,
        deletedAt = deletedAt,
    )

    fun allocation(
        expenseId: ExpenseId,
        envelopeId: EnvelopeId = envelopeA,
        amount: Long = 10_000,
    ): ExpenseAllocationEntity = ExpenseAllocationEntity(
        expenseId = expenseId,
        envelopeId = envelopeId,
        amount = amount,
    )

    /**
     * 生成一条支出及其分摊，返回 (支出实体, 分摊实体列表)，便于直接拼进 seed 参数。
     */
    fun record(
        value: Long,
        date: LocalDate,
        vararg shares: Pair<EnvelopeId, Long>,
    ): Pair<ExpenseEntity, List<ExpenseAllocationEntity>> {
        val id = expenseId(value)
        return expense(id, date) to shares.map { (envelopeId, amount) ->
            allocation(id, envelopeId, amount)
        }
    }

}

/**
 * 按外键顺序把测试数据写进内存库。
 */
suspend fun EnvelogDatabase.seed(
    envelopes: List<EnvelopeEntity> = emptyList(),
    budgets: List<BudgetEntity> = emptyList(),
    expenses: List<ExpenseEntity> = emptyList(),
    allocations: List<ExpenseAllocationEntity> = emptyList(),
) {
    val dao = seedDao()
    if (envelopes.isNotEmpty()) dao.insertEnvelopes(envelopes)
    if (budgets.isNotEmpty()) dao.insertBudgets(budgets)
    if (expenses.isNotEmpty()) dao.insertExpenses(expenses)
    if (allocations.isNotEmpty()) dao.insertAllocations(allocations)
}
