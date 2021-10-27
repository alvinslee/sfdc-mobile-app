package com.example.sfdc

import android.content.AsyncTaskLoader
import android.content.Context
import android.content.Intent
import android.util.Log
import com.salesforce.androidsdk.accounts.UserAccount
import com.salesforce.androidsdk.app.SalesforceSDKManager
import com.salesforce.androidsdk.mobilesync.app.MobileSyncSDKManager
import com.salesforce.androidsdk.mobilesync.manager.SyncManager
import com.salesforce.androidsdk.mobilesync.manager.SyncManager.MobileSyncException
import com.salesforce.androidsdk.mobilesync.manager.SyncManager.SyncUpdateCallback
import com.salesforce.androidsdk.mobilesync.util.SyncState
import com.salesforce.androidsdk.smartstore.store.QuerySpec
import com.salesforce.androidsdk.smartstore.store.SmartSqlHelper.SmartSqlException
import com.salesforce.androidsdk.smartstore.store.SmartStore
import org.json.JSONArray
import org.json.JSONException
import java.util.ArrayList

class BrokerListLoader(context: Context?, account: UserAccount?) :
        AsyncTaskLoader<List<String>?>(context) {
    private val smartStore: SmartStore
    private val syncMgr: SyncManager
    override fun loadInBackground(): List<String>? {
        if (!smartStore.hasSoup(BROKER_SOUP)) {
            return null
        }
        val querySpec = QuerySpec.buildAllQuerySpec(
                BROKER_SOUP,
                "name", QuerySpec.Order.ascending, LIMIT
        )
        val results: JSONArray
        val brokers: MutableList<String> = ArrayList<String>()
        try {
            results = smartStore.query(querySpec, 0)
            for (i in 0 until results.length()) {
                brokers.add(results.getJSONObject(i).getString("Name"))
            }
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException occurred while parsing", e)
        } catch (e: SmartSqlException) {
            Log.e(TAG, "SmartSqlException occurred while fetching data", e)
        }
        return brokers
    }

    @Synchronized
    fun syncUp() {
        try {
            syncMgr.reSync(
                    SYNC_UP_NAME
            ) { sync ->
                if (SyncState.Status.DONE == sync.status) {
                    syncDown()
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException occurred while parsing", e)
        } catch (e: MobileSyncException) {
            Log.e(TAG, "MobileSyncException occurred while attempting to sync up", e)
        }
    }

    /**
     * Pulls the latest records from the server.
     */
    @Synchronized
    fun syncDown() {
        try {
            syncMgr.reSync(
                    SYNC_DOWN_NAME
            ) { sync ->
                if (SyncState.Status.DONE == sync.status) {
                    fireLoadCompleteIntent()
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException occurred while parsing", e)
        } catch (e: MobileSyncException) {
            Log.e(TAG, "MobileSyncException occurred while attempting to sync down", e)
        }
    }

    private fun fireLoadCompleteIntent() {
        val intent = Intent(LOAD_COMPLETE_INTENT_ACTION)
        SalesforceSDKManager.getInstance().appContext.sendBroadcast(intent)
    }

    companion object {
        const val BROKER_SOUP = "brokers"
        const val LOAD_COMPLETE_INTENT_ACTION =
                "com.salesforce.samples.mobilesyncexplorer.loaders.LIST_LOAD_COMPLETE"
        private const val TAG = "BrokerListLoader"
        private const val SYNC_DOWN_NAME = "syncDownBrokers"
        private const val SYNC_UP_NAME = "syncUpBrokers"
        private const val LIMIT = 10000
    }

    init {
        val sdkManager = MobileSyncSDKManager.getInstance()
        smartStore = sdkManager.getSmartStore(account)
        syncMgr = SyncManager.getInstance(account)
        // Setup schema if needed
        sdkManager.setupUserStoreFromDefaultConfig()
        // Setup syncs if needed
        sdkManager.setupUserSyncsFromDefaultConfig()
    }
}