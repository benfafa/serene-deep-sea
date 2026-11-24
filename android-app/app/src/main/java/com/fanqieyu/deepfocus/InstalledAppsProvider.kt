package com.fanqieyu.deepfocus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
)

object InstalledAppsProvider {

    suspend fun loadLaunchableApps(context: Context): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }.getOrElse {
            pm.queryIntentActivities(intent, 0)
        }
        val selfPackage = context.packageName
        val seen = HashSet<String>()
        val list = mutableListOf<InstalledAppInfo>()
        for (info in resolved) {
            val activityInfo = info.activityInfo ?: continue
            val pkg = activityInfo.packageName ?: continue
            if (pkg == selfPackage || !seen.add(pkg)) continue
            val label = runCatching { info.loadLabel(pm).toString().trim() }
                .getOrDefault(pkg)
                .ifBlank { pkg }
            val iconBitmap = runCatching {
                val drawable = info.loadIcon(pm)
                val size = 72
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                bmp.asImageBitmap()
            }.getOrNull()
            list += InstalledAppInfo(
                packageName = pkg,
                appName = label,
                icon = iconBitmap,
            )
        }
        list.sortedBy { it.appName.lowercase() }
    }
}
