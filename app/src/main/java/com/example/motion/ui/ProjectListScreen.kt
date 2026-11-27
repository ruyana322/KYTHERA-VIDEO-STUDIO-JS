package com.example.motion.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.motion.model.ProjectSummary
import com.example.ui.components.GlassCard
import com.example.ui.components.SquircleBadge
import com.example.ui.theme.KytheraCrimson
import com.example.ui.theme.KytheraCyan
import com.example.ui.theme.KytheraElectricBlue
import com.example.ui.theme.KytheraPeriwinkle
import com.example.ui.theme.KytheraViolet
import com.example.ui.theme.LocalKytheraGlass
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectListScreen(
  projects: List<ProjectSummary>,
  isImportingZip: Boolean,
  darkMode: Boolean,
  onToggleTheme: () -> Unit,
  onNewProject: () -> Unit,
  onImportZip: () -> Unit,
  onLoadSample51s: () -> Unit,
  onOpenProject: (String) -> Unit,
  onRenameProject: (String, String) -> Unit,
  onDuplicateProject: (String) -> Unit,
  onExportZip: (String) -> Unit,
  onDeleteProject: (String) -> Unit,
) {
  val glass = LocalKytheraGlass.current
  var menuTarget by remember { mutableStateOf<ProjectSummary?>(null) }
  var renameTarget by remember { mutableStateOf<ProjectSummary?>(null) }
  var deleteTarget by remember { mutableStateOf<ProjectSummary?>(null) }

  Scaffold(
    containerColor = Color.Transparent,
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onNewProject,
        containerColor = KytheraPeriwinkle,
        contentColor = Color.White,
        icon = { Icon(Icons.Filled.Add, contentDescription = "Proyek Baru") },
        text = { Text("Proyek Baru", fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("fab_proyek_baru"),
      )
    },
  ) { innerPadding ->
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 164.dp),
      contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = innerPadding.calculateTopPadding() + 12.dp,
        bottom = innerPadding.calculateBottomPadding() + 92.dp,
      ),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize(),
    ) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Image(
                painter = painterResource(R.drawable.img_creator_avatar),
                contentDescription = "Avatar Studio",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape),
              )
              Column {
                Text(
                  text = "Kythera Studio",
                  style = MaterialTheme.typography.titleLarge,
                  color = glass.textPrimary,
                )
                Text(
                  text = "Motion Graphic 100% Offline · 1080×1920",
                  style = MaterialTheme.typography.bodySmall,
                  color = glass.textSecondary,
                )
              }
            }
            IconButton(
              onClick = onToggleTheme,
              modifier = Modifier.testTag("btn_toggle_tema"),
            ) {
              Icon(
                imageVector = if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Ubah Tema",
                tint = glass.textPrimary,
              )
            }
          }

          GlassCard(elevated = true, contentPadding = 16.dp) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              SquircleBadge(
                icon = Icons.Filled.Movie,
                contentDescription = "Render MP4",
                startColor = KytheraPeriwinkle,
                endColor = KytheraCyan,
                size = 48.dp,
                iconSize = 24.dp,
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Impor ZIP atau Buat Proyek Baru",
                  style = MaterialTheme.typography.titleMedium,
                  color = glass.textPrimary,
                )
                Text(
                  text = "Render H.264 + AAC 30 fps langsung ke Galeri (maks 60 detik).",
                  style = MaterialTheme.typography.bodySmall,
                  color = glass.textSecondary,
                )
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = onImportZip,
                enabled = !isImportingZip,
                colors = ButtonDefaults.buttonColors(containerColor = KytheraElectricBlue),
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_impor_zip"),
              ) {
                if (isImportingZip) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                  Icon(Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("Impor ZIP", maxLines = 1)
              }
              OutlinedButton(
                onClick = onLoadSample51s,
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_contoh_51d"),
              ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Contoh 51d", maxLines = 1)
              }
            }
          }

          Text(
            text = "Daftar Proyek (${projects.size})",
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
            modifier = Modifier.padding(top = 4.dp),
          )
        }
      }

      if (projects.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          GlassCard(contentPadding = 24.dp) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              SquircleBadge(
                icon = Icons.Filled.FolderZip,
                contentDescription = "Belum ada proyek",
                startColor = KytheraViolet,
                endColor = KytheraPeriwinkle,
                size = 54.dp,
                iconSize = 28.dp,
              )
              Text(
                text = "Belum ada proyek motion graphic",
                style = MaterialTheme.typography.titleMedium,
                color = glass.textPrimary,
              )
              Text(
                text = "Tekan 'Proyek Baru', 'Impor ZIP', atau coba 'Contoh 51d' (7 adegan lengkap).",
                style = MaterialTheme.typography.bodySmall,
                color = glass.textSecondary,
              )
            }
          }
        }
      } else {
        items(projects, key = { it.id }) { proj ->
          ProjectGridCard(
            project = proj,
            onClick = { onOpenProject(proj.id) },
            onLongClick = { menuTarget = proj },
          )
        }
      }
    }
  }

  menuTarget?.let { target ->
    ModalBottomSheet(onDismissRequest = { menuTarget = null }) {
      Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
          text = target.judul,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        ListItem(
          headlineContent = { Text("Ganti nama") },
          leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
          modifier = Modifier
            .combinedClickable(onClick = {
              renameTarget = target
              menuTarget = null
            })
            .testTag("menu_ganti_nama"),
        )
        ListItem(
          headlineContent = { Text("Duplikat") },
          leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
          modifier = Modifier
            .combinedClickable(onClick = {
              onDuplicateProject(target.id)
              menuTarget = null
            })
            .testTag("menu_duplikat"),
        )
        ListItem(
          headlineContent = { Text("Ekspor ZIP") },
          leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
          modifier = Modifier
            .combinedClickable(onClick = {
              onExportZip(target.id)
              menuTarget = null
            })
            .testTag("menu_ekspor_zip"),
        )
        ListItem(
          headlineContent = { Text("Hapus", color = KytheraCrimson) },
          leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null, tint = KytheraCrimson) },
          modifier = Modifier
            .combinedClickable(onClick = {
              deleteTarget = target
              menuTarget = null
            })
            .testTag("menu_hapus"),
        )
      }
    }
  }

  renameTarget?.let { target ->
    var draftName by remember(target.id) { mutableStateOf(target.judul) }
    AlertDialog(
      onDismissRequest = { renameTarget = null },
      title = { Text("Ganti Nama Proyek") },
      text = {
        OutlinedTextField(
          value = draftName,
          onValueChange = { draftName = it },
          label = { Text("Judul proyek") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(onClick = {
          onRenameProject(target.id, draftName)
          renameTarget = null
        }) {
          Text("Simpan")
        }
      },
      dismissButton = {
        TextButton(onClick = { renameTarget = null }) { Text("Batal") }
      },
    )
  }

  deleteTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text("Hapus Proyek?") },
      text = { Text("Proyek '${target.judul}' dan seluruh aset di dalamnya akan dihapus permanen.") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteProject(target.id)
            deleteTarget = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = KytheraCrimson),
        ) {
          Text("Hapus")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
      },
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectGridCard(
  project: ProjectSummary,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val glass = LocalKytheraGlass.current
  val thumbBmp = remember(project.thumbnailPath, project.updatedAtMs) {
    project.thumbnailPath?.let { path ->
      val f = File(path)
      if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
    }
  }
  val dateStr = remember(project.updatedAtMs) {
    SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")).format(Date(project.updatedAtMs))
  }

  GlassCard(
    contentPadding = 10.dp,
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .testTag("card_proyek_${project.id}"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(9f / 12f)
        .clip(RoundedCornerShape(14.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0A1020), Color(0xFF172244)),
          ),
        ),
    ) {
      if (thumbBmp != null) {
        Image(
          bitmap = thumbBmp,
          contentDescription = "Thumbnail ${project.judul}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Icon(
          imageVector = Icons.Filled.Movie,
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.5f),
          modifier = Modifier
            .size(36.dp)
            .align(Alignment.Center),
        )
      }
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color.Black.copy(alpha = 0.7f))
          .padding(horizontal = 8.dp, vertical = 3.dp),
      ) {
        Text(
          text = "${project.durasiDetik.toInt()} dtk · ${project.sceneCount} adg",
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = project.judul,
      style = MaterialTheme.typography.titleMedium,
      color = glass.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = "${project.gaya.label} · $dateStr",
      style = MaterialTheme.typography.bodySmall,
      color = glass.textSecondary,
      maxLines = 1,
    )
  }
}
