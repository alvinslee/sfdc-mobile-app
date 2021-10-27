package com.example.sfdc

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.salesforce.androidsdk.accounts.UserAccount
import com.salesforce.androidsdk.accounts.UserAccountManager
import com.salesforce.androidsdk.app.SalesforceSDKManager
import com.example.sfdc.BrokerListLoader

class BrokerSyncAdapter
(
        context: Context?, autoInitialize: Boolean,
        allowParallelSyncs: Boolean
) :
        AbstractThreadedSyncAdapter(context, autoInitialize, allowParallelSyncs) {
    override fun onPerformSync(
            account: Account, extras: Bundle, authority: String,
            provider: ContentProviderClient, syncResult: SyncResult
    ) {
        val syncDownOnly = extras.getBoolean(SYNC_DOWN_ONLY, false)
        val sdkManager = SalesforceSDKManager.getInstance()
        val accManager = sdkManager.userAccountManager
        if (sdkManager.isLoggingOut || accManager.authenticatedUsers == null) {
            return
        }
        if (account != null) {
            val user = sdkManager.userAccountManager.buildUserAccount(account)
            val contactLoader = BrokerListLoader(context, user)
            if (syncDownOnly) {
                contactLoader.syncDown()
            } else {
                contactLoader.syncUp() // does a sync up followed by a sync down
            }
        }
    }

    companion object {
        // Key for extras bundle
        const val SYNC_DOWN_ONLY = "syncDownOnly"
    }
}