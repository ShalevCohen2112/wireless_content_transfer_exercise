package com.wirelesscontenttransferexercise;

import android.Manifest;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abemart.wroup.client.WroupClient;
import com.abemart.wroup.common.WiFiDirectBroadcastReceiver;
import com.abemart.wroup.common.WiFiP2PError;
import com.abemart.wroup.common.WiFiP2PInstance;
import com.abemart.wroup.common.WroupDevice;
import com.abemart.wroup.common.WroupServiceDevice;
import com.abemart.wroup.common.listeners.ClientConnectedListener;
import com.abemart.wroup.common.listeners.ClientDisconnectedListener;
import com.abemart.wroup.common.listeners.DataReceivedListener;
import com.abemart.wroup.common.listeners.ServiceDiscoveredListener;
import com.abemart.wroup.common.listeners.ServiceRegisteredListener;
import com.abemart.wroup.common.messages.MessageWrapper;
import com.abemart.wroup.service.WroupService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DataReceivedListener, ClientConnectedListener, ClientDisconnectedListener {


    private static final String TAG = "MainActivityTAG";
    private final String source = "SOURCE";
    private final String target = "TARGET";

    private WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver;
    private WroupService wroupService;
    private WroupClient wroupClient;
    private ListView availableDevicesLv;
    ArrayAdapter<String> itemsAdapter;
    List<WroupServiceDevice> deviceList = new ArrayList<>();
    AlertDialog pickOptionDialog;
    boolean isHost = false;
    RecyclerView contactsRv;
ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: " + Build.MODEL);
        wiFiDirectBroadcastReceiver = WiFiP2PInstance.getInstance(this).getBroadcastReceiver();
        initViews();
        setListener();
        startSearchingProcess();


    }

    private void startSearchingProcess() {
        new Handler().postDelayed(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                askLocationPermeation();
            }
        }, 1000);


    }


    private void initViews() {
        availableDevicesLv = findViewById(R.id.available_devices_lv);
        itemsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        availableDevicesLv.setAdapter(itemsAdapter);

        contactsRv = findViewById(R.id.contacts_rv);
        contactsRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        progressBar=findViewById(R.id.progress_bar);
    }

    private void setListener() {
        availableDevicesLv.setOnItemClickListener((parent, view, position, id) -> {
            final WroupServiceDevice serviceSelected = deviceList.get(position);

            wroupClient.connectToService(serviceSelected, serviceDevice -> {
                Log.d(TAG, "onServiceConnected: connected" + serviceDevice.getDeviceName());

                showSourceOrTargetDialog(true);
            });

        });


    }

    private void searchAvailableDevices() {

        new Handler().postDelayed(() -> {
            wroupClient = WroupClient.getInstance(getApplicationContext());
            wroupClient.setDataReceivedListener(this);
            wroupClient.setClientDisconnectedListener(this);
            wroupClient.setClientConnectedListener(this);
            wroupClient.discoverServices(5000L, new ServiceDiscoveredListener() {

                @Override
                public void onNewServiceDeviceDiscovered(WroupServiceDevice serviceDevice) {
                    Log.i(TAG, "New group found:");
                    Log.i(TAG, "\tName: " + serviceDevice.getTxtRecordMap().get(WroupService.SERVICE_GROUP_NAME));
                    deviceList.add(serviceDevice);
                    itemsAdapter.add(serviceDevice.getTxtRecordMap().get(WroupService.SERVICE_GROUP_NAME));
                    itemsAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFinishServiceDeviceDiscovered(List<WroupServiceDevice> serviceDevices) {
                    Log.i(TAG, "Found '" + serviceDevices.size() + "' devices");
                    if (serviceDevices.isEmpty()) {
                        searchAvailableDevices();
                    }
                }

                @Override
                public void onError(WiFiP2PError wiFiP2PError) {
//                    Toast.makeText(getApplicationContext(), "Error searching devices: " + wiFiP2PError, Toast.LENGTH_LONG).show();
                }
            });

        }, 1000);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void askLocationPermeation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            registerService();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);


        }
    }


    private void registerService() {

        wroupService = WroupService.getInstance(getApplicationContext());
        wroupService.setDataReceivedListener(this);
        wroupService.setClientDisconnectedListener(this);
        wroupService.setClientConnectedListener(this);
        wroupService.registerService(android.os.Build.MODEL, new ServiceRegisteredListener() {
            @Override
            public void onSuccessServiceRegistered() {
                Log.d(TAG, "Register service Success");
                searchAvailableDevices();
            }

            @Override
            public void onErrorServiceRegistered(WiFiP2PError wiFiP2PError) {
                Log.d(TAG, "Error creating group " + wiFiP2PError.toString());
                Toast.makeText(getApplicationContext(), "Error creating group", Toast.LENGTH_SHORT).show();
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(wiFiDirectBroadcastReceiver, intentFilter);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // TODO: 1/1/2021 make batter
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                registerService();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    askLocationPermeation();
                }
            }

        } else if (requestCode == 1001) {
            sendPhoneBookAndPhotos();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wiFiDirectBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        disconnect();
    }

    private void disconnect() {
        if (wroupService != null) {
            wroupService.disconnect();
        }

        if (wroupClient != null) {
            wroupClient.disconnect();
        }

    }


    @Override
    public void onClientConnected(WroupDevice wroupDevice) {
        Log.d(TAG, "onClientConnected: ");

        runOnUiThread(() -> showSourceOrTargetDialog(false));

    }

    private void showSourceOrTargetDialog(boolean isHost) {
        this.isHost = isHost;

        runOnUiThread(() -> {
            List<String> options = new ArrayList<>();
            options.add("Source");
            options.add("target");
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Select a group");
            builder.setItems(options.toArray(new String[2]), (dialog, which) -> {
                MessageWrapper normalMessage = new MessageWrapper();
                normalMessage.setMessageType(MessageWrapper.MessageType.NORMAL);

                progressBar.setVisibility(View.VISIBLE);

                if (which == 0) {
                    normalMessage.setMessage(target);

                } else {
                    normalMessage.setMessage(source);
                }

                if (isHost) {
                    wroupClient.sendMessageToAllClients(normalMessage);
                } else {
                    wroupService.sendMessageToAllClients(normalMessage);
                }

                if (which == 0) {
                    sendPhoneBookAndPhotos();
                }


            });

            pickOptionDialog = builder.create();
            pickOptionDialog.show();
        });

    }

    @Override
    public void onClientDisconnected(WroupDevice wroupDevice) {
        Log.d(TAG, "onClientDisconnected: ");
        runOnUiThread(() -> {
//            disconnect();
//            startSearchingProcess();
        });

    }

    @Override
    public void onDataReceived(MessageWrapper messageWrapper) {
        runOnUiThread(() -> {
            if (messageWrapper.getMessage().equals(source) || messageWrapper.getMessage().equals(target)) {
                if (pickOptionDialog.isShowing()) {
                    pickOptionDialog.dismiss();
                }
                progressBar.setVisibility(View.VISIBLE);

                if (messageWrapper.getMessage().equals(source)) {
                    sendPhoneBookAndPhotos();
                }
            } else {

                try {
                    JSONObject jsonObject = new JSONObject(messageWrapper.getMessage());
                    JSONArray jsonArray = jsonObject.getJSONArray("contact_list");
                    List<Contact> contactList = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        contactList.add(new Contact(jsonArray.getJSONObject(i)));


                    }


                    contactsRv.setAdapter(new ContactListAdapter(contactList));
                    progressBar.setVisibility(View.GONE);
                    availableDevicesLv.setVisibility(View.GONE);

                } catch (JSONException e) {
                    e.printStackTrace();
                }


//                disconnect();
//                startSearchingProcess();
            }

        });

    }

    private void sendPhoneBookAndPhotos() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 1001);
                return;
            }
        }
        getContactList(jsonArray -> {

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("contact_list", jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            MessageWrapper normalMessage = new MessageWrapper();
            normalMessage.setMessageType(MessageWrapper.MessageType.NORMAL);
            normalMessage.setMessage(jsonObject.toString());
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                availableDevicesLv.setVisibility(View.GONE);
            });

            if (isHost) {
                wroupClient.sendMessageToAllClients(normalMessage);
            } else {
                wroupService.sendMessageToAllClients(normalMessage);
            }
        });


    }


    private void getContactList(CompletionHandler completionHandler) {
        new Thread(() -> {
            ContentResolver cr = getContentResolver();
            JSONArray jsonArray = new JSONArray();
            Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            if (cursor != null) {
                HashSet<String> mobileNoSet = new HashSet<>();
                final int idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID);

                final int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                final int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String name, number, image = "";
                while (cursor.moveToNext()) {
                    try {
                        try {
                            image = "";
                            image = convertImageToBase64(getPhotoUri(cursor.getString(idIndex)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        name = cursor.getString(nameIndex);
                        number = cursor.getString(numberIndex);
                        number = number.replace(" ", "");
                        if (!mobileNoSet.contains(number)) {
                            jsonArray.put(new Contact(name, number, image).toJsonObject());

                            mobileNoSet.add(number);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                cursor.close();

            }
            if (completionHandler != null) {
                completionHandler.onComplete(jsonArray);
            }

        }).start();


    }


    public String convertImageToBase64(Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

        if (bitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); // bm is the bitmap object
            byte[] b = baos.toByteArray();
            return Base64.encodeToString(b, Base64.DEFAULT);
        } else {
            return "";
        }

    }


    public Uri getPhotoUri(String id) {
        try {
            Cursor cur = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, ContactsContract.Data.CONTACT_ID + "=" + id + " AND " + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null,
                    null);
            if (cur != null) {
                if (!cur.moveToFirst()) {
                    return null; // no photo
                }
            } else {
                return null; // error in cursor process
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(id));
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }


}