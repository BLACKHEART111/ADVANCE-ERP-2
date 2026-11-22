package com.pocketdev.advance_erp.feature.product

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pocketdev.advance_erp.core.designsystem.FullScreenImageDialog
import com.pocketdev.advance_erp.core.designsystem.LocalIsDarkTheme
import com.pocketdev.advance_erp.core.designsystem.rememberImageModel
import com.pocketdev.advance_erp.core.model.AppLanguage
import com.pocketdev.advance_erp.data.entity.ProductCategory
import com.pocketdev.advance_erp.data.entity.Warehouse
import java.io.File

@Immutable
internal data class AddProductPalette(
    val isDark: Boolean,
    val pageBg: Color,
    val cardSurface: Color,
    val inputSurface: Color,
    val primaryPurple: Color,
    val primaryButtonFill: Color,
    val primaryButtonDark: Color,
    val premiumTeal: Color,
    val tealButtonFill: Color,
    val lavenderTint: Color,
    val lavenderBorder: Color,
    val cyanTint: Color,
    val cyanBorder: Color,
    val amberCardBg: Color,
    val amberCardBorder: Color,
    val amberAccent: Color,
    val dashedBorder: Color,
    val dashedBg: Color,
    val fieldBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val footerBg: Color,
    val danger: Color,
    val dangerContainer: Color,
    val requiredStar: Color,
    val headerGradientStart: Color,
    val headerGradientMid: Color,
    val headerGradientEnd: Color,
    val waveAccentStart: Color,
    val waveAccentEnd: Color
)

@Composable
internal fun rememberAddProductPalette(): AddProductPalette {
    val dark = LocalIsDarkTheme.current
    return remember(dark) {
        if (dark) {
            AddProductPalette(
                isDark = true,
                pageBg = Color(0xFF0D1425),
                cardSurface = Color(0xFF17243A),
                inputSurface = Color(0xFF1B2A44),
                primaryPurple = Color(0xFFD596F5),
                primaryButtonFill = Color(0xFF832BA0),
                primaryButtonDark = Color(0xFF641B7E),
                premiumTeal = Color(0xFF4ADBD8),
                tealButtonFill = Color(0xFF0A7D88),
                lavenderTint = Color(0xFF211C3B),
                lavenderBorder = Color(0xFF423269),
                cyanTint = Color(0xFF122C39),
                cyanBorder = Color(0xFF1E5466),
                amberCardBg = Color(0xFF262018),
                amberCardBorder = Color(0xFF524124),
                amberAccent = Color(0xFFF5B041),
                dashedBorder = Color(0xFF54437A),
                dashedBg = Color(0xFF131D32),
                fieldBorder = Color(0xFF2E3F5E),
                textPrimary = Color(0xFFF5F2FA),
                textSecondary = Color(0xFFAAB4CA),
                textMuted = Color(0xFF7D8AA4),
                footerBg = Color(0xFF121C30),
                danger = Color(0xFFFCA5A5),
                dangerContainer = Color(0xFF3E1E24),
                requiredStar = Color(0xFFF87171),
                headerGradientStart = Color(0xFF4A1864),
                headerGradientMid = Color(0xFF332761),
                headerGradientEnd = Color(0xFF0B5A68),
                waveAccentStart = Color(0xFF9B4DCA),
                waveAccentEnd = Color(0xFF24B5B9)
            )
        } else {
            AddProductPalette(
                isDark = false,
                pageBg = Color(0xFFF8F6FC),
                cardSurface = Color(0xFFFFFFFF),
                inputSurface = Color(0xFFFFFFFF),
                primaryPurple = Color(0xFF7D288C),
                primaryButtonFill = Color(0xFF7D288C),
                primaryButtonDark = Color(0xFF641D74),
                premiumTeal = Color(0xFF087D88),
                tealButtonFill = Color(0xFF087D88),
                lavenderTint = Color(0xFFF3ECFA),
                lavenderBorder = Color(0xFFE3D5F2),
                cyanTint = Color(0xFFE7F6F8),
                cyanBorder = Color(0xFFC4EAEF),
                amberCardBg = Color(0xFFFFF5E5),
                amberCardBorder = Color(0xFFF5DFBF),
                amberAccent = Color(0xFFD97706),
                dashedBorder = Color(0xFFCBB8DE),
                dashedBg = Color(0xFFFCFAFF),
                fieldBorder = Color(0xFFE6DFEE),
                textPrimary = Color(0xFF211B2B),
                textSecondary = Color(0xFF746D7D),
                textMuted = Color(0xFF9890A3),
                footerBg = Color(0xFFFFFFFF),
                danger = Color(0xFFB3261E),
                dangerContainer = Color(0xFFFCE4E1),
                requiredStar = Color(0xFFDC2626),
                headerGradientStart = Color(0xFF832792),
                headerGradientMid = Color(0xFF5F308E),
                headerGradientEnd = Color(0xFF0C838E),
                waveAccentStart = Color(0xFFC98AE6),
                waveAccentEnd = Color(0xFF5AD5D8)
            )
        }
    }
}

@Composable
internal fun addProductFieldColors(palette: AddProductPalette): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.inputSurface,
        unfocusedContainerColor = palette.inputSurface,
        disabledContainerColor = palette.inputSurface.copy(alpha = 0.65f),
        focusedBorderColor = palette.primaryPurple,
        unfocusedBorderColor = palette.fieldBorder,
        disabledBorderColor = palette.fieldBorder.copy(alpha = 0.6f),
        focusedTextColor = palette.textPrimary,
        unfocusedTextColor = palette.textPrimary,
        disabledTextColor = palette.textSecondary,
        focusedPlaceholderColor = palette.textMuted,
        unfocusedPlaceholderColor = palette.textMuted,
        cursorColor = palette.primaryPurple
    )
}

/**
 * Purple-to-teal wave gradient hero header modeled closely after the reference screens.
 */
@Composable
internal fun AddProductWaveHeader(
    title: String,
    subtitle: String,
    palette: AddProductPalette,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        palette.headerGradientStart,
                        palette.headerGradientMid,
                        palette.headerGradientEnd
                    )
                )
            )
    ) {
        // Subtle decorative circles + bottom flowing dual wave into page background
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                color = Color.White.copy(alpha = if (palette.isDark) 0.05f else 0.08f),
                radius = h * 0.65f,
                center = androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.25f)
            )
            drawCircle(
                color = Color.White.copy(alpha = if (palette.isDark) 0.03f else 0.05f),
                radius = h * 0.48f,
                center = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.12f)
            )

            // Back translucent lavender-to-aqua wave ribbon
            val ribbonPath = Path().apply {
                moveTo(0f, h * 0.62f)
                cubicTo(w * 0.28f, h * 0.56f, w * 0.54f, h * 0.98f, w, h * 0.60f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = ribbonPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        palette.waveAccentStart.copy(alpha = if (palette.isDark) 0.34f else 0.45f),
                        palette.waveAccentEnd.copy(alpha = if (palette.isDark) 0.42f else 0.58f)
                    )
                )
            )

            // Front crisp page background cutout wave
            val frontWave = Path().apply {
                moveTo(0f, h * 0.75f)
                cubicTo(w * 0.30f, h * 0.68f, w * 0.60f, h * 1.05f, w, h * 0.76f)
                lineTo(w, h + 2f)
                lineTo(0f, h + 2f)
                close()
            }
            drawPath(path = frontWave, color = palette.pageBg)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = if (palette.isDark) 0.15f else 0.20f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_product_close")
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (palette.isDark) 0.16f else 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Three-step horizontal card selector:
 * 1. মূল তথ্য (Basic Info) -> 2. অন্যান্য তথ্য (Other Info) -> 3. ভ্যারিয়েন্ট (Variants)
 */
@Composable
internal fun AddProductStepBar(
    currentStep: Int,
    variantsEnabled: Boolean,
    language: AppLanguage,
    palette: AddProductPalette,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    val steps = remember(isBangla) {
        if (isBangla) {
            listOf(
                Triple(0, "১. মূল তথ্য", Icons.Outlined.Inventory2),
                Triple(1, "২. অন্যান্য", Icons.Outlined.Settings),
                Triple(2, "৩. ভ্যারিয়েন্ট", Icons.Outlined.Layers)
            )
        } else {
            listOf(
                Triple(0, "1. Basic Info", Icons.Outlined.Inventory2),
                Triple(1, "2. Other Info", Icons.Outlined.Settings),
                Triple(2, "3. Variants", Icons.Outlined.Layers)
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        steps.forEach { (index, label, icon) ->
            val isActive = currentStep == index
            val isCompleted = index < currentStep
            val isOptionalInactive = index == 2 && !variantsEnabled && !isActive

            val shape = RoundedCornerShape(16.dp)
            val borderColor by animateColorAsState(
                targetValue = when {
                    isActive -> Color.Transparent
                    isCompleted -> palette.primaryPurple.copy(alpha = 0.40f)
                    else -> palette.fieldBorder
                },
                label = "stepBorder"
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 66.dp)
                    .alpha(if (isOptionalInactive) 0.72f else 1f)
                    .clip(shape)
                    .clickable { onStepSelected(index) }
                    .testTag("add_product_step_$index"),
                shape = shape,
                color = if (isActive) Color.Transparent else palette.cardSurface,
                border = if (isActive) null else BorderStroke(1.dp, borderColor),
                shadowElevation = if (isActive) 6.dp else 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isActive) {
                                Modifier.background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            palette.primaryButtonFill,
                                            palette.primaryButtonDark
                                        )
                                    )
                                )
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Outlined.CheckCircle else icon,
                            contentDescription = null,
                            tint = when {
                                isActive -> Color.White
                                isCompleted -> palette.primaryPurple
                                else -> palette.textSecondary
                            },
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.SemiBold,
                            color = when {
                                isActive -> Color.White
                                isCompleted -> palette.textPrimary
                                else -> palette.textSecondary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddProductFieldLabel(
    text: String,
    palette: AddProductPalette,
    required: Boolean = false,
    leadingIcon: ImageVector? = null,
    iconTint: Color = palette.primaryPurple,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        if (required) {
            Text(
                text = " *",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.requiredStar
            )
        }
    }
}

@Composable
internal fun AddProductSelectorCard(
    text: String,
    placeholder: String,
    leadingIcon: ImageVector,
    palette: AddProductPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = true,
    testTag: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = palette.inputSurface,
        border = BorderStroke(1.dp, palette.fieldBorder),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = palette.primaryPurple,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text.ifBlank { placeholder },
                color = if (isSelected && text.isNotBlank()) palette.textPrimary else palette.textMuted,
                fontSize = 14.5.sp,
                fontWeight = if (isSelected && text.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun AddProductMagicButton(
    tint: Color,
    containerColor: Color,
    borderColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(54.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.AutoFixHigh,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Dashed photo uploader with Camera, Gallery, and File selection + reordering/primary thumbnails.
 */
@Composable
internal fun AddProductPhotoUploader(
    title: String,
    photos: List<GalleryItemDraft>,
    onPhotosChanged: (List<GalleryItemDraft>) -> Unit,
    maxPhotos: Int,
    language: AppLanguage,
    palette: AddProductPalette,
    showSourceButtons: Boolean = true,
    dropzoneTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    val context = LocalContext.current
    var previewUri by remember { mutableStateOf<String?>(null) }
    var photoToDelete by remember { mutableStateOf<GalleryItemDraft?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    fun appendUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val updated = photos.toMutableList()
        for (uri in uris) {
            if (updated.size < maxPhotos) {
                val isFirst = updated.isEmpty()
                updated.add(
                    GalleryItemDraft(
                        id = "draft-${System.nanoTime()}-${updated.size}",
                        uriString = uri.toString(),
                        isPrimary = isFirst,
                        sortOrder = updated.size
                    )
                )
            }
        }
        onPhotosChanged(updated)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxPhotos.coerceAtLeast(2))
    ) { uris ->
        appendUris(uris)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        appendUris(uris)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val capturedFile = pendingCameraFile
        pendingCameraFile = null
        if (success && capturedFile != null && capturedFile.exists()) {
            appendUris(listOf(Uri.fromFile(capturedFile)))
        } else {
            capturedFile?.delete()
        }
    }

    fun launchCamera() {
        try {
            val (tmpFile, tmpUri) = createProductCameraTempUri(context)
            pendingCameraFile = tmpFile
            takePictureLauncher.launch(tmpUri)
        } catch (t: Throwable) {
            Toast.makeText(
                context,
                if (isBangla) "ক্যামেরা চালু করা যায়নি: ${t.message}" else "Unable to open camera: ${t.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(
                context,
                if (isBangla) "ছবি তোলার জন্য ক্যামেরা অনুমতি প্রয়োজন" else "Camera permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val openGallery: () -> Unit = {
        if (photos.size >= maxPhotos) {
            Toast.makeText(
                context,
                if (isBangla) "সর্বোচ্চ ${maxPhotos}টি ছবি যোগ করা যাবে" else "Maximum $maxPhotos photos allowed",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    val openCamera: () -> Unit = {
        if (photos.size >= maxPhotos) {
            Toast.makeText(
                context,
                if (isBangla) "সর্বোচ্চ ${maxPhotos}টি ছবি যোগ করা যাবে" else "Maximum $maxPhotos photos allowed",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val openFiles: () -> Unit = {
        if (photos.size >= maxPhotos) {
            Toast.makeText(
                context,
                if (isBangla) "সর্বোচ্চ ${maxPhotos}টি ছবি যোগ করা যাবে" else "Maximum $maxPhotos photos allowed",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            filePickerLauncher.launch(arrayOf("image/*"))
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$title (${photos.size}/$maxPhotos)",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            if (photos.size < maxPhotos) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = openGallery)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddAPhoto,
                        contentDescription = null,
                        tint = palette.primaryPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isBangla) "+ ছবি যোগ করুন" else "+ Add Photos",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dashed Upload Dropzone
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.dashedBg)
                .clickable(onClick = openGallery)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val radius = 16.dp.toPx()
                drawRoundRect(
                    color = palette.dashedBorder,
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 11f), 0f)
                    )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (photos.isEmpty()) 22.dp else 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = palette.primaryPurple,
                    modifier = Modifier.size(if (photos.isEmpty()) 38.dp else 28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dropzoneTitle ?: if (isBangla) "গ্যালারি থেকে ছবি নির্বাচন করুন" else "Select photos from gallery",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBangla) {
                        "JPG, PNG • সর্বোচ্চ ${maxPhotos}টি • প্রতি ছবি ≤ 5 MB"
                    } else {
                        "JPG, PNG • Max $maxPhotos photos • ≤ 5 MB each"
                    },
                    fontSize = 11.5.sp,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showSourceButtons) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PhotoSourceActionTile(
                    icon = Icons.Outlined.CameraAlt,
                    label = if (isBangla) "ক্যামেরা" else "Camera",
                    containerColor = palette.cardSurface,
                    borderColor = palette.fieldBorder,
                    iconTint = palette.primaryPurple,
                    textColor = palette.textPrimary,
                    onClick = openCamera,
                    modifier = Modifier.weight(1f)
                )
                PhotoSourceActionTile(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = if (isBangla) "গ্যালারি" else "Gallery",
                    containerColor = palette.cyanTint,
                    borderColor = palette.cyanBorder,
                    iconTint = palette.premiumTeal,
                    textColor = palette.textPrimary,
                    onClick = openGallery,
                    modifier = Modifier.weight(1f)
                )
                PhotoSourceActionTile(
                    icon = Icons.Outlined.Description,
                    label = if (isBangla) "ফাইল" else "File",
                    containerColor = palette.cardSurface,
                    borderColor = palette.fieldBorder,
                    iconTint = palette.primaryPurple,
                    textColor = palette.textPrimary,
                    onClick = openFiles,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (photos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(photos, key = { _, item -> item.id }) { index, item ->
                    Surface(
                        modifier = Modifier.width(114.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = palette.cardSurface,
                        border = BorderStroke(
                            width = if (item.isPrimary) 2.dp else 1.dp,
                            color = if (item.isPrimary) palette.premiumTeal else palette.fieldBorder
                        ),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { previewUri = item.uriString }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(rememberImageModel(item.uriString))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Photo ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (item.isPrimary) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .background(palette.tealButtonFill, RoundedCornerShape(5.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) "প্রধান" else "PRIMARY",
                                            fontSize = 9.5.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val reordered = photos.toMutableList()
                                            val tmp = reordered[index]
                                            reordered[index] = reordered[index - 1]
                                            reordered[index - 1] = tmp
                                            onPhotosChanged(reordered.mapIndexed { idx, d -> d.copy(sortOrder = idx) })
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Move Left",
                                        tint = if (index > 0) palette.textSecondary else palette.textMuted.copy(alpha = 0.3f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val updated = photos.mapIndexed { idx, d -> d.copy(isPrimary = idx == index) }
                                        onPhotosChanged(updated)
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isPrimary) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Set Primary",
                                        tint = if (item.isPrimary) palette.premiumTeal else palette.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (index < photos.size - 1) {
                                            val reordered = photos.toMutableList()
                                            val tmp = reordered[index]
                                            reordered[index] = reordered[index + 1]
                                            reordered[index + 1] = tmp
                                            onPhotosChanged(reordered.mapIndexed { idx, d -> d.copy(sortOrder = idx) })
                                        }
                                    },
                                    enabled = index < photos.size - 1,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = "Move Right",
                                        tint = if (index < photos.size - 1) palette.textSecondary else palette.textMuted.copy(alpha = 0.3f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { photoToDelete = item },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete Photo",
                                        tint = palette.danger,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (photoToDelete != null) {
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text(if (isBangla) "ছবি মুছে ফেলবেন?" else "Delete Photo?") },
            text = {
                Text(
                    if (isBangla) "আপনি কি গ্যালারি থেকে এই ছবিটি সরাতে চান?"
                    else "Are you sure you want to remove this photo from the gallery?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toRemove = photoToDelete
                        photoToDelete = null
                        if (toRemove != null) {
                            onPhotosChanged(removePhotoAndReassignPrimary(photos, toRemove.id))
                        }
                    }
                ) {
                    Text(if (isBangla) "মুছুন" else "Delete", color = palette.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel", color = palette.textSecondary)
                }
            }
        )
    }

    if (previewUri != null) {
        FullScreenImageDialog(
            imageUriOrPath = previewUri!!,
            onDismiss = { previewUri = null }
        )
    }
}

@Composable
private fun PhotoSourceActionTile(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    borderColor: Color,
    iconTint: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun AddProductVariantToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    language: AppLanguage,
    palette: AddProductPalette,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    Surface(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier
            .fillMaxWidth()
            .testTag("add_product_variant_toggle_card"),
        shape = RoundedCornerShape(16.dp),
        color = palette.amberCardBg,
        border = BorderStroke(1.dp, palette.amberCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.amberAccent.copy(alpha = 0.16f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = null,
                        tint = palette.amberAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBangla) "ভ্যারিয়েন্ট ব্যবহার করবেন?" else "Use Product Variants?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isBangla) {
                        "একই পণ্যের বিভিন্ন রং, সাইজ, মডেল বা ধরন যোগ করুন।"
                    } else {
                        "Add colors, sizes, models or variations under this product."
                    },
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("add_product_variant_switch"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = palette.primaryButtonFill,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = palette.fieldBorder
                )
            )
        }
    }
}

@Composable
internal fun AddProductStickyFooter(
    currentStep: Int,
    variantsEnabled: Boolean,
    isRestock: Boolean,
    isSubmitting: Boolean,
    language: AppLanguage,
    palette: AddProductPalette,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBangla = language == AppLanguage.BANGLA
    val showSubmitOnThisStep = currentStep == 2 || (currentStep == 1 && !variantsEnabled)
    val submitLabel = when {
        isRestock && isBangla -> "মজুদ যোগ ও সংরক্ষণ"
        isRestock -> "Restock & Save"
        isBangla -> "পণ্য যোগ করুন"
        else -> "Add Product"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.footerBg,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, palette.fieldBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep == 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.lavenderTint,
                    border = BorderStroke(1.dp, palette.lavenderBorder)
                ) {
                    Text(
                        text = if (isBangla) "ধাপ ১ / ৩" else "Step 1 / 3",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryPurple,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(0.15f))
                Button(
                    onClick = onNext,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("add_product_next_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonFill,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (isBangla) "পরবর্তী" else "Next",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("add_product_prev_btn"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.fieldBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = palette.cardSurface,
                        contentColor = palette.textPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) "পূর্ববর্তী" else "Previous",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = if (showSubmitOnThisStep) onSubmit else onNext,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .weight(1.25f)
                        .heightIn(min = 48.dp)
                        .testTag(if (showSubmitOnThisStep) "add_product_submit_btn" else "add_product_next_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primaryButtonFill,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "সংরক্ষণ হচ্ছে..." else "Saving...",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (showSubmitOnThisStep) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = submitLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = if (isBangla) "পরবর্তী" else "Next",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a bottom-sheet overlay inside the same full-screen Dialog window so it always stays above form content.
 */
@Composable
private fun AddProductInWindowSheetOverlay(
    onDismiss: () -> Unit,
    palette: AddProductPalette,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onDismiss)
    val scrimInteraction = remember { MutableInteractionSource() }
    val sheetInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = sheetInteraction,
                    indication = null,
                    onClick = {}
                )
                .imePadding(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = palette.cardSurface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.fieldBorder)
                )
                content()
            }
        }
    }
}

@Composable
internal fun AddProductSearchableCategorySheet(
    categories: List<ProductCategory>,
    selectedCategory: ProductCategory?,
    language: AppLanguage,
    palette: AddProductPalette,
    canCreateCategory: Boolean,
    onSelectCategory: (ProductCategory) -> Unit,
    onRequestCreateCategory: () -> Unit,
    onDismiss: () -> Unit
) {
    val isBangla = language == AppLanguage.BANGLA
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(categories, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) categories
        else categories.filter {
            it.nameEn.lowercase().contains(q) ||
                it.nameBn.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
        }
    }

    AddProductInWindowSheetOverlay(
        onDismiss = onDismiss,
        palette = palette
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBangla) "ক্যাটাগরি নির্বাচন করুন" else "Select Category",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                if (canCreateCategory) {
                    TextButton(onClick = {
                        onDismiss()
                        onRequestCreateCategory()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = palette.primaryPurple,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "নতুন তৈরি" else "New Category",
                            color = palette.primaryPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isBangla) "ক্যাটাগরি খুঁজুন..." else "Search categories...",
                        color = palette.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = palette.textSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear",
                                tint = palette.textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = addProductFieldColors(palette),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBangla) "কোনো ক্যাটাগরি পাওয়া যায়নি" else "No matching category found",
                        color = palette.textSecondary,
                        fontSize = 13.5.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { cat ->
                        val selected = selectedCategory?.id == cat.id
                        Surface(
                            onClick = {
                                onSelectCategory(cat)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) palette.lavenderTint else palette.inputSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) palette.primaryPurple else palette.fieldBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBangla) cat.nameBn else cat.nameEn,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "${if (isBangla) cat.nameEn else cat.nameBn} • ${cat.code}",
                                        fontSize = 12.sp,
                                        color = palette.textSecondary
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = palette.primaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (canCreateCategory) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = palette.fieldBorder)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onRequestCreateCategory()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.primaryPurple.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = palette.primaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBangla) "+ নতুন ক্যাটাগরি তৈরি করুন" else "+ Create New Category",
                        color = palette.primaryPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun AddProductWarehouseSheet(
    warehouses: List<Warehouse>,
    selectedWarehouse: Warehouse?,
    language: AppLanguage,
    palette: AddProductPalette,
    onSelectWarehouse: (Warehouse) -> Unit,
    onDismiss: () -> Unit
) {
    val isBangla = language == AppLanguage.BANGLA

    AddProductInWindowSheetOverlay(
        onDismiss = onDismiss,
        palette = palette
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isBangla) "ওয়্যারহাউস নির্বাচন করুন" else "Select Warehouse",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (warehouses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBangla) "কোনো ওয়্যারহাউস পাওয়া যায়নি" else "No warehouses found",
                        color = palette.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(warehouses, key = { it.id }) { wh ->
                        val selected = selectedWarehouse?.id == wh.id
                        Surface(
                            onClick = {
                                onSelectWarehouse(wh)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) palette.lavenderTint else palette.inputSurface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) palette.primaryPurple else palette.fieldBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warehouse,
                                    contentDescription = null,
                                    tint = palette.primaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = wh.name,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    if (wh.location.isNotBlank()) {
                                        Text(
                                            text = wh.location,
                                            fontSize = 12.sp,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = palette.primaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

internal fun createProductCameraTempUri(context: Context): Pair<File, Uri> {
    val tmpDir = File(context.cacheDir, "product_camera_tmp").apply {
        if (!exists()) mkdirs()
    }
    val tmpFile = File(tmpDir, "prd_cam_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tmpFile
    )
    return tmpFile to uri
}

internal fun removePhotoAndReassignPrimary(
    photos: List<GalleryItemDraft>,
    removeId: String
): List<GalleryItemDraft> {
    val target = photos.firstOrNull { it.id == removeId } ?: return photos
    val remaining = photos.filter { it.id != removeId }.toMutableList()
    if (target.isPrimary && remaining.isNotEmpty() && remaining.none { it.isPrimary }) {
        remaining[0] = remaining[0].copy(isPrimary = true)
    }
    return remaining.mapIndexed { idx, draft -> draft.copy(sortOrder = idx) }
}

internal fun normalizeProductDescription(raw: String, maxChars: Int = 500): String {
    return if (raw.length <= maxChars) raw else raw.take(maxChars)
}

internal fun computeEffectiveAddProductStock(
    hideStock: Boolean,
    variantsEnabled: Boolean,
    variants: List<NewVariantDraft>,
    initialStockInput: String
): Int {
    if (hideStock) return 0
    val variantSum = if (variantsEnabled) variants.sumOf { it.initialStock.coerceAtLeast(0) } else 0
    return if (variantSum > 0) variantSum else (initialStockInput.toIntOrNull()?.coerceAtLeast(0) ?: 0)
}

internal fun findVariantDuplicateError(
    candidateSku: String,
    candidateBarcode: String?,
    baseSku: String,
    baseBarcode: String?,
    existingVariants: List<NewVariantDraft>,
    editingIndex: Int?,
    isBangla: Boolean
): String? {
    val cleanSku = candidateSku.trim().uppercase()
    if (cleanSku.isNotBlank()) {
        if (cleanSku.equals(baseSku.trim(), ignoreCase = true)) {
            return if (isBangla) "ভ্যারিয়েন্ট SKU মূল পণ্যের SKU-এর মতো হতে পারবে না" else "Variant SKU cannot match Base SKU"
        }
        val dupSku = existingVariants.withIndex().any { (idx, v) ->
            idx != editingIndex && v.sku.trim().equals(cleanSku, ignoreCase = true)
        }
        if (dupSku) {
            return if (isBangla) "এই ভ্যারিয়েন্ট SKU ইতিমধ্যে তালিকায় আছে" else "Variant SKU already exists in the list"
        }
    }
    val cleanBar = candidateBarcode?.trim().orEmpty()
    if (cleanBar.isNotBlank()) {
        if (!baseBarcode.isNullOrBlank() && cleanBar == baseBarcode.trim()) {
            return if (isBangla) "ভ্যারিয়েন্ট বারকোড মূল পণ্যের বারকোডের সাথে মিলে গেছে" else "Variant barcode matches product barcode"
        }
        val dupBar = existingVariants.withIndex().any { (idx, v) ->
            idx != editingIndex && !v.barcode.isNullOrBlank() && v.barcode.trim() == cleanBar
        }
        if (dupBar) {
            return if (isBangla) "এই ভ্যারিয়েন্ট বারকোড ইতিমধ্যে তালিকায় আছে" else "Variant barcode already exists in the list"
        }
    }
    return null
}

@Composable
internal fun AddProductBannerInfoCard(
    icon: ImageVector = Icons.Outlined.Info,
    title: String,
    subtitle: String,
    palette: AddProductPalette,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.lavenderTint,
        border = BorderStroke(1.dp, palette.lavenderBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.primaryPurple,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryPurple
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = palette.textSecondary,
                    lineHeight = 17.sp
                )
            }
            if (actionLabel != null && onActionClick != null) {
                TextButton(
                    onClick = onActionClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.premiumTeal
                    )
                }
            }
        }
    }
}
