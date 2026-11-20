package com.pocketdev.advance_erp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.security.UserRole
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.dao.CustomerDao
import com.pocketdev.advance_erp.data.dao.ProductDao
import com.pocketdev.advance_erp.data.dao.SupplierDao
import com.pocketdev.advance_erp.data.dao.WarehouseDao
import com.pocketdev.advance_erp.data.entity.Warehouse
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class DrawerMetricsState(
    val activeWarehouseName: String? = null,
    val hasActiveWarehouse: Boolean = false,
    val productCount: Int = 0,
    val customerCount: Int = 0,
    val supplierCount: Int = 0,
    val userRole: UserRole = UserRole.OWNER_ADMIN,
    val isSyncing: Boolean = false
)

@HiltViewModel
class MainDrawerViewModel @Inject constructor(
    warehouseDao: WarehouseDao,
    productDao: ProductDao,
    customerDao: CustomerDao,
    supplierDao: SupplierDao,
    preferencesRepository: UserPreferencesRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)

    val state: StateFlow<DrawerMetricsState> = combine(
        warehouseDao.getAllWarehouses(),
        productDao.getProductCount(),
        customerDao.getCustomerCount(),
        supplierDao.getSupplierCount(),
        preferencesRepository.selectedUserRole,
        _isSyncing
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val warehouses = args[0] as? List<Warehouse> ?: emptyList()
        val (activeName, hasActive) = resolveActiveWarehouse(warehouses)
        DrawerMetricsState(
            activeWarehouseName = activeName,
            hasActiveWarehouse = hasActive,
            productCount = (args[1] as? Int) ?: 0,
            customerCount = (args[2] as? Int) ?: 0,
            supplierCount = (args[3] as? Int) ?: 0,
            userRole = (args[4] as? UserRole) ?: UserRole.OWNER_ADMIN,
            isSyncing = (args[5] as? Boolean) ?: false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DrawerMetricsState()
    )

    fun triggerSync(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        viewModelScope.launch {
            try {
                backupRepository.createBackup(
                    isAutomatic = false,
                    notes = "Drawer manual sync"
                )
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onFailure(e.message ?: "Sync failed")
            } finally {
                _isSyncing.value = false
            }
        }
    }
}

internal fun resolveActiveWarehouse(warehouses: List<Warehouse>): Pair<String?, Boolean> {
    val active = warehouses.filter { it.isActive }
    val selected = active.firstOrNull { it.isDefault } ?: active.firstOrNull()
    return if (selected != null) {
        selected.name to true
    } else {
        null to false
    }
}

private val BANGLA_WEEKDAYS = mapOf(
    Calendar.SUNDAY to "রবিবার",
    Calendar.MONDAY to "সোমবার",
    Calendar.TUESDAY to "মঙ্গলবার",
    Calendar.WEDNESDAY to "বুধবার",
    Calendar.THURSDAY to "বৃহস্পতিবার",
    Calendar.FRIDAY to "শুক্রবার",
    Calendar.SATURDAY to "শনিবার"
)

private val BANGLA_MONTHS = arrayOf(
    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
)

internal fun formatDrawerDate(calendar: Calendar, language: AppLanguage): String {
    return if (language == AppLanguage.BANGLA) {
        val dayName = BANGLA_WEEKDAYS[calendar.get(Calendar.DAY_OF_WEEK)] ?: ""
        val day = Formatters.toBanglaDigits(calendar.get(Calendar.DAY_OF_MONTH).toString())
        val monthIdx = calendar.get(Calendar.MONTH).coerceIn(0, 11)
        val monthName = BANGLA_MONTHS[monthIdx]
        val year = Formatters.toBanglaDigits(calendar.get(Calendar.YEAR).toString())
        "$dayName, $day $monthName $year"
    } else {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.ENGLISH).format(calendar.time)
    }
}
