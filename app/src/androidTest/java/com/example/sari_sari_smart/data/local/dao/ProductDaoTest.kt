package com.example.sari_sari_smart.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sari_sari_smart.data.local.AppDatabase
import com.example.sari_sari_smart.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProductDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.productDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetProductById() = runBlocking {
        val product = ProductEntity(1, "Test Item", 10, 15.0, 20.0)
        dao.insertProduct(product)

        val loaded = dao.getProductById(1)
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("Test Item", it.name)
            assertEquals(10, it.quantity)
            assertEquals(15.0, it.costPrice, 0.001)
            assertEquals(20.0, it.sellingPrice, 0.001)
        }
    }

    @Test
    fun insertAndGetAllProducts() = runBlocking {
        val product1 = ProductEntity(1, "Item A", 10, 15.0, 20.0)
        val product2 = ProductEntity(2, "Item B", 5, 8.0, 12.0)
        dao.insertProduct(product1)
        dao.insertProduct(product2)

        val allProducts = dao.getAllProducts().first()
        assertEquals(2, allProducts.size)
        assertTrue(allProducts.any { it.name == "Item A" })
        assertTrue(allProducts.any { it.name == "Item B" })
    }

    @Test
    fun insertProducts_BulkInsert() = runBlocking {
        val products = listOf(
            ProductEntity(1, "Item A", 10, 15.0, 20.0),
            ProductEntity(2, "Item B", 5, 8.0, 12.0),
            ProductEntity(3, "Item C", 20, 30.0, 45.0)
        )
        dao.insertProducts(products)

        val allProducts = dao.getAllProducts().first()
        assertEquals(3, allProducts.size)
    }

    @Test
    fun searchProductsByName() = runBlocking {
        val products = listOf(
            ProductEntity(1, "Canned Sardines", 15, 15.0, 20.0),
            ProductEntity(2, "Instant Noodles", 8, 8.0, 12.0),
            ProductEntity(3, "Cooking Oil", 3, 30.0, 42.0)
        )
        dao.insertProducts(products)

        val results = dao.searchProducts("Sardines")
        assertEquals(1, results.size)
        assertEquals("Canned Sardines", results[0].name)
    }

    @Test
    fun searchProducts_PartialMatch() = runBlocking {
        val products = listOf(
            ProductEntity(1, "Canned Sardines", 15, 15.0, 20.0),
            ProductEntity(2, "Canned Corned Beef", 10, 25.0, 35.0),
            ProductEntity(3, "Cooking Oil", 3, 30.0, 42.0)
        )
        dao.insertProducts(products)

        val results = dao.searchProducts("Canned")
        assertEquals(2, results.size)
    }

    @Test
    fun searchProducts_NoMatch() = runBlocking {
        val products = listOf(
            ProductEntity(1, "Canned Sardines", 15, 15.0, 20.0)
        )
        dao.insertProducts(products)

        val results = dao.searchProducts("NonExistent")
        assertTrue(results.isEmpty())
    }

    @Test
    fun searchProducts_CaseInsensitive() = runBlocking {
        val products = listOf(
            ProductEntity(1, "Canned Sardines", 15, 15.0, 20.0)
        )
        dao.insertProducts(products)

        val results = dao.searchProducts("sardines")
        assertEquals(1, results.size)
    }

    @Test
    fun updateProduct() = runBlocking {
        val product = ProductEntity(1, "Test Item", 10, 15.0, 20.0)
        dao.insertProduct(product)

        val updated = product.copy(quantity = 25, sellingPrice = 25.0)
        dao.updateProduct(updated)

        val loaded = dao.getProductById(1)
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals(25, it.quantity)
            assertEquals(25.0, it.sellingPrice, 0.001)
        }
    }

    @Test
    fun deleteProduct() = runBlocking {
        val product = ProductEntity(1, "Test Item", 10, 15.0, 20.0)
        dao.insertProduct(product)
        assertNotNull(dao.getProductById(1))

        dao.deleteProduct(1)
        assertNull(dao.getProductById(1))
    }

    @Test
    fun deleteAll() = runBlocking {
        val products = listOf(
            ProductEntity(1, "Item A", 10, 15.0, 20.0),
            ProductEntity(2, "Item B", 5, 8.0, 12.0)
        )
        dao.insertProducts(products)
        assertEquals(2, dao.getAllProducts().first().size)

        dao.deleteAll()
        assertTrue(dao.getAllProducts().first().isEmpty())
    }

    @Test
    fun getAllProducts_Empty() = runBlocking {
        val allProducts = dao.getAllProducts().first()
        assertTrue(allProducts.isEmpty())
    }

    @Test
    fun getProductById_NonExistent() = runBlocking {
        val loaded = dao.getProductById(999)
        assertNull(loaded)
    }

    @Test
    fun insertProduct_ReplacesExistingOnConflict() = runBlocking {
        val product = ProductEntity(1, "Original", 10, 15.0, 20.0)
        dao.insertProduct(product)

        val replacement = ProductEntity(1, "Replaced", 20, 30.0, 40.0)
        dao.insertProduct(replacement)

        val loaded = dao.getProductById(1)
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("Replaced", it.name)
            assertEquals(20, it.quantity)
        }
    }
}
