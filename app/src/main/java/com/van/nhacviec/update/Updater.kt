package com.van.nhacviec.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.van.nhacviec.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tự kiểm tra bản mới trên GitHub Releases và cài đè.
 * Mỗi lần GitHub đóng gói, số bản (build-N) tăng lên.
 */
object Updater {
    data class Release(val build: Int, val name: String, val notes: String, val apkUrl: String)

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val r: Release) : State
        data class Downloading(val r: Release, val percent: Int) : State
        data class Failed(val msg: String) : State
    }

    val state = MutableStateFlow<State>(State.Idle)
    private var lastCheck = 0L

    suspend fun check(force: Boolean = false) {
        if (BuildConfig.REPO.isBlank()) {
            if (force) state.value = State.Failed("Bản này chưa gắn với GitHub")
            return
        }
        val now = System.currentTimeMillis()
        if (!force && now - lastCheck < 3 * 60 * 60_000L) return
        if (state.value is State.Downloading) return
        lastCheck = now
        if (force) state.value = State.Checking
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val c = URL("https://api.github.com/repos/${BuildConfig.REPO}/releases/latest")
                    .openConnection() as HttpURLConnection
                c.setRequestProperty("Accept", "application/vnd.github+json")
                c.connectTimeout = 15_000
                c.readTimeout = 15_000
                val text = c.inputStream.bufferedReader().use { it.readText() }
                val o = Json.parseToJsonElement(text).jsonObject
                val tag = o["tag_name"]?.jsonPrimitive?.content ?: ""
                val build = tag.filter { it.isDigit() }.toIntOrNull() ?: 0
                val apk = o["assets"]?.jsonArray
                    ?.map { it.jsonObject }
                    ?.firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true }
                    ?.get("browser_download_url")?.jsonPrimitive?.content
                Release(
                    build = build,
                    name = o["name"]?.jsonPrimitive?.content ?: tag,
                    notes = o["body"]?.jsonPrimitive?.content ?: "",
                    apkUrl = apk ?: "",
                )
            }
        }
        result.onSuccess { r ->
            state.value = if (r.build > BuildConfig.VERSION_CODE && r.apkUrl.isNotBlank()) State.Available(r)
            else if (force) State.UpToDate else State.Idle
        }.onFailure {
            if (force) state.value = State.Failed("Không kết nối được, thử lại sau nhé")
        }
    }

    /** Tải bản mới rồi mở màn hình cài đặt của Android. */
    suspend fun downloadAndInstall(ctx: Context, r: Release) {
        if (!ctx.packageManager.canRequestPackageInstalls()) {
            val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
            state.value = State.Available(r)
            return
        }
        state.value = State.Downloading(r, 0)
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
                val f = File(dir, "NhacViec5Phut.apk")
                if (f.exists()) f.delete()
                val c = URL(r.apkUrl).openConnection() as HttpURLConnection
                c.instanceFollowRedirects = true
                c.connectTimeout = 20_000
                c.readTimeout = 60_000
                val total = c.contentLengthLong
                c.inputStream.use { input ->
                    f.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var read = 0L
                        var lastPct = -1
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            read += n
                            if (total > 0) {
                                val pct = (read * 100 / total).toInt()
                                if (pct != lastPct) {
                                    lastPct = pct
                                    state.value = State.Downloading(r, pct)
                                }
                            }
                        }
                    }
                }
                f
            }
        }
        file.onSuccess { f ->
            state.value = State.Available(r)
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
            val i = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        }.onFailure {
            state.value = State.Failed("Tải bản mới bị lỗi, thử lại nhé")
        }
    }
}
