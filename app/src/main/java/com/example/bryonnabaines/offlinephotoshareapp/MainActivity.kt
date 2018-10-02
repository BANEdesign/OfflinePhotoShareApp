package com.example.bryonnabaines.offlinephotoshareapp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_CONNECT_DEVICE = 3
    //    private var mChatService: BluetoothChatService? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null

    private val SELECT_IMAGE = 11
    private val MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2
    private var selectedImagePath: String? = null
    private var selectedImage: ImageView? = null

    private var mFileName: String? = null

    val DEVICE_NAME = "device_name"
    val TOAST = "toast"

    val MESSAGE_STATE_CHANGE = 1
    val MESSAGE_READ = 2
    val MESSAGE_WRITE = 3
    val MESSAGE_DEVICE_NAME = 4
    val MESSAGE_TOAST = 5

    private var mConversationArrayAdapter: ArrayAdapter<String>? = null

    private var mConnectedDeviceName: String? = null

    private var mOutStringBuffer: StringBuffer? = null
    private var mConversationView: ListView? = null
    private var mEditText: EditText? = null
    private var mButtonSend: ImageButton? = null
    lateinit var connectWiFi: ImageButton
    lateinit var connectBT: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
        // Record to the external cache directory for visibility
        mFileName = externalCacheDir!!.absolutePath
        mFileName += "/audiorecordtest.3gp"

        connectBT.setOnClickListener {
//            val bluetoothIntent = Intent(applicationContext, DeviceListActivity::class.java)
//            startActivityForResult(bluetoothIntent, REQUEST_CONNECT_DEVICE)
        }
    }

    fun init() {
        connectWiFi = btn_connect_wifi
        connectBT = btn_connect_bt
        mConversationView = message_history
        mEditText = edit_text_text_message
        mButtonSend = btn_send
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show()
            //            finish();
        }
    }
}
