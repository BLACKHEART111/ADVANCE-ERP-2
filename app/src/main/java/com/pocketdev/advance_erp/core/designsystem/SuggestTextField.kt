package com.pocketdev.advance_erp.core.designsystem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketdev.advance_erp.data.entity.SavedSuggestion
import com.pocketdev.advance_erp.data.repository.SuggestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuggestionViewModel @Inject constructor(
    private val repo: SuggestionRepository
) : ViewModel() {
    init { viewModelScope.launch { try { repo.seedIfEmpty() } catch (_: Exception) {} } }
    suspend fun suggest(type: String, q: String) = try { repo.suggest(type, q) } catch (_: Exception) { emptyList() }
    fun remember(type: String, value: String) { viewModelScope.launch { try { repo.remember(type, value) } catch (_: Exception) {} } }
    fun forget(id: String) { viewModelScope.launch { try { repo.forget(id) } catch (_: Exception) {} } }
}

/**
 * Text field that remembers what was typed once and suggests it again after the first letters.
 * Tap a chip to fill; long-press a chip to delete a wrong suggestion.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestTextField(
    fieldType: String,
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val vm: SuggestionViewModel = hiltViewModel()
    var focused by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<SavedSuggestion>>(emptyList()) }
    var refresh by remember { mutableStateOf(0) }
    val latest by rememberUpdatedState(value)

    LaunchedEffect(value, focused, refresh) {
        suggestions = if (focused) vm.suggest(fieldType, value) else emptyList()
    }
    DisposableEffect(Unit) {
        onDispose { if (touched) vm.remember(fieldType, latest) }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            singleLine = singleLine,
            shape = shape,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidthCompat()
                .onFocusChanged {
                    if (focused && !it.isFocused) vm.remember(fieldType, latest)
                    if (it.isFocused) touched = true
                    focused = it.isFocused
                }
        )
        val shown = suggestions.filter { !it.value.equals(value.trim(), ignoreCase = true) }
        if (focused && shown.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items(shown, key = { it.id }) { s ->
                    Text(
                        text = s.value,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryPurple,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryPurple.copy(alpha = 0.10f))
                            .border(0.5.dp, PrimaryPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .combinedClickable(
                                onClick = { onValueChange(s.value); vm.remember(fieldType, s.value) },
                                onLongClick = { vm.forget(s.id); refresh++ }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun Modifier.fillMaxWidthCompat(): Modifier = this.then(Modifier.fillMaxWidth())
