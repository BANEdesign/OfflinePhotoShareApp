package com.example.bryonnabaines.offlinephotoshareapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 3;
    private BluetoothChatService mChatService = null;
    private BluetoothAdapter mBluetoothAdapter = null;

    private static final int SELECT_IMAGE = 11;
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private ImageView selectedImage;

    private static final int REQUEST_CAMERA_PERMISSION = 8;
    static final int REQUEST_IMAGE_CAPTURE = 100;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private ArrayAdapter<String> mConversationArrayAdapter;

    private String mConnectedDeviceName = null;

    private StringBuffer mOutStringBuffer;
    private EditText mEditText;
    private ImageButton mButtonSend;
    ImageButton takePhoto, connectBT, attachPhoto;
    Switch makeDiscoverable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        connectBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bluetoothIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(bluetoothIntent, REQUEST_CONNECT_DEVICE);
            }
        });

    }

    public void init() {
//        connectWiFi = findViewById(R.id.btn_connect_wifi);
        connectBT = findViewById(R.id.btn_connect_bt);
        takePhoto = findViewById(R.id.btn_take_photo);
        selectedImage = findViewById(R.id.selected_image);
        // you might want to bring this back if you enable messages
//        mConversationView = findViewById(R.id.message_history);
        mEditText = findViewById(R.id.edit_text_text_message);
        mButtonSend = findViewById(R.id.btn_send);
        attachPhoto = findViewById(R.id.btn_photo_attach);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        makeDiscoverable = findViewById(R.id.menu_make_discoverable);

        makeDiscoverable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(makeDiscoverable.isChecked())
                    ensureDiscoverable();
            }
        });

        attachPhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PhotoMessage(v);
            }
        });

        takePhoto.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                takePicture(v);
            }
        });

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_make_discoverable:
            ensureDiscoverable();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void PhotoMessage(View view) {
        permissionCheck();
    }

    public void permissionCheck() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                takePhoto.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show explanation
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Add your explanation for the user here.
                Toast.makeText(this, "You have declined the permissions. Please allow them first to proceed.", Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed, we can request the permission
                ActivityCompat.requestPermissions(this, new String[]
                {Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }

        } else {
            // Request image from the gallery app
            requestImageFromGallery();
        }
    }

    public void requestImageFromGallery() {
        Intent attachImageIntent = new Intent();
        attachImageIntent.setType("image/*");
        attachImageIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(attachImageIntent, "Select Picture"), SELECT_IMAGE);
    }

    public void takePicture(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();

                // Initialize the BluetoothChatService to perform bluetooth connection
                mChatService = new BluetoothChatService(getApplicationContext(), mHandler);
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                finish();
            }
            break;

            case SELECT_IMAGE:
            if (resultCode == RESULT_OK) {

                Uri selectedImageUri = data.getData();

                // Attach the selectedImage to the ImageView
                selectedImage.setImageURI(selectedImageUri);
            }
            break;

            case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data);
            }
            break;

            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    Bundle extras = data.getExtras();
                    if(extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        selectedImage.setImageBitmap(imageBitmap);
                    }
                }
        }
    }


    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //FragmentActivity activity = getActivity();
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                    case BluetoothChatService.STATE_CONNECTED:
                    connectBT.setImageResource(R.drawable.ic_bluetooth_connected_black_24dp);
                    mConversationArrayAdapter.clear();
                    break;
                    case BluetoothChatService.STATE_CONNECTING:
                    connectBT.setImageResource(R.drawable.ic_bluetooth_searching_black_24dp);
                    break;
                    case BluetoothChatService.STATE_LISTEN:
                    case BluetoothChatService.STATE_NONE:
                    connectBT.setImageResource(R.drawable.ic_bluetooth_disabled_black_24dp);
                    //setStatus(R.string.title_not_connected);
                    break;
                }
                break;
                case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
                case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                break;
                case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                if (null != getApplicationContext()) {
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                }
                break;
                case MESSAGE_TOAST:
                if (null != getApplicationContext()) {
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    };

    public String getPath(Uri uri) {
        if (uri == null) {
            Toast.makeText(getApplicationContext(), "Uri is null", Toast.LENGTH_SHORT).show();
            return null;
        }

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        // this is our fallback here
        return uri.getPath();
    }


    private void setupChat() {
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

//        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                String message = mEditText.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getApplicationContext(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
    = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Do the external storage related work here.

                    requestImageFromGallery();
                } else {
                    // Permission is denied.
                    Toast.makeText(this, "Can't Proceed. You rejected the permission.", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto.setEnabled(true);
                    //TODO start camera activity
                }
            }
            break;
        }
    }
}
