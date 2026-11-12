package com.pocketdev.advance_erp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pocketdev.advance_erp.data.entity.ImportShipment
import com.pocketdev.advance_erp.data.entity.ShipmentCarton
import com.pocketdev.advance_erp.data.entity.ShipmentFreightRate
import com.pocketdev.advance_erp.data.entity.ShipmentSupplier
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportShipmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipment(shipment: ImportShipment)

    @Update
    suspend fun updateShipment(shipment: ImportShipment)

    @Query("DELETE FROM import_shipments WHERE id = :shipmentId")
    suspend fun deleteShipment(shipmentId: String)

    @Query("SELECT * FROM import_shipments ORDER BY createdAt DESC")
    fun getAllShipments(): Flow<List<ImportShipment>>

    @Query("SELECT * FROM import_shipments WHERE id = :shipmentId")
    suspend fun getShipmentById(shipmentId: String): ImportShipment?

    @Query("SELECT * FROM import_shipments WHERE purchaseOrderId = :orderId ORDER BY shippingDate DESC")
    fun getShipmentsForOrder(orderId: String): Flow<List<ImportShipment>>

    @Query("SELECT * FROM import_shipments WHERE purchaseOrderId = :orderId ORDER BY shippingDate DESC")
    suspend fun getShipmentsByOrderIdSync(orderId: String): List<ImportShipment>

    @Query("SELECT * FROM import_shipments WHERE shipmentNumber = :shipmentNumber")
    suspend fun getShipmentByNumber(shipmentNumber: String): ImportShipment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCarton(carton: ShipmentCarton)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartons(cartons: List<ShipmentCarton>)

    @Query("DELETE FROM shipment_cartons WHERE shipmentId = :shipmentId")
    suspend fun deleteCartonsForShipment(shipmentId: String)

    @Query("DELETE FROM shipment_cartons WHERE id = :cartonId")
    suspend fun deleteCartonById(cartonId: String)

    @Query("DELETE FROM shipment_cartons WHERE purchaseOrderId = :orderId AND cartonNumber = :cartonNumber")
    suspend fun deleteCartonByNumber(orderId: String, cartonNumber: String)

    @Query("DELETE FROM shipment_cartons WHERE purchaseOrderId = :orderId")
    suspend fun deleteCartonsForOrder(orderId: String)

    @Query("SELECT * FROM shipment_cartons WHERE shipmentId = :shipmentId ORDER BY cartonNumber ASC")
    fun getCartonsForShipment(shipmentId: String): Flow<List<ShipmentCarton>>

    @Query("SELECT * FROM shipment_cartons WHERE shipmentId = :shipmentId ORDER BY cartonNumber ASC")
    suspend fun getCartonsForShipmentSync(shipmentId: String): List<ShipmentCarton>

    @Query("SELECT * FROM shipment_cartons WHERE purchaseOrderId = :orderId ORDER BY cartonNumber ASC")
    fun getCartonsForOrder(orderId: String): Flow<List<ShipmentCarton>>

    @Query("SELECT * FROM shipment_cartons WHERE purchaseOrderId = :orderId ORDER BY cartonNumber ASC")
    suspend fun getCartonsForOrderSync(orderId: String): List<ShipmentCarton>

    @Query("SELECT * FROM import_shipments WHERE purchaseOrderId = :orderId LIMIT 1")
    suspend fun getBatchShipmentForOrder(orderId: String): ImportShipment?

    @Query("SELECT COUNT(*) FROM import_shipments")
    suspend fun getShipmentCount(): Int

    @Query("UPDATE import_shipments SET status = :status, actualArrivalDate = :arrivalDate, updatedAt = :updatedAt WHERE id = :shipmentId")
    suspend fun updateShipmentStatus(
        shipmentId: String,
        status: String,
        arrivalDate: Long? = null,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Transaction
    suspend fun saveShipmentWithCartons(shipment: ImportShipment, cartons: List<ShipmentCarton>) {
        insertShipment(shipment)
        deleteCartonsForShipment(shipment.id)
        insertCartons(cartons)
    }

    // --- Shipment Suppliers (Multi-Supplier & China Local Delivery) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipmentSupplier(supplier: ShipmentSupplier)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipmentSuppliers(suppliers: List<ShipmentSupplier>)

    @Update
    suspend fun updateShipmentSupplier(supplier: ShipmentSupplier)

    @Query("DELETE FROM shipment_suppliers WHERE id = :id")
    suspend fun deleteShipmentSupplierById(id: String)

    @Query("DELETE FROM shipment_suppliers WHERE shipmentId = :shipmentId")
    suspend fun deleteSuppliersForShipment(shipmentId: String)

    @Query("SELECT * FROM shipment_suppliers WHERE shipmentId = :shipmentId ORDER BY createdAt ASC")
    fun getSuppliersForShipment(shipmentId: String): Flow<List<ShipmentSupplier>>

    @Query("SELECT * FROM shipment_suppliers WHERE shipmentId = :shipmentId ORDER BY createdAt ASC")
    suspend fun getSuppliersForShipmentSync(shipmentId: String): List<ShipmentSupplier>

    @Query("SELECT * FROM shipment_suppliers WHERE id = :id LIMIT 1")
    suspend fun getShipmentSupplierById(id: String): ShipmentSupplier?

    // --- Shipment Category Freight Rates (Two-Level Freight Management) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipmentFreightRate(rate: ShipmentFreightRate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShipmentFreightRates(rates: List<ShipmentFreightRate>)

    @Update
    suspend fun updateShipmentFreightRate(rate: ShipmentFreightRate)

    @Query("DELETE FROM shipment_freight_rates WHERE shipmentId = :shipmentId")
    suspend fun deleteFreightRatesForShipment(shipmentId: String)

    @Query("SELECT * FROM shipment_freight_rates WHERE shipmentId = :shipmentId ORDER BY categoryNameEn ASC")
    fun getFreightRatesForShipment(shipmentId: String): Flow<List<ShipmentFreightRate>>

    @Query("SELECT * FROM shipment_freight_rates WHERE shipmentId = :shipmentId ORDER BY categoryNameEn ASC")
    suspend fun getFreightRatesForShipmentSync(shipmentId: String): List<ShipmentFreightRate>

    @Query("SELECT * FROM shipment_freight_rates WHERE shipmentId = :shipmentId AND categoryId = :categoryId LIMIT 1")
    suspend fun getFreightRateForShipmentAndCategorySync(shipmentId: String, categoryId: String): ShipmentFreightRate?
}
