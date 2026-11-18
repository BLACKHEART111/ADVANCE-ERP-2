package com.pocketdev.advance_erp.core.util

import com.pocketdev.advance_erp.data.entity.ProductVariant
import java.util.Locale

/** Own photo first, then the same product/color in another size. Never borrow a base/other-color photo. */
fun resolveVariantPhotoUris(
    variants: List<ProductVariant>,
    photosByVariant: Map<String, String>
): Map<String, String?> = variants.associate { variant ->
    val own = photosByVariant[variant.id]?.takeIf { it.isNotBlank() }
    val color = variant.color?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }
    val sameColor = if (own == null && color != null) {
        variants.firstNotNullOfOrNull { peer ->
            if (peer.productId == variant.productId && peer.color?.trim()?.lowercase(Locale.ROOT) == color) {
                photosByVariant[peer.id]?.takeIf { it.isNotBlank() }
            } else null
        }
    } else null
    variant.id to (own ?: sameColor)
}
