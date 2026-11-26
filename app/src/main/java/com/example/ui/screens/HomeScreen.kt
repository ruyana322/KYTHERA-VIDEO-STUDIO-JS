package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.StudioProject
import com.example.ui.components.GlassCard
import com.example.ui.components.SquircleBadge
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.KytheraCoral
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraMint
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraViolet
import com.example.ui.theme.LocalKytheraGlass
import com.example.viewmodel.StudioUiState

@Composable
fun HomeScreen(
  uiState: StudioUiState,
  onOpenProject: (String) -> Unit,
  onNewProject: () -> Unit,
  onImportProject: () -> Unit,
  onImportZip: () -> Unit,
  onOpenJsStudio: () -> Unit,
  onDuplicateProject: (String) -> Unit,
  onDeleteProject: (String) -> Unit,
  onToggleTheme: () -> Unit,
  onOpenDiagnostics: () -> Unit,
  bottomContentPadding: PaddingValues,
  modifier: Modifier = Modifier,
) {
  val glass = LocalKytheraGlass.current
  val activeProject = uiState.activeProject
  val totalLayers = uiState.projects.sumOf { it.layers.size }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = 18.dp,
      end = 18.dp,
      top = 12.dp,
      bottom = bottomContentPadding.calculateBottomPadding() + 20.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Header with Greeting, Theme Toggle, Diagnostics & Creator Avatar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Create Without Limits.",
            style = MaterialTheme.typography.bodyMedium,
            color = glass.textSecondary,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "KYTHERA STUDIO",
            style = MaterialTheme.typography.headlineSmall,
            color = glass.textPrimary,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenDiagnostics)
                .testTag("home_diagnostics_button"),
              color = glass.glassElevated,
              border = BorderStroke(1.dp, glass.glassBorder),
              shape = RoundedCornerShape(50),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Memory,
                  contentDescription = "Hardware Diagnostics",
                  tint = KytheraPeriwinkle,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = uiState.diagnostics?.abis?.firstOrNull()?.uppercase() ?: "ARM64",
                  fontFamily = JetBrainsMonoFamily,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = glass.textPrimary,
                )
              }
            }

            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onToggleTheme)
                .testTag("home_theme_toggle"),
              color = glass.glassElevated,
              border = BorderStroke(1.dp, glass.glassBorder),
              shape = RoundedCornerShape(50),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = if (uiState.darkGlassMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                  contentDescription = "Toggle Frosted Theme",
                  tint = KytheraViolet,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = if (uiState.darkGlassMode) "Light Glass" else "Dark Glass",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = glass.textPrimary,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
          modifier = Modifier
            .size(62.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .clickable(onClick = onOpenDiagnostics),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_creator_avatar),
            contentDescription = "Creator Studio Profile",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .scale(1.12f),
          )
        }
      }
    }

    // 2. Three Frosted Stat Tiles (matching reference 1 layout)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        HomeStatGlassTile(
          value = "${uiState.projects.size}",
          label = "Projects",
          icon = Icons.Filled.Movie,
          startColor = KytheraViolet,
          endColor = KytheraPeriwinkle,
          modifier = Modifier.weight(1f),
        )
        HomeStatGlassTile(
          value = "${activeProject?.fps ?: 60} FPS",
          label = "Timeline Rate",
          icon = Icons.Filled.Speed,
          startColor = KytheraElectricBlue,
          endColor = KytheraCyan,
          modifier = Modifier.weight(1f),
        )
        HomeStatGlassTile(
          value = "$totalLayers",
          label = "Total Layers",
          icon = Icons.Filled.Layers,
          startColor = KytheraCoral,
          endColor = Color(0xFFFFA07A),
          modifier = Modifier.weight(1f),
        )
      }
    }

    // 3. Featured Hero Motion Card (matching reference 1 & 4 mountain banner)
    if (activeProject != null) {
      item {
        val layerProgress = (activeProject.layers.size.coerceIn(1, 6) / 6f)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.2.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
            .clickable { onOpenProject(activeProject.id) }
            .testTag("home_featured_project_card"),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_studio),
            contentDescription = "Featured Studio Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .scale(1.18f),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    Color(0xD83D47D6),
                    Color(0xAA6A5AE0),
                    Color(0x4540A9FF),
                  ),
                ),
              ),
          )
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                  )
                  Text(
                    text = "Active Workspace · ${activeProject.aspectRatio.label}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = activeProject.name,
                  color = Color.White,
                  fontSize = 21.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${activeProject.layers.size} layers · ${activeProject.resolutionLabel} @ ${activeProject.fps} FPS",
                  color = Color.White.copy(alpha = 0.85f),
                  fontSize = 12.sp,
                )
              }

              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                  contentDescription = "Open Editor",
                  tint = KytheraPeriwinkle,
                  modifier = Modifier.size(26.dp),
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(8.dp)
                  .clip(RoundedCornerShape(50))
                  .background(Color.White.copy(alpha = 0.28f)),
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(layerProgress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White),
                )
              }
              Text(
                text = "${activeProject.durationSec.toInt()}s",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
            }
          }
        }
      }
    }

    // 4. Quick Actions Row (matching reference 2 & 4 squircle tiles)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        QuickActionGlassTile(
          label = "New Project",
          sub = "Create",
          icon = Icons.Filled.Add,
          startColor = KytheraPeriwinkle,
          endColor = KytheraElectricBlue,
          testTag = "home_new_project_button",
          onClick = onNewProject,
          modifier = Modifier.weight(1f),
        )
        QuickActionGlassTile(
          label = "Import JSON",
          sub = "Project",
          icon = Icons.Filled.UploadFile,
          startColor = KytheraViolet,
          endColor = Color(0xFFB388FF),
          testTag = "home_import_json_button",
          onClick = onImportProject,
          modifier = Modifier.weight(1f),
        )
        QuickActionGlassTile(
          label = "Import ZIP",
          sub = "Bundle",
          icon = Icons.Filled.FolderZip,
          startColor = KytheraCoral,
          endColor = Color(0xFFFFA07A),
          testTag = "home_import_zip_button",
          onClick = onImportZip,
          modifier = Modifier.weight(1f),
        )
        QuickActionGlassTile(
          label = "JS Studio",
          sub = "Shaders",
          icon = Icons.Filled.Code,
          startColor = KytheraMint,
          endColor = KytheraCyan,
          testTag = "home_js_studio_button",
          onClick = onOpenJsStudio,
          modifier = Modifier.weight(1f),
        )
      }
    }

    // 5. Recent Projects Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Recent Projects",
          style = MaterialTheme.typography.titleLarge,
          color = glass.textPrimary,
        )
        Text(
          text = "${uiState.projects.size} saved locally",
          style = MaterialTheme.typography.bodySmall,
          color = glass.textSecondary,
        )
      }
    }

    items(uiState.projects, key = { it.id }) { project ->
      ProjectListGlassCard(
        project = project,
        isSelected = project.id == activeProject?.id,
        onOpen = { onOpenProject(project.id) },
        onDuplicate = { onDuplicateProject(project.id) },
        onDelete = { onDeleteProject(project.id) },
      )
    }
  }
}

@Composable
private fun HomeStatGlassTile(
  value: String,
  label: String,
  icon: ImageVector,
  startColor: Color,
  endColor: Color,
  modifier: Modifier = Modifier,
) {
  val glass = LocalKytheraGlass.current
  GlassCard(
    modifier = modifier,
    cornerRadius = 20.dp,
    contentPadding = 12.dp,
  ) {
    SquircleBadge(
      icon = icon,
      contentDescription = label,
      startColor = startColor,
      endColor = endColor,
      size = 34.dp,
      iconSize = 18.dp,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      color = glass.textPrimary,
      maxLines = 1,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = glass.textSecondary,
      maxLines = 1,
    )
  }
}

@Composable
private fun QuickActionGlassTile(
  label: String,
  sub: String,
  icon: ImageVector,
  startColor: Color,
  endColor: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val glass = LocalKytheraGlass.current
  GlassCard(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .testTag(testTag),
    cornerRadius = 18.dp,
    contentPadding = 10.dp,
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      SquircleBadge(
        icon = icon,
        contentDescription = label,
        startColor = startColor,
        endColor = endColor,
        size = 36.dp,
        iconSize = 18.dp,
      )
      Spacer(modifier = Modifier.height(7.dp))
      Text(
        text = label,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = glass.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = sub,
        fontSize = 10.sp,
        color = glass.textSecondary,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun ProjectListGlassCard(
  project: StudioProject,
  isSelected: Boolean,
  onOpen: () -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val accent = Color(project.accentColorHex)

  GlassCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("project_card_${project.id}"),
    cornerRadius = 20.dp,
    elevated = isSelected,
    contentPadding = 12.dp,
    onClick = onOpen,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Visual aspect-aware thumbnail preview
      Box(
        modifier = Modifier
          .size(width = 56.dp, height = 56.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(
            Brush.linearGradient(
              listOf(accent.copy(alpha = 0.9f), KytheraElectricBlue.copy(alpha = 0.75f)),
            ),
          )
          .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = project.aspectRatio.label,
          fontFamily = JetBrainsMonoFamily,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = project.name,
          style = MaterialTheme.typography.titleMedium,
          color = glass.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = accent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = "${project.resolutionLabel} · ${project.fps} FPS",
              fontFamily = JetBrainsMonoFamily,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              color = accent,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "${project.layers.size} layers · ${project.updatedLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = glass.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      IconButton(
        onClick = onDuplicate,
        modifier = Modifier.testTag("duplicate_${project.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.ContentCopy,
          contentDescription = "Duplicate ${project.name}",
          tint = glass.textSecondary,
          modifier = Modifier.size(18.dp),
        )
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier.testTag("delete_${project.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = "Delete ${project.name}",
          tint = glass.textMuted,
          modifier = Modifier.size(19.dp),
        )
      }
    }
  }
}
