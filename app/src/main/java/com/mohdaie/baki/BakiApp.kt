package com.mohdaie.baki

import android.app.Application
import com.mohdaie.baki.data.AppDatabase
import com.mohdaie.baki.data.Repository
import com.mohdaie.baki.service.Notifier

class BakiApp : Application() {

    lateinit var repository: Repository
        private set

    /** True while the main screen is on screen, so we don't also open the popup activity. */
    @Volatile
    var mainVisible: Boolean = false

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this, AppDatabase.build(this))
        Notifier.createChannel(this)
    }
}
