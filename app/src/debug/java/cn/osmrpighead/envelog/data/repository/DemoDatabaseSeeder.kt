package cn.osmrpighead.envelog.data.repository

import androidx.room3.withWriteTransaction
import cn.osmrpighead.envelog.data.db.DatabaseSeeder
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class DemoDatabaseSeeder @Inject constructor(
    private val database: EnvelogDatabase,
    private val clock: Clock,
) : DatabaseSeeder {
    override suspend fun seedIfNeeded() {
        val seedDao = database.seedDao()
        if (seedDao.envelopeCount() > 0) return

        val seed = DemoSeedData.build(LocalDate.now(clock), clock.zone)
        database.withWriteTransaction {
            seedDao.insertEnvelopes(seed.envelopes)
            seedDao.insertBudgets(seed.budgets)
            seedDao.insertExpenditures(seed.expenditures)
            seedDao.insertAllocations(seed.allocations)
        }
    }
}
