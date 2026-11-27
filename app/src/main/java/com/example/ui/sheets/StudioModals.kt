package com.example.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.DeviceDiagnostics
import com.example.model.AspectRatioOption
import com.example.model.LayerTrack
import com.example.model.LayerType
import com.example.model.ShapeKind
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCrimson
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraViolet
import com.example.ui.theme.LocalKytheraGlass
import kotlin.math.roundToInt

private val STUDIO_COLORS = listOf(
  0xFFF3F6FFL,
  0xFF5B6BFFL,
  0xFF00C6FFL,
  0xFF9C6BFFL,
  0xFFFF7A64L,
  0xFF00C48CL,
)

@Composable
fun AddTextDialog(
  onAdd: (text: String, fontSizeSp: Float, colorHex: Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  var text by remember { mutableStateOf("KYTHERA") }
  var fontSize by remember { mutableFloatStateOf(34f) }
  var colorHex by remember { mutableLongStateOf(0xFFF3F6FFL) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Text Layer", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Enter Text") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("add_text_input"),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Text Size", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
          Text("${fontSize.roundToInt()} sp", fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = KytheraPeriwinkle)
        }
        Slider(
          value = fontSize,
          onValueChange = { fontSize = it },
          valueRange = 18f..64f,
          colors = SliderDefaults.colors(thumbColor = KytheraPeriwinkle, activeTrackColor = KytheraPeriwinkle),
        )
        Text("Text Color", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          STUDIO_COLORS.forEach { hex ->
            Box(
              modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(hex))
                .border(
                  width = if (colorHex == hex) 2.5.dp else 1.dp,
                  color = if (colorHex == hex) KytheraPeriwinkle else Color.White,
                  shape = CircleShape,
                )
                .clickable { colorHex = hex },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onAdd(text.trim().ifEmpty { "TEXT" }, fontSize, colorHex) },
        colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
        modifier = Modifier.testTag("confirm_add_text"),
      ) {
        Text("Add Text")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun ShapePickerDialog(
  onPickShape: (ShapeKind, Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  var selectedShape by remember { mutableStateOf(ShapeKind.ROUNDED_RECT) }
  var colorHex by remember { mutableLongStateOf(0xFF00C6FFL) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Choose Shape", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          ShapeKind.entries.forEach { kind ->
            val sel = kind == selectedShape
            val icon = when (kind) {
              ShapeKind.ROUNDED_RECT -> Icons.Filled.RoundedCorner
              ShapeKind.CIRCLE -> Icons.Filled.Circle
              ShapeKind.RING -> Icons.Filled.RadioButtonUnchecked
              ShapeKind.STAR -> Icons.Filled.Star
              ShapeKind.WAVE -> Icons.Filled.Waves
            }
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { selectedShape = kind }
                .testTag("pick_shape_${kind.name.lowercase()}"),
              color = if (sel) KytheraPeriwinkle else glass.glassSurface,
              border = BorderStroke(1.dp, if (sel) KytheraPeriwinkle else glass.subtleBorder),
              shape = RoundedCornerShape(12.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(icon, contentDescription = kind.label, tint = if (sel) Color.White else Color(colorHex), modifier = Modifier.size(20.dp))
                Text(kind.label, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else glass.textPrimary)
              }
            }
          }
        }
        Text("Shape Color", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          STUDIO_COLORS.forEach { hex ->
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(hex))
                .border(
                  width = if (colorHex == hex) 2.5.dp else 1.dp,
                  color = if (colorHex == hex) KytheraPeriwinkle else Color.White,
                  shape = CircleShape,
                )
                .clickable { colorHex = hex },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onPickShape(selectedShape, colorHex) },
        colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
        modifier = Modifier.testTag("confirm_add_shape"),
      ) {
        Text("Add Shape")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun NewProjectDialog(
  onCreate: (String, AspectRatioOption, Int, String) -> Unit,
  onDismiss: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  var name by remember { mutableStateOf("Aurora Reel") }
  var aspect by remember { mutableStateOf(AspectRatioOption.PORTRAIT_9_16) }
  var fps by remember { mutableIntStateOf(60) }
  var res by remember { mutableStateOf("1080p") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Motion Project", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Project Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("new_project_name_input"),
        )
        Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          AspectRatioOption.entries.forEach { opt ->
            val sel = opt == aspect
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { aspect = opt },
              color = if (sel) KytheraPeriwinkle else glass.glassSurface,
              border = BorderStroke(1.dp, glass.subtleBorder),
              shape = RoundedCornerShape(10.dp),
            ) {
              Text(
                text = opt.label,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = if (sel) Color.White else glass.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
        Text("Frame Rate & Resolution", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(24, 30, 60).forEach { f ->
            val sel = f == fps
            Surface(
              modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { fps = f },
              color = if (sel) KytheraElectricBlue else glass.glassSurface,
              border = BorderStroke(1.dp, glass.subtleBorder),
              shape = RoundedCornerShape(10.dp),
            ) {
              Text(
                text = "$f FPS",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = if (sel) Color.White else glass.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
          listOf("1080p", "4K").forEach { r ->
            val sel = r == res
            Surface(
              modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { res = r },
              color = if (sel) KytheraViolet else glass.glassSurface,
              border = BorderStroke(1.dp, glass.subtleBorder),
              shape = RoundedCornerShape(10.dp),
            ) {
              Text(
                text = r,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = if (sel) Color.White else glass.textPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onCreate(name, aspect, fps, res) },
        colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
        modifier = Modifier.testTag("confirm_create_project"),
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
fun LayerSettingsDialog(
  layer: LayerTrack,
  onSave: (String, String, Long, ShapeKind) -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  var name by remember(layer.id) { mutableStateOf(layer.name) }
  var textContent by remember(layer.id) { mutableStateOf(layer.textContent) }
  var colorHex by remember(layer.id) { mutableLongStateOf(layer.colorHex) }
  var shapeKind by remember(layer.id) { mutableStateOf(layer.shapeKind) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Layer Settings", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Layer Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (layer.type == LayerType.TEXT) {
          OutlinedTextField(
            value = textContent,
            onValueChange = { textContent = it },
            label = { Text("Display Text") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        if (layer.type == LayerType.SHAPE) {
          Text("Shape Preset", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ShapeKind.entries.forEach { sk ->
              val sel = sk == shapeKind
              Surface(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { shapeKind = sk },
                color = if (sel) KytheraPeriwinkle else glass.glassSurface,
                border = BorderStroke(1.dp, glass.subtleBorder),
                shape = RoundedCornerShape(10.dp),
              ) {
                Text(
                  text = sk.label,
                  fontSize = 11.sp,
                  color = if (sel) Color.White else glass.textPrimary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }
        }
        Text("Layer Color", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          STUDIO_COLORS.forEach { hex ->
            Box(
              modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(hex))
                .border(
                  width = if (colorHex == hex) 2.5.dp else 1.dp,
                  color = if (colorHex == hex) KytheraPeriwinkle else Color.White,
                  shape = CircleShape,
                )
                .clickable { colorHex = hex },
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedButton(
          onClick = onDelete,
          border = BorderStroke(1.dp, KytheraCrimson),
          modifier = Modifier.fillMaxWidth().testTag("delete_layer_button"),
        ) {
          Text("Delete Layer", color = KytheraCrimson, fontWeight = FontWeight.SemiBold)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(name, textContent, colorHex, shapeKind)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
      ) {
        Text("Apply")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
fun DiagnosticsDialog(
  diagnostics: DeviceDiagnostics?,
  onDismiss: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Native Engine Diagnostics", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = diagnostics?.runtime ?: "Android Runtime",
          fontFamily = JetBrainsMonoFamily,
          fontSize = 11.5.sp,
          color = glass.textPrimary,
        )
        Text(
          text = "ABIs: ${diagnostics?.abis?.joinToString(", ") ?: "arm64-v8a"}",
          fontFamily = JetBrainsMonoFamily,
          fontSize = 11.5.sp,
          color = KytheraPeriwinkle,
        )
        Text(
          text = "Hardware MediaCodec Encoders:",
          style = MaterialTheme.typography.labelMedium,
          color = glass.textSecondary,
        )
        Text(
          text = diagnostics?.hardwareEncoders?.joinToString("\n") ?: "MediaCodec ready",
          fontFamily = JetBrainsMonoFamily,
          fontSize = 10.5.sp,
          color = glass.textSecondary,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = KytheraPeriwinkle),
      ) {
        Text("Done")
      }
    },
  )
}
