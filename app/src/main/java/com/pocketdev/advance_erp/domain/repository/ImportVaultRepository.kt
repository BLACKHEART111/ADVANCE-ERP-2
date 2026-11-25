package com.pocketdev.advance_erp.domain.repository

import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.data.entity.ExpenseAllocation
import com.pocketdev.advance_erp.data.entity.ExpenseAllocationRule
import com.pocketdev.advance_erp.data.entity.ImportVaultAttachment
import com.pocketdev.advance_erp.data.entity.ImportVaultEntry
import com.pocketdev.advance_erp.domain.model.ImportVaultExportSource
import com.pocketdev.advance_erp.domain.model.ImportVaultFilter
import com.pocketdev.advance_erp.domain.model.ImportVaultFilterOptions
import com.pocketdev.advance_erp.domain.model.ImportVaultOverview
import com.pocketdev.advance_erp.domain.model.ProductVaultRow
import com.pocketdev.advance_erp.domain.model.PurchaseOrderVaultDetail
import com.pocketdev.advance_erp.domain.model.ShipmentVaultDetail
import com.pocketdev.advance_erp.domain.model.VaultScopeType
import kotlinx.coroutines.flow.Flow

interface ImportVaultRepository {
    suspend fun getOverview(role: UserRole, filter: ImportVaultFilter = ImportVaultFilter()): ImportVaultOverview
    suspend fun getFilterOptions(role: UserRole): ImportVaultFilterOptions
    suspend fun getShipmentDetail(role: UserRole, shipmentId: String, filter: ImportVaultFilter = ImportVaultFilter()): ShipmentVaultDetail?
    suspend fun getPurchaseOrderDetail(role: UserRole, purchaseOrderId: String, filter: ImportVaultFilter = ImportVaultFilter()): PurchaseOrderVaultDetail?
    suspend fun getProductProfitability(role: UserRole, filter: ImportVaultFilter = ImportVaultFilter()): List<ProductVaultRow>
    suspend fun loadExportSource(role: UserRole): ImportVaultExportSource

    suspend fun syncVaultEntries(role: UserRole = UserRole.OWNER_ADMIN)
    fun observeVaultEntries(role: UserRole = UserRole.OWNER_ADMIN): Flow<List<ImportVaultEntry>>
    suspend fun updateVaultEntryMetadata(
        role: UserRole = UserRole.OWNER_ADMIN,
        scopeType: VaultScopeType,
        sourceId: String,
        archiveStatus: String? = null,
        isPinned: Boolean? = null,
        tagsJson: String? = null,
        notes: String? = null,
        displayTitle: String? = null
    )

    fun observeAttachments(
        role: UserRole = UserRole.OWNER_ADMIN,
        scopeType: String,
        sourceId: String
    ): Flow<List<ImportVaultAttachment>>

    suspend fun addAttachment(
        role: UserRole = UserRole.OWNER_ADMIN,
        scopeType: String,
        sourceId: String,
        category: String,
        title: String,
        filePath: String,
        mimeType: String = "application/octet-stream",
        fileSizeBytes: Long = 0L,
        isImmutableSnapshot: Boolean = false,
        createdBy: String = role.code
    ): String

    suspend fun deleteAttachment(role: UserRole = UserRole.OWNER_ADMIN, attachmentId: String)

    fun observeExpenseAllocationRules(role: UserRole = UserRole.OWNER_ADMIN): Flow<List<ExpenseAllocationRule>>
    suspend fun createOrActivateExpenseAllocationRule(
        role: UserRole,
        ruleName: String,
        allocationBasis: String,
        periodStartMillis: Long,
        periodEndMillis: Long,
        targetScopeType: VaultScopeType = VaultScopeType.SHIPMENT,
        activateImmediately: Boolean = true,
        notes: String? = null
    ): String
    suspend fun setExpenseAllocationRuleActive(role: UserRole, ruleId: String, isActive: Boolean)
    suspend fun previewRuleAllocations(role: UserRole, rule: ExpenseAllocationRule): List<ExpenseAllocation>
}
