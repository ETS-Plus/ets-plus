package app.etsprivate.transit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkCost {
    fun free(context: Context): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
