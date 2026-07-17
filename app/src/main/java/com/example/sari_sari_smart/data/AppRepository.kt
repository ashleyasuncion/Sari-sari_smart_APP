package com.example.sari_sari_smart.data

import com.example.sari_sari_smart.data.local.dao.*
import com.example.sari_sari_smart.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository that wraps all Room DAOs and converts between Room entities and domain models.
 * This is the single source of truth for data access.
 */
class AppRepository(
    private val productDao: ProductDao,
    private val dailyEntryDao: DailyEntryDao,
    private val specificSaleDao: SpecificSaleDao,
    private val customerDebtDao: CustomerDebtDao,
    private val endOfDayDao: EndOfDayDao,
    private val restockLogDao: RestockLogDao
) {
    // ── Products ────────────────────────────────────────────────────────
    fun getAllProducts(): Flow<List<Product>> =
        productDao.getAllProducts().map { entities -> entities.map { it.toDomainModel() } }

    suspend fun getProductById(id: Int): Product? =
        productDao.getProductById(id)?.toDomainModel()

    suspend fun saveProduct(product: Product) =
        productDao.insertProduct(ProductEntity.fromDomainModel(product))

    suspend fun saveProducts(products: List<Product>) =
        productDao.insertProducts(products.map { ProductEntity.fromDomainModel(it) })

    suspend fun deleteProduct(id: Int) = productDao.deleteProduct(id)

    suspend fun deleteAllProducts() = productDao.deleteAll()

    suspend fun searchProducts(query: String): List<Product> =
        productDao.searchProducts(query).map { it.toDomainModel() }

    // ── Daily Entries ───────────────────────────────────────────────────
    fun getLatestDailyEntry(): Flow<DailyEntry?> =
        dailyEntryDao.getLatestEntry().map { it?.toDomainModel() }

    suspend fun getDailyEntryByDate(date: String): DailyEntry? =
        dailyEntryDao.getByDate(date)?.toDomainModel()

    suspend fun saveDailyEntry(entry: DailyEntry) =
        dailyEntryDao.insertEntry(DailyEntryEntity.fromDomainModel(entry))

    // ── Specific Sales ──────────────────────────────────────────────────
    fun getAllSpecificSales(): Flow<List<SpecificSale>> =
        specificSaleDao.getAllSales().map { entities -> entities.map { it.toDomainModel() } }

    fun getSpecificSalesByDate(date: String): Flow<List<SpecificSale>> =
        specificSaleDao.getSalesByDate(date).map { entities -> entities.map { it.toDomainModel() } }

    suspend fun saveSpecificSale(sale: SpecificSale) =
        specificSaleDao.insertSale(SpecificSaleEntity.fromDomainModel(sale))

    suspend fun saveSpecificSales(sales: List<SpecificSale>) =
        specificSaleDao.insertSales(sales.map { SpecificSaleEntity.fromDomainModel(it) })

    suspend fun deleteAllSpecificSales() = specificSaleDao.deleteAll()

    // ── Debts ───────────────────────────────────────────────────────────
    fun getAllDebts(): Flow<List<CustomerDebt>> =
        customerDebtDao.getAllDebts().map { entities -> entities.map { it.toDomainModel() } }

    suspend fun getDebtById(id: Int): CustomerDebt? =
        customerDebtDao.getDebtById(id)?.toDomainModel()

    suspend fun saveDebt(debt: CustomerDebt) =
        customerDebtDao.insertDebt(CustomerDebtEntity.fromDomainModel(debt))

    suspend fun saveDebts(debts: List<CustomerDebt>) =
        customerDebtDao.insertDebts(debts.map { CustomerDebtEntity.fromDomainModel(it) })

    suspend fun updateDebt(debt: CustomerDebt) =
        customerDebtDao.updateDebt(CustomerDebtEntity.fromDomainModel(debt))

    suspend fun deleteDebt(id: Int) = customerDebtDao.deleteDebt(id)

    suspend fun deleteAllDebts() = customerDebtDao.deleteAll()

    suspend fun getUsedCustomerNames(): List<String> =
        customerDebtDao.getUsedCustomerNames()

    // ── End-of-Day Data ─────────────────────────────────────────────────
    fun getLatestEndOfDayData(): Flow<EndOfDayData?> =
        endOfDayDao.getLatest().map { it?.toDomainModel() }

    suspend fun getEndOfDayByDate(date: String): EndOfDayData? =
        endOfDayDao.getByDate(date)?.toDomainModel()

    suspend fun saveEndOfDayData(data: EndOfDayData) =
        endOfDayDao.insert(EndOfDayEntity.fromDomainModel(data))

    suspend fun deleteEndOfDayData(date: String) =
        endOfDayDao.deleteByDate(date)

    // ── Restock Log ─────────────────────────────────────────────────────
    fun getAllRestockLogs(): Flow<List<RestockLogEntry>> =
        restockLogDao.getAll().map { entities -> entities.map { it.toDomainModel() } }

    fun getLatestRestockLog(): Flow<RestockLogEntry?> =
        restockLogDao.getLatest().map { it?.toDomainModel() }

    suspend fun saveRestockLog(entry: RestockLogEntry) =
        restockLogDao.insert(RestockLogEntity.fromDomainModel(entry))

    suspend fun deleteAllRestockLogs() = restockLogDao.deleteAll()

    // ── Batch operations ────────────────────────────────────────────────
    suspend fun deleteAll() {
        deleteAllProducts()
        deleteAllSpecificSales()
        deleteAllDebts()
        dailyEntryDao.deleteAll()
        endOfDayDao.deleteAll()
        deleteAllRestockLogs()
    }
}
