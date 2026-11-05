package com.van.nhacviec

import android.app.Application
import com.van.nhacviec.data.Store
import com.van.nhacviec.notify.Notifier
import com.van.nhacviec.notify.Reminders

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Notifier.createChannels(this)
        Reminders.schedule(this)
    }
}
