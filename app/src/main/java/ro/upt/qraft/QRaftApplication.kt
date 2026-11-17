package ro.upt.qraft

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ro.upt.qraft.data.sync.SyncManager
import javax.inject.Inject

@HiltAndroidApp
class QRaftApplication : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        syncManager.start()
    }
}
