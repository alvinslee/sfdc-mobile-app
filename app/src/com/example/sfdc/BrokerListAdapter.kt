package com.example.sfdc

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast
import com.salesforce.androidsdk.rest.ApiVersionStrings
import com.salesforce.androidsdk.rest.RestClient
import com.salesforce.androidsdk.rest.RestRequest
import com.salesforce.androidsdk.rest.RestResponse

class BrokerListAdapter : ArrayAdapter<String> {
    internal var context: Context
    private var client: RestClient? = null
    private val nameToId: MutableMap<String, String> = mutableMapOf()

    constructor(context: Context) : super(context, R.layout.broker_item, ArrayList<String>(0)) {
        this.context = context;
    }

    fun setClient(client: RestClient?) {
        this.client = client
    }

    fun map(name: String, id: String) {
        this.nameToId.put(name, id)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView: View = inflater.inflate(R.layout.broker_item, parent, false)
        val editText = rowView.findViewById<View>(R.id.broker_field) as EditText
        var item = getItem(position)
        var brokerId = nameToId.get(item)
        val fields: MutableMap<String, String> =  mutableMapOf()

        editText.setText(item)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                item = editText.text.toString()

                fields.put("Name", item!!)
                val restRequest = RestRequest.getRequestForUpdate(ApiVersionStrings.getVersionNumber(context), "Broker__c", brokerId,
                        fields as Map<String, Any>?
                )
                client!!.sendAsync(restRequest, object : RestClient.AsyncRequestCallback {
                    override fun onSuccess(request: RestRequest, result: RestResponse) {}

                    override fun onError(exception: Exception) {}
                })
            }
        })
        return rowView
    }
}