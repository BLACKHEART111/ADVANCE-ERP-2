package com.pocketdev.advance_erp

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import com.pocketdev.advance_erp.core.util.resolveVariantPhotoUris
import com.pocketdev.advance_erp.data.entity.ProductVariant
import org.junit.Assert.*
import org.junit.Test

class VariantPhotoResolverTest {
    private fun variant(id: String, color: String?, size: String, product: String = "insole") =
        ProductVariant(id, product, "$product-$id", color, size, variantPriceMinor=100, variantCostMinor=50)
    private val master=variant("master","MASTER","L")
    private val blackS=variant("black-s","BLACK","S")
    private val blackM=variant("black-m","BLACK","M")
    private val blackL=variant("black-l","BLACK","L")
    private val variants=listOf(master,blackS,blackM,blackL)

    @Test fun explicitBlackPhotoWinsOverMasterAndPeer() {
        val photos=resolveVariantPhotoUris(variants,mapOf("master" to "master.jpg","black-s" to "black-s.jpg","black-m" to "black-m.jpg"))
        assertEquals("black-s.jpg",photos["black-s"])
        assertEquals("black-m.jpg",photos["black-m"])
    }
    @Test fun sizesWithoutPhotoInheritSameColor() {
        val photos=resolveVariantPhotoUris(variants,mapOf("master" to "master.jpg","black-s" to "black.jpg"))
        assertEquals("black.jpg",photos["black-m"])
        assertEquals("black.jpg",photos["black-l"])
        assertEquals("master.jpg",photos["master"])
    }
    @Test fun missingColorPhotoStaysEmptyInsteadOfShowingBaseOrOtherColor() {
        val photos=resolveVariantPhotoUris(variants,mapOf("master" to "master.jpg"))
        assertNull(photos["black-s"])
        assertNull(photos["black-m"])
    }
    @Test fun colorMatchingIgnoresCaseAndOuterWhitespace() {
        val peer=variant("peer"," black ","XL")
        assertEquals("black.jpg",resolveVariantPhotoUris(listOf(blackS,peer),mapOf("black-s" to "black.jpg"))["peer"])
    }
    @Test fun sameColorFromAnotherProductIsNeverUsed() {
        val other=variant("other","BLACK","S","another-product")
        assertNull(resolveVariantPhotoUris(listOf(blackM,other),mapOf("other" to "other.jpg"))["black-m"])
    }
    @Test fun unspecifiedColorDoesNotBorrowAnotherUnspecifiedPhoto() {
        val a=variant("a",null,"S");val b=variant("b"," ","M")
        assertNull(resolveVariantPhotoUris(listOf(a,b),mapOf("a" to "a.jpg"))["b"])
    }
    @Test fun emptyOwnUriCanUseSameColorPeer() {
        assertEquals("black.jpg",resolveVariantPhotoUris(variants,mapOf("black-m" to "","black-s" to "black.jpg"))["black-m"])
    }
    @Test fun asynchronousPhotoChangesInvalidateSnapshotDerivedSlides() {
        val source=mutableStateMapOf("master" to "master.jpg")
        val slides=derivedStateOf { resolveVariantPhotoUris(variants,source) }
        assertNull(slides.value["black-m"])
        source["black-s"]="black.jpg"
        assertEquals("black.jpg",slides.value["black-s"])
        assertEquals("black.jpg",slides.value["black-m"])
        source["black-m"]="black-own.jpg"
        assertEquals("black-own.jpg",slides.value["black-m"])
        source.remove("black-m")
        assertEquals("black.jpg",slides.value["black-m"])
    }
}
