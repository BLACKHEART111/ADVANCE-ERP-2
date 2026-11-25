package com.pocketdev.advance_erp.feature.sales

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.core.model.Money
import com.pocketdev.advance_erp.core.util.ContactPickerHelper
import com.pocketdev.advance_erp.core.util.Formatters
import com.pocketdev.advance_erp.data.entity.Customer
import com.pocketdev.advance_erp.domain.repository.NewCustomerDraft
import com.pocketdev.advance_erp.feature.inventory.BarcodeQrScannerDialog
import java.io.File

/** Pure search & scan matcher for customer name (EN/BN), mobile digits (EN/BN), code, or address. */
internal object CustomerLookupMatcher {

    fun normalizePhoneOrCode(raw: String): String =
        Formatters.toEnglishDigits(raw)
            .replace("[\\s\\-_().+]".toRegex(), "")
            .trim()
            .lowercase()

    fun filterCustomers(customers: List<Customer>, rawQuery: String): List<Customer> {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) return customers

        val lower = trimmed.lowercase()
        val asciiDigitsQuery = Formatters.toEnglishDigits(trimmed)
            .replace("[^0-9+]".toRegex(), "")
        val normalizedQuery = normalizePhoneOrCode(trimmed)

        return customers.filter { customer ->
            val phoneAscii = Formatters.toEnglishDigits(customer.phone)
            val phoneNormalized = normalizePhoneOrCode(customer.phone)
            val codeNormalized = normalizePhoneOrCode(customer.customerCode)
            val codeAscii = Formatters.toEnglishDigits(customer.customerCode).lowercase()

            customer.name.lowercase().contains(lower) ||
                customer.address.lowercase().contains(lower) ||
                customer.phone.lowercase().contains(lower) ||
                phoneAscii.contains(trimmed, ignoreCase = true) ||
                (asciiDigitsQuery.isNotBlank() && phoneAscii.contains(asciiDigitsQuery)) ||
                (normalizedQuery.isNotBlank() && (
                    phoneNormalized.contains(normalizedQuery) ||
                        codeNormalized.contains(normalizedQuery)
                    )) ||
                codeAscii.contains(lower)
        }
    }

    fun findScannedCustomer(customers: List<Customer>, rawCode: String): Customer? {
        val trimmed = rawCode.trim()
        if (trimmed.isBlank()) return null

        val normScanned = normalizePhoneOrCode(trimmed)
        val exactMatch = customers.firstOrNull { customer ->
            customer.id.equals(trimmed, ignoreCase = true) ||
                customer.customerCode.equals(trimmed, ignoreCase = true) ||
                (normScanned.isNotBlank() && (
                    normalizePhoneOrCode(customer.customerCode) == normScanned ||
                        normalizePhoneOrCode(customer.phone) == normScanned
                    ))
        }
        if (exactMatch != null) return exactMatch

        val filtered = filterCustomers(customers, trimmed)
        return filtered.singleOrNull()
    }
}

@Composable
fun CustomerLookupDialog(
    customers: List<Customer>,
    selectedCustomerId: String = "",
    pendingNewCustomer: NewCustomerDraft? = null,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onCreateNewCustomer: (NewCustomerDraft) -> Unit,
    onCreateCustomerImmediate: ((NewCustomerDraft, onCreated: (Customer) -> Unit, onError: (String) -> Unit) -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isCreatingNew by rememberSaveable { mutableStateOf(false) }
    var showQrScanner by rememberSaveable { mutableStateOf(false) }

    // New Customer Form State
    var newName by rememberSaveable { mutableStateOf("") }
    var newPhone by rememberSaveable { mutableStateOf("") }
    var newAddress by rememberSaveable { mutableStateOf("") }
    var newEmail by rememberSaveable { mutableStateOf("") }
    var newOpeningBalanceStr by rememberSaveable { mutableStateOf("0.00") }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSavingNewCustomer by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val (pickedName, pickedPhone) = ContactPickerHelper.extractContactInfo(context, uri)
                if (!pickedName.isNullOrBlank()) {
                    newName = pickedName
                    formError = null
                }
                if (!pickedPhone.isNullOrBlank()) {
                    newPhone = pickedPhone
                    formError = null
                }
            }
        }
    }

    val filteredCustomers = remember(searchQuery, customers) {
        CustomerLookupMatcher.filterCustomers(customers, searchQuery)
    }

    // Palette tokens matching Light & Dark designs
    val dialogBg = if (isDark) Color(0xFF1D1727) else Color(0xFFFFFFFF)
    val dialogBorder = if (isDark) Color(0xFF3E3154) else Color(0xFFE7DEF6)
    val headerGrad = if (isDark) {
        listOf(Color(0xFF2D2043), Color(0xFF231934), Color(0xFF31224A))
    } else {
        listOf(Color(0xFFEDE2FC), Color(0xFFF6F1FE), Color(0xFFEFE5FD))
    }
    val waveColor1 = if (isDark) Color(0xFF7B3FA0).copy(alpha = 0.22f) else Color(0xFFD6BDF8).copy(alpha = 0.42f)
    val waveColor2 = if (isDark) Color(0xFF4C2B73).copy(alpha = 0.28f) else Color(0xFFE3D1FB).copy(alpha = 0.56f)
    val headerDivider = if (isDark) Color(0xFF382B4E) else Color(0xFFE6DCF6)
    val iconBadgeGrad = if (isDark) {
        listOf(Color(0xFF422E63), Color(0xFF342352))
    } else {
        listOf(Color(0xFFE7D6FE), Color(0xFFD6B9FA))
    }
    val iconBadgeBorder = if (isDark) Color(0xFF5D4585) else Color.White.copy(alpha = 0.85f)
    val primaryPurple = if (isDark) Color(0xFFD596F2) else Color(0xFF6A1B9A)
    val closeBtnBg = if (isDark) Color(0xFF342748) else Color.White
    val closeBtnBorder = if (isDark) Color(0xFF4D3B6B) else Color(0xFFE4DAF3)
    val closeIconTint = if (isDark) Color(0xFFEBDDFB) else Color(0xFF4A156B)
    val textPrimary = if (isDark) Color(0xFFF5EFFC) else Color(0xFF1A142E)
    val textSecondary = if (isDark) Color(0xFFC5B9D9) else Color(0xFF5D5372)
    val textMuted = if (isDark) Color(0xFF988AB0) else Color(0xFF7A708D)
    val searchBg = if (isDark) Color(0xFF15101D) else Color.White
    val searchBorder = if (isDark) Color(0xFF9868C2) else Color(0xFF7E49A8)
    val qrBtnBg = if (isDark) Color(0xFF2B213D) else Color(0xFFF6F1FC)
    val qrBtnBorder = if (isDark) Color(0xFF4B3B66) else Color(0xFFD9CAEE)
    val selectedCardBg = if (isDark) Color(0xFF2D2142) else Color(0xFFF3ECFD)
    val selectedCardBorder = if (isDark) Color(0xFF6E5299) else Color(0xFFC8B3E8)
    val defaultCardBg = if (isDark) Color(0xFF231C30) else Color(0xFFFAF8FE)
    val defaultCardBorder = if (isDark) Color(0xFF3A2E4D) else Color(0xFFE6DDF5)
    val avatarBg = if (isDark) Color(0xFF3E2C5E) else Color(0xFFE3D0FA)
    val selectedCheckBg = if (isDark) Color(0xFF9C27B0) else Color(0xFF6A1B9A)
    val unselectedCircleBorder = if (isDark) Color(0xFF564473) else Color(0xFFD0C0EA)
    val ctaGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF15999B), Color(0xFF4A53A6), Color(0xFF7B1FA2))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            border = BorderStroke(1.dp, dialogBorder),
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Lavender Wave Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(colors = headerGrad))
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        drawPath(
                            path = Path().apply {
                                moveTo(w * 0.42f, h)
                                cubicTo(w * 0.56f, h * 0.45f, w * 0.72f, h * 0.92f, w * 0.88f, h * 0.48f)
                                lineTo(w, h * 0.35f)
                                lineTo(w, h)
                                close()
                            },
                            color = waveColor2
                        )
                        drawPath(
                            path = Path().apply {
                                moveTo(w * 0.58f, h)
                                cubicTo(w * 0.70f, h * 0.52f, w * 0.84f, h * 1.05f, w, h * 0.65f)
                                lineTo(w, h)
                                close()
                            },
                            color = waveColor1
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Brush.linearGradient(colors = iconBadgeGrad))
                                .border(1.dp, iconBadgeBorder, RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = primaryPurple,
                                modifier = Modifier.size(25.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Text(
                                text = if (isCreatingNew) {
                                    if (isBangla) "নতুন গ্রাহক নিবন্ধন" else "Create New Customer"
                                } else {
                                    if (isBangla) "গ্রাহক অনুসন্ধান ও নির্বাচন" else "Customer Lookup & Selection"
                                },
                                fontSize = 14.5.sp,
                                letterSpacing = (-0.25).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (isCreatingNew) {
                                    if (isBangla) "গ্রাহকের তথ্য দিয়ে চালানে যুক্ত করুন" else "Enter customer details and continue"
                                } else {
                                    if (isBangla) "মোবাইল নম্বর বা নাম দিয়ে খুঁজুন" else "Search by mobile number or name"
                                },
                                fontSize = 11.5.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = closeBtnBg,
                            border = BorderStroke(1.dp, closeBtnBorder),
                            shadowElevation = if (isDark) 0.dp else 2.dp,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close",
                                    tint = closeIconTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = headerDivider, thickness = 1.dp)

                if (!isCreatingNew) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Search Input Bar with QR/Barcode Scan Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = searchBg,
                            border = BorderStroke(1.3.dp, searchBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 12.dp, end = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Search",
                                    tint = primaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = if (isBangla) {
                                                "মোবাইল নম্বর বা গ্রাহকের নাম লিখুন..."
                                            } else {
                                                "Enter mobile number or customer name..."
                                            },
                                            fontSize = 12.sp,
                                            letterSpacing = (-0.2).sp,
                                            color = textMuted,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = textPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(primaryPurple),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotBlank()) {
                                    Surface(
                                        onClick = { searchQuery = "" },
                                        shape = CircleShape,
                                        color = qrBtnBg,
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Clear Search",
                                                tint = textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    onClick = { showQrScanner = true },
                                    shape = RoundedCornerShape(9.dp),
                                    color = qrBtnBg,
                                    border = BorderStroke(1.dp, qrBtnBorder),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.QrCodeScanner,
                                            contentDescription = "Scan Customer QR or Barcode",
                                            tint = primaryPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Teal-to-Purple Gradient CTA Button
                        Surface(
                            onClick = {
                                val normalizedDigits = Formatters.toEnglishDigits(searchQuery.trim())
                                if (normalizedDigits.matches("^[0-9+\\s-]{4,}$".toRegex())) {
                                    newPhone = normalizedDigits.replace("[^0-9+]".toRegex(), "")
                                } else if (searchQuery.isNotBlank()) {
                                    newName = searchQuery.trim()
                                }
                                formError = null
                                isCreatingNew = true
                            },
                            shape = RoundedCornerShape(13.dp),
                            color = Color.Transparent,
                            shadowElevation = if (isDark) 2.dp else 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ctaGradient)
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) {
                                        "নতুন গ্রাহক — তৈরি ও চালিয়ে যান"
                                    } else {
                                        "New Customer — Create & Continue"
                                    },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    letterSpacing = (-0.15).sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Customers (N) Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = primaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBangla) {
                                    "গ্রাহক তালিকা (${filteredCustomers.size})"
                                } else {
                                    "Customers (${filteredCustomers.size})"
                                },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }

                        // Customer List or Empty State
                        if (filteredCustomers.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(15.dp),
                                color = defaultCardBg,
                                border = BorderStroke(1.dp, defaultCardBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) {
                                            "কোনো গ্রাহক খুঁজে পাওয়া যায়নি"
                                        } else {
                                            "No matching customer found"
                                        },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textMuted
                                    )
                                    if (searchQuery.isNotBlank()) {
                                        Button(
                                            onClick = {
                                                val normalizedDigits = Formatters.toEnglishDigits(searchQuery.trim())
                                                if (normalizedDigits.matches("^[0-9+\\s-]{4,}$".toRegex())) {
                                                    newPhone = normalizedDigits.replace("[^0-9+]".toRegex(), "")
                                                } else {
                                                    newName = searchQuery.trim()
                                                }
                                                formError = null
                                                isCreatingNew = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryPurple),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = if (isBangla) {
                                                    "'${searchQuery.trim()}' দিয়ে নতুন গ্রাহক তৈরি করুন"
                                                } else {
                                                    "Create '${searchQuery.trim()}' as New Customer"
                                                },
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 264.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredCustomers, key = { it.id }) { customer ->
                                    val isSelected = (selectedCustomerId.isNotBlank() && customer.id == selectedCustomerId) ||
                                        (selectedCustomerId.isBlank() && pendingNewCustomer == null &&
                                            filteredCustomers.size == 1 && customers.firstOrNull()?.id == customer.id)
                                    val photoFileValid = remember(customer.profileImagePath) {
                                        customer.profileImagePath?.takeIf { it.isNotBlank() && File(it).exists() }
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            onSelectCustomer(customer)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(15.dp),
                                        color = if (isSelected) selectedCardBg else defaultCardBg,
                                        border = BorderStroke(
                                            width = if (isSelected) 1.2.dp else 1.dp,
                                            color = if (isSelected) selectedCardBorder else defaultCardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(avatarBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (photoFileValid != null) {
                                                    AsyncImage(
                                                        model = rememberImageModel(photoFileValid),
                                                        contentDescription = customer.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Outlined.PersonOutline,
                                                        contentDescription = null,
                                                        tint = primaryPurple,
                                                        modifier = Modifier.size(23.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(11.dp))

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = customer.name,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${customer.phone} • ${customer.customerCode}",
                                                    fontSize = 12.sp,
                                                    color = textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(selectedCheckBg)
                                                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .border(1.5.dp, unselectedCircleBorder, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Keyboard-Friendly Scrollable New Customer Form
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryPurple,
                        unfocusedBorderColor = defaultCardBorder,
                        focusedLabelColor = primaryPurple,
                        unfocusedLabelColor = textSecondary,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedContainerColor = searchBg,
                        unfocusedContainerColor = searchBg
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBangla) "গ্রাহকের তথ্য পূরণ করুন" else "Fill Customer Information",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                            Surface(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                    contactPickerLauncher.launch(intent)
                                },
                                shape = RoundedCornerShape(9.dp),
                                color = Color(0xFF0E7C7B)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContactPhone,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isBangla) "কন্টাক্ট থেকে আনুন" else "Pick from Contacts",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newName,
                            onValueChange = {
                                newName = it
                                formError = null
                            },
                            label = { Text(if (isBangla) "গ্রাহকের নাম *" else "Customer Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(11.dp),
                            colors = textFieldColors
                        )

                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = {
                                newPhone = it
                                formError = null
                            },
                            label = { Text(if (isBangla) "মোবাইল নম্বর *" else "Mobile Number *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(11.dp),
                            colors = textFieldColors
                        )

                        OutlinedTextField(
                            value = newAddress,
                            onValueChange = {
                                newAddress = it
                                formError = null
                            },
                            label = { Text(if (isBangla) "ঠিকানা (ঐচ্ছিক)" else "Address (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(11.dp),
                            colors = textFieldColors
                        )

                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text(if (isBangla) "ইমেইল (ঐচ্ছিক)" else "Email (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(11.dp),
                            colors = textFieldColors
                        )

                        OutlinedTextField(
                            value = newOpeningBalanceStr,
                            onValueChange = { newOpeningBalanceStr = it },
                            label = { Text(if (isBangla) "পূর্বের বকেয়া (৳, ঐচ্ছিক)" else "Opening Due (৳, Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(11.dp),
                            colors = textFieldColors
                        )

                        if (formError != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(9.dp),
                                color = if (isDark) Color(0xFF3A1B22) else Color(0xFFFDF0F2),
                                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = formError!!,
                                    color = if (isDark) Color(0xFFFF8A8A) else Color(0xFFC62828),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isCreatingNew = false
                                    formError = null
                                },
                                enabled = !isSavingNewCustomer,
                                shape = RoundedCornerShape(11.dp),
                                border = BorderStroke(1.dp, defaultCardBorder),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(
                                    text = if (isBangla) "ফিরে যান" else "Back",
                                    color = textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Surface(
                                onClick = {
                                    if (isSavingNewCustomer) return@Surface
                                    val trimmedName = newName.trim()
                                    val trimmedPhone = Formatters.toEnglishDigits(newPhone.trim())
                                        .replace("[^0-9+]".toRegex(), "")
                                    val trimmedAddress = newAddress.trim()

                                    if (trimmedName.isBlank()) {
                                        formError = if (isBangla) "গ্রাহকের নাম আবশ্যক" else "Customer name is required"
                                        return@Surface
                                    }
                                    if (trimmedPhone.isBlank()) {
                                        formError = if (isBangla) "মোবাইল নম্বর আবশ্যক" else "Mobile number is required"
                                        return@Surface
                                    }

                                    val duplicate = customers.find {
                                        Formatters.toEnglishDigits(it.phone)
                                            .replace("[^0-9+]".toRegex(), "") == trimmedPhone
                                    }
                                    if (duplicate != null) {
                                        formError = if (isBangla) {
                                            "এই মোবাইল নম্বরে ইতিমধ্যে একজন গ্রাহক নিবন্ধিত আছে: ${duplicate.name}"
                                        } else {
                                            "Customer already exists with this phone: ${duplicate.name}"
                                        }
                                        return@Surface
                                    }

                                    val normalizedOpeningStr = Formatters.normalizeAsciiNumberInput(newOpeningBalanceStr)
                                    val openBal = runCatching {
                                        if (normalizedOpeningStr.isBlank()) 0L
                                        else Money.parseFromString(normalizedOpeningStr).minorUnits
                                    }.getOrDefault(0L)

                                    val draft = NewCustomerDraft(
                                        name = trimmedName,
                                        phone = trimmedPhone,
                                        address = trimmedAddress,
                                        email = newEmail.trim().takeIf { it.isNotBlank() },
                                        openingBalanceMinor = openBal.coerceAtLeast(0L)
                                    )

                                    if (onCreateCustomerImmediate != null) {
                                        isSavingNewCustomer = true
                                        onCreateCustomerImmediate(
                                            draft,
                                            { createdCustomer ->
                                                isSavingNewCustomer = false
                                                onSelectCustomer(createdCustomer)
                                                onDismiss()
                                            },
                                            { errMsg ->
                                                isSavingNewCustomer = false
                                                formError = errMsg
                                            }
                                        )
                                    } else {
                                        onCreateNewCustomer(draft)
                                        onDismiss()
                                    }
                                },
                                shape = RoundedCornerShape(11.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ctaGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSavingNewCustomer) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = if (isBangla) "তৈরি ও চালিয়ে যান" else "Create & Continue",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        BarcodeQrScannerDialog(
            language = if (isBangla) AppLanguage.BANGLA else AppLanguage.ENGLISH,
            onDismiss = { showQrScanner = false },
            onCodeScanned = { scannedCode ->
                showQrScanner = false
                val matched = CustomerLookupMatcher.findScannedCustomer(customers, scannedCode)
                if (matched != null) {
                    onSelectCustomer(matched)
                    onDismiss()
                } else {
                    searchQuery = scannedCode.trim()
                }
            }
        )
    }
}
