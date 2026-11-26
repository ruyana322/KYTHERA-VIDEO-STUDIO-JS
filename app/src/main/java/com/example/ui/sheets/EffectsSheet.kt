package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EffectCategory
import com.example.model.EffectDef
import com.example.model.EffectsCatalog
import com.example.model.LayerTrack
import com.example.ui.components.GlassCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCrimson
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.LocalKytheraGlass
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EffectsSheet(
  layer: LayerTrack,
  selectedCategory: EffectCategory,
  searchQuery: String,
  onSelectCategory: (EffectCategory) -> Unit,
  onSearchChange: (String) -> Unit,
  onAddEffect: (EffectDef) -> Unit,
  onUpdateEffectValue: (Int, Float) -> Unit,
  onRemoveEffect: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val q = searchQuery.trim().lowercase()
  val filtered = EffectsCatalog.ALL.filter { fx ->
    val catOk = selectedCategory == EffectCategory.ALL || fx.category == selectedCategory
    val qOk = q.isEmpty() || fx.name.lowercase().contains(q) || fx.category.label.lowercase().contains(q)
    catOk && qOk
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = glass.glassElevated,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text("Effects Browser", style = MaterialTheme.typography.titleLarge, color = glass.textPrimary)
          Text(
            text = "Layer: ${layer.name} · ${layer.effects.size} active",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.sp,
            color = glass.textSecondary,
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Close Effects", tint = glass.textPrimary)
        }
      }

      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text("Search blur, glow, distortion...") },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        EffectCategory.entries.forEach { cat ->
          val active = cat == selectedCategory
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .clickable { onSelectCategory(cat) },
            color = if (active) KytheraPeriwinkle else glass.glassSurface,
            border = BorderStroke(1.dp, if (active) KytheraPeriwinkle else glass.subtleBorder),
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = cat.label,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (active) Color.White else glass.textPrimary,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }
      }

      // Active Layer Effect Stack
      if (layer.effects.isNotEmpty()) {
        Text("Active Stack", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
        layer.effects.forEachIndexed { idx, fx ->
          GlassCard(cornerRadius = 16.dp, contentPadding = 12.dp) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(fx.name, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                Text(
                  text = "${fx.paramLabel}: ${fx.value.roundToInt()}",
                  fontFamily = JetBrainsMonoFamily,
                  fontSize = 11.sp,
                  color = KytheraPeriwinkle,
                )
              }
              IconButton(onClick = { onRemoveEffect(idx) }) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove ${fx.name}", tint = KytheraCrimson)
              }
            }
            Slider(
              value = fx.value.coerceIn(fx.minVal, fx.maxVal),
              onValueChange = { onUpdateEffectValue(idx, it) },
              valueRange = fx.minVal..fx.maxVal,
              colors = SliderDefaults.colors(thumbColor = KytheraPeriwinkle, activeTrackColor = KytheraPeriwinkle),
            )
          }
        }
      }

      // Catalog Items
      Text("Add Effect (${filtered.size})", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        filtered.forEach { def ->
          GlassCard(
            cornerRadius = 16.dp,
            contentPadding = 12.dp,
            onClick = { onAddEffect(def) },
            modifier = Modifier.fillMaxWidth().testTag("fx_card_${def.id}"),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = def.category.label.uppercase(),
                  fontFamily = JetBrainsMonoFamily,
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = KytheraElectricBlue,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(def.name, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                Text(def.description, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
              }
              Surface(
                color = KytheraPeriwinkle.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Add,
                  contentDescription = "Add ${def.name}",
                  tint = KytheraPeriwinkle,
                  modifier = Modifier.padding(8.dp).size(18.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
