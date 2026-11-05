package com.van.nhacviec.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.van.nhacviec.data.Store
import com.van.nhacviec.timer.TimerController

/** Xử lý các nút bấm trên thông báo. */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TASK = "task_id"
        const val SNOOZE = "com.van.nhacviec.SNOOZE"
        const val DONE = "com.van.nhacviec.DONE"
        const val T_PAUSE = "com.van.nhacviec.T_PAUSE"
        const val T_RESUME = "com.van.nhacviec.T_RESUME"
        const val T_DONE = "com.van.nhacviec.T_DONE"
        const val T_STOP = "com.van.nhacviec.T_STOP"
        const val T_PLUS10 = "com.van.nhacviec.T_PLUS10"
        const val T_KEEP = "com.van.nhacviec.T_KEEP"
        const val T_PROMPT = "com.van.nhacviec.T_PROMPT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        val id = intent.getStringExtra(EXTRA_TASK)
        when (intent.action) {
            SNOOZE -> id?.let {
                Store.snooze(it)
                Notifier.cancel(context, it.hashCode())
            }
            DONE -> id?.let {
                Store.setDone(it, true)
                Notifier.cancel(context, it.hashCode())
            }
            T_PAUSE -> TimerController.pause(context)
            T_RESUME -> TimerController.resume(context)
            T_DONE -> TimerController.finish(context, markDone = true)
            T_STOP -> TimerController.finish(context, markDone = false)
            T_PLUS10 -> TimerController.extend(context, 10)
            T_KEEP -> TimerController.keepGoing(context)
            T_PROMPT -> TimerController.showPrompt(context)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.init(context)
        Reminders.schedule(context)
        TimerController.restore(context)
    }
}
