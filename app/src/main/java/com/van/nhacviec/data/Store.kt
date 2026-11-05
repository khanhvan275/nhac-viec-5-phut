package com.van.nhacviec.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.van.nhacviec.widget.NhacViecWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Toàn bộ dữ liệu lưu trong một file JSON trên điện thoại.
 * Cập nhật app (cài đè) vẫn giữ nguyên file này.
 */
object Store {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data

    private var file: File? = null
    private var appContext: Context? = null

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        appContext = context.applicationContext
        val f = File(context.applicationContext.filesDir, "data.json")
        file = f
        if (f.exists()) {
            runCatching { json.decodeFromString(AppData.serializer(), f.readText()) }
                .onSuccess { _data.value = it }
                .onFailure {
                    // Giữ lại bản lỗi để không mất dữ liệu.
                    f.copyTo(File(f.parentFile, "data-loi-${System.currentTimeMillis()}.json"), true)
                }
        }
    }

    val value: AppData get() = _data.value

    @Synchronized
    fun update(block: (AppData) -> AppData) {
        val next = block(_data.value)
        _data.value = next
        save(next)
        refreshWidget()
    }

    private fun save(d: AppData) {
        val f = file ?: return
        val tmp = File(f.parentFile, "data.json.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(f)) {
            f.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun refreshWidget() {
        val ctx = appContext ?: return
        scope.launch { runCatching { NhacViecWidget().updateAll(ctx) } }
    }

    // ---- Các thao tác tiện dụng ----

    fun upsertTask(t: Task) = update { d ->
        val exists = d.tasks.any { it.id == t.id }
        d.copy(tasks = if (exists) d.tasks.map { if (it.id == t.id) t else it } else d.tasks + t)
    }

    fun deleteTask(id: String) = update { d ->
        d.copy(
            tasks = d.tasks.filterNot { it.id == id },
            timer = if (d.timer?.taskId == id) null else d.timer,
        )
    }

    fun setDone(id: String, done: Boolean) = update { d ->
        val td = today()
        d.copy(tasks = d.tasks.map { t ->
            if (t.id != id) t
            else if (t.daily) t.copy(doneDays = if (done) (t.doneDays + td).distinct().takeLast(400) else t.doneDays - td)
            else t.copy(doneAt = if (done) System.currentTimeMillis() else null)
        })
    }

    fun snooze(id: String) = update { d ->
        d.copy(tasks = d.tasks.map { if (it.id == id) it.copy(lastRemindAt = System.currentTimeMillis()) else it })
    }

    fun upsertCountdown(c: Countdown) = update { d ->
        val exists = d.countdowns.any { it.id == c.id }
        d.copy(countdowns = if (exists) d.countdowns.map { if (it.id == c.id) c else it } else d.countdowns + c)
    }

    fun deleteCountdown(id: String) = update { d -> d.copy(countdowns = d.countdowns.filterNot { it.id == id }) }

    fun setSettings(s: Settings) = update { it.copy(settings = s) }
}
