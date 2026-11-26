package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudioTab
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraViolet
import com.example.ui.theme.LocalKytheraGlass

@Composable
fun AmbientGlassBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val glass = LocalKytheraGlass.current
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(glass.bgTop, glass.bgMid, glass.bgBottom),
        ),
      )
      .drawBehind {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(glass.blobPrimary.copy(alpha = 0.55f), Color.Transparent),
            center = Offset(size.width * 0.15f, size.height * 0.14f),
            radius = size.maxDimension * 0.48f,
          ),
          radius = size.maxDimension * 0.48f,
          center = Offset(size.width * 0.15f, size.height * 0.14f),
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(glass.blobSecondary.copy(alpha = 0.50f), Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.24f),
            radius = size.maxDimension * 0.42f,
          ),
          radius = size.maxDimension * 0.42f,
          center = Offset(size.width * 0.88f, size.height * 0.24f),
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(glass.blobTertiary.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(size.width * 0.72f, size.height * 0.88f),
            radius = size.maxDimension * 0.45f,
          ),
          radius = size.maxDimension * 0.45f,
          center = Offset(size.width * 0.72f, size.height * 0.88f),
        )
      },
    content = content,
  )
}

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 22.dp,
  elevated: Boolean = false,
  onClick: (() -> Unit)? = null,
  contentPadding: Dp = 14.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val shape = RoundedCornerShape(cornerRadius)
  val bg = if (elevated) glass.glassElevated else glass.glassSurface

  Surface(
    modifier = modifier
      .shadow(
        elevation = if (glass.isDark) 10.dp else 12.dp,
        shape = shape,
        ambientColor = KytheraPeriwinkle.copy(alpha = if (glass.isDark) 0.22f else 0.14f),
        spotColor = KytheraPeriwinkle.copy(alpha = if (glass.isDark) 0.24f else 0.12f),
      )
      .clip(shape)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    shape = shape,
    color = bg,
    border = BorderStroke(1.dp, glass.glassBorder),
  ) {
    Column(
      modifier = Modifier
        .background(
          Brush.linearGradient(
            colors = listOf(
              Color.White.copy(alpha = if (glass.isDark) 0.06f else 0.36f),
              Color.Transparent,
            ),
          ),
        )
        .padding(contentPadding),
      content = content,
    )
  }
}

@Composable
fun SquircleBadge(
  icon: ImageVector,
  contentDescription: String,
  startColor: Color,
  endColor: Color,
  size: Dp = 40.dp,
  iconSize: Dp = 20.dp,
) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(13.dp))
      .background(Brush.linearGradient(listOf(startColor, endColor)))
      .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(13.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = Color.White,
      modifier = Modifier.size(iconSize),
    )
  }
}

@Composable
fun FrostedFloatingBottomBar(
  activeTab: StudioTab,
  onSelectTab: (StudioTab) -> Unit,
  onNewProjectClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val glass = LocalKytheraGlass.current
  Box(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(
          elevation = 18.dp,
          shape = RoundedCornerShape(28.dp),
          ambientColor = KytheraPeriwinkle.copy(alpha = 0.22f),
          spotColor = KytheraPeriwinkle.copy(alpha = 0.22f),
        ),
      shape = RoundedCornerShape(28.dp),
      color = glass.glassElevated,
      border = BorderStroke(1.dp, glass.glassBorder),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FloatingNavItem(
          label = "Home",
          selected = activeTab == StudioTab.HOME,
          selectedIcon = Icons.Filled.Home,
          unselectedIcon = Icons.Outlined.Home,
          testTag = "nav_home",
          onClick = { onSelectTab(StudioTab.HOME) },
          modifier = Modifier.weight(1f),
        )
        FloatingNavItem(
          label = "Editor",
          selected = activeTab == StudioTab.EDITOR,
          selectedIcon = Icons.Filled.Movie,
          unselectedIcon = Icons.Outlined.Movie,
          testTag = "nav_editor",
          onClick = { onSelectTab(StudioTab.EDITOR) },
          modifier = Modifier.weight(1f),
        )

        // Centered glowing + FAB matching user's reference 1 & 4
        Box(
          modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(50.dp)
            .shadow(10.dp, CircleShape, ambientColor = KytheraPeriwinkle, spotColor = KytheraPeriwinkle)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(KytheraPeriwinkle, KytheraElectricBlue, KytheraCyan)))
            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onNewProjectClick)
            .testTag("nav_new_project_fab"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "New Project",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
          )
        }

        FloatingNavItem(
          label = "Scripts",
          selected = activeTab == StudioTab.SCRIPTS,
          selectedIcon = Icons.Filled.Code,
          unselectedIcon = Icons.Outlined.Code,
          testTag = "nav_scripts",
          onClick = { onSelectTab(StudioTab.SCRIPTS) },
          modifier = Modifier.weight(1f),
        )
        FloatingNavItem(
          label = "Export",
          selected = activeTab == StudioTab.EXPORT,
          selectedIcon = Icons.Filled.Upload,
          unselectedIcon = Icons.Outlined.Upload,
          testTag = "nav_export",
          onClick = { onSelectTab(StudioTab.EXPORT) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun FloatingNavItem(
  label: String,
  selected: Boolean,
  selectedIcon: ImageVector,
  unselectedIcon: ImageVector,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val glass = LocalKytheraGlass.current
  val tint = if (selected) KytheraPeriwinkle else glass.textSecondary
  Column(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(18.dp))
      .background(if (selected) KytheraPeriwinkle.copy(alpha = 0.12f) else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = if (selected) selectedIcon else unselectedIcon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      color = tint,
      fontSize = 10.5.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
    )
  }
}

@Composable
fun FrostedNavigationRail(
  activeTab: StudioTab,
  onSelectTab: (StudioTab) -> Unit,
  onNewProjectClick: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  NavigationRail(
    containerColor = glass.glassElevated,
    modifier = Modifier
      .fillMaxHeight()
      .width(84.dp)
      .border(1.dp, glass.glassBorder),
    header = {
      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Brush.linearGradient(listOf(KytheraPeriwinkle, KytheraViolet)))
          .clickable(onClick = onNewProjectClick)
          .testTag("rail_new_project_fab"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Filled.Add, contentDescription = "New Project", tint = Color.White)
      }
      Spacer(modifier = Modifier.height(12.dp))
    },
  ) {
    val entries = listOf(
      Triple(StudioTab.HOME, "Home", Icons.Filled.Home),
      Triple(StudioTab.EDITOR, "Editor", Icons.Filled.Movie),
      Triple(StudioTab.SCRIPTS, "Scripts", Icons.Filled.Code),
      Triple(StudioTab.EXPORT, "Export", Icons.Filled.Upload),
    )
    entries.forEach { (tab, label, icon) ->
      NavigationRailItem(
        selected = activeTab == tab,
        onClick = { onSelectTab(tab) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
      )
    }
  }
}
