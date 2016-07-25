package com.accent_systems.ibkssdksampleproject;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


import com.accent_systems.ibks_sdk.EDSTService.ASEDSTCallback;
import com.accent_systems.ibks_sdk.EDSTService.ASEDSTDefs;
import com.accent_systems.ibks_sdk.EDSTService.ASEDSTService;
import com.accent_systems.ibks_sdk.EDSTService.ASEDSTSlot;
import com.accent_systems.ibks_sdk.GlobalService.ASGlobalCallback;
import com.accent_systems.ibks_sdk.GlobalService.ASGlobalDefs;
import com.accent_systems.ibks_sdk.GlobalService.ASGlobalService;
import com.accent_systems.ibks_sdk.connections.ASConDevice;
import com.accent_systems.ibks_sdk.connections.ASConDeviceCallback;
import com.accent_systems.ibks_sdk.iBeaconService.ASiBeaconCallback;
import com.accent_systems.ibks_sdk.iBeaconService.ASiBeaconService;
import com.accent_systems.ibks_sdk.iBeaconService.ASiBeaconSlot;
import com.accent_systems.ibks_sdk.scanner.ASBleScanner;
import com.accent_systems.ibks_sdk.scanner.ASResultParser;
import com.accent_systems.ibks_sdk.scanner.ASScannerCallback;
import com.accent_systems.ibks_sdk.utils.ASUtils;
import com.accent_systems.ibks_sdk.utils.AuthorizedServiceTask;
import com.google.android.gms.common.AccountPicker;
import com.google.sample.libproximitybeacon.ProximityBeacon;
import com.google.sample.libproximitybeacon.ProximityBeaconImpl;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ASScannerCallback,ASConDeviceCallback,ASEDSTCallback, ASiBeaconCallback, ASGlobalCallback {

    public static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    public static final int SCOPE_USERLOCATION    =  0;
    public static final int SCOPE_CLOUDPLATFORM   =  1;

    private List<String> scannedDeivcesList;
    private ArrayAdapter<String> adapter;

    BluetoothGattCharacteristic myCharRead;
    BluetoothGattCharacteristic myCharWrite;
    //DEFINE LAYOUT
    ListView devicesList;

    public static ProximityBeacon client;
    SharedPreferences getPrefs;
    public static Activity actv;

    static ProgressDialog connDialog;
    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actv = this;
        //Define listview in layout
        devicesList = (ListView) findViewById(R.id.devicesList);
        //Setup list on device click listener
        setupListClickListener();

        //Inicialize de devices list
        scannedDeivcesList = new ArrayList<>();

        //Inicialize the list adapter for the listview with params: Context / Layout file / TextView ID in layout file / Devices list
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, scannedDeivcesList);

        //Set the adapter to the listview
        devicesList.setAdapter(adapter);

        connDialog = new ProgressDialog(MainActivity.this);
        connDialog.setTitle("Please wait...");

        //  checkBlePermissions();
        startScan();
        getPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if(getPrefs.getString("clientName", null)!=null){

            try{
                client = new ProximityBeaconImpl(MainActivity.this, getPrefs.getString("clientName", null));
                new AuthorizedServiceTask(MainActivity.this, getPrefs.getString("clientName", null),SCOPE_USERLOCATION).execute();
                new AuthorizedServiceTask(MainActivity.this, getPrefs.getString("clientName", null),SCOPE_CLOUDPLATFORM).execute();
                getProjectList();

            } catch (final Exception ee) {
                Log.i(TAG,"CLIENT ERROR: " + ee.toString());
                pickUserAccount();
            }
        }else{
            pickUserAccount();
        }
    }

    private void pickUserAccount() {
        Log.i(TAG, "PICK USER - CALLED");
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(
                null, null, accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == 1 && resultCode == Activity.RESULT_CANCELED) {
            return;
        }else{
            if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
                if (resultCode == Activity.RESULT_OK) {
                    String name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    try{
                        client = new ProximityBeaconImpl(MainActivity.this, name);

                        new AuthorizedServiceTask(MainActivity.this, name,SCOPE_USERLOCATION).execute();
                        new AuthorizedServiceTask(MainActivity.this, name,SCOPE_CLOUDPLATFORM).execute();
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("clientName", name).commit();
                        getProjectList();
                    } catch (final Exception ee) {
                        Log.i(TAG,"CLIENT ERROR: "+ ee.toString());
                    }

                }else{
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static void getProjectList()
    {
        Callback listProjectsCallback = new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.d(TAG, "listProjectsCallback - Failed request: " + request, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String body = response.body().string();
                if (response.isSuccessful()) {

                    try {
                        JSONObject json = new JSONObject(body);
                        final JSONArray projects = json.getJSONArray("projects");
                        final int numprojects = projects.length();
                        final String [] items = new String[numprojects+1];
                        for(int i=0;i<numprojects;i++)
                        {
                            items[i] =  projects.getJSONObject(i).getString("name");
                        }
                        items[numprojects] = "None";
                        AlertDialog.Builder builder=new AlertDialog.Builder(actv);
                        builder.setTitle("Select a project");
                        builder.setCancelable(false);
                        builder.setItems(items, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {

                                    if(which == numprojects)
                                    {
                                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectId", "null").commit();
                                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectName", "null").commit();
                                    }
                                    else {
                                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectId", projects.getJSONObject(which).getString("projectId")).commit();
                                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectName", projects.getJSONObject(which).getString("name")).commit();
                                        String projectId = PreferenceManager.getDefaultSharedPreferences(actv).getString("projectId", "null");
                                        String projectName = PreferenceManager.getDefaultSharedPreferences(actv).getString("projectName", "null");
                                        Log.i(TAG,"Project selected: "+ projectId);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "listProjectsCallback - JSONException", e);
                                }
                            }
                        });

                        builder.show();

                    } catch (JSONException e) {
                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectId", "null").commit();
                        PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectName", "null").commit();
                        Log.i(TAG, "listProjectsCallback - This account has no an associated project");
                    }
                } else {
                    PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectId", "null").commit();
                    PreferenceManager.getDefaultSharedPreferences(actv).edit().putString("projectName", "null").commit();
                    Log.d(TAG, "Unsuccessful project list request: " + body);
                }
            }
        };

        client.getProjectList(listProjectsCallback);
    }

    void setupListClickListener(){
        devicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Stop the scan
                Log.i(TAG, "SCAN STOPED");
                ASBleScanner.stopScan();

                //Get the string from the item clicked
                String fullString = scannedDeivcesList.get(position);
                //Get only the address from the previous string. Substring from '(' to ')'
                String address = fullString.substring(fullString.indexOf("(")+1, fullString.indexOf(")"));

                Log.i(TAG,"*************************************************");
                Log.i(TAG, "CONNECTION STARTED TO DEVICE "+address);
                Log.i(TAG,"*************************************************");

                BluetoothAdapter mBluetoothAdapter = ASBleScanner.getmBluetoothAdapter();
                if(mBluetoothAdapter != null) {
                    ASConDevice mcondevice;
                    mcondevice =  new ASConDevice(MainActivity.this, mBluetoothAdapter, MainActivity.this);
                    new ASEDSTService(mcondevice,MainActivity.this,10);
                    new ASiBeaconService(mcondevice,MainActivity.this,10);
                    new ASGlobalService(mcondevice,MainActivity.this,10);

                    //show dialog to notify the user that the app is working
                    connDialog.setMessage("Showing info on Android Monitor!");
                    connDialog.show();

                    ASConDevice.connectDevice(address);

                } else{
                    Log.i(TAG,"BLE not enabled/supported!");
                }
            }
        });
    }

    private void startScan(){
        int err;
        new ASBleScanner(this, this).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        err = ASBleScanner.startScan();
        if(err != ASUtils.TASK_OK) {
            Log.i(TAG, "startScan - Error (" + Integer.toString(err) + ")");

            if(err == ASUtils.ERROR_LOCATION_PERMISSION_NOT_GRANTED){
                requestLocationPermissions();
            }
        }
    }

    @TargetApi(23)
    public void requestLocationPermissions(){
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "LOCATION PERMISSION GRANTED");
                    startScan();
                } else {
                    Log.i(TAG, "LOCATION PERMISSION NOT GRANTED");
                }
                return;
            }
        }
    }

    @Override
    public void scannedBleDevices(ScanResult result){

        String advertisingString = ASResultParser.byteArrayToHex(result.getScanRecord().getBytes());

        String logstr = result.getDevice().getAddress()+" / RSSI: "+result.getRssi()+" / Adv packet: "+advertisingString;
        //Check if scanned device is already in the list by mac address
        boolean contains = false;
        for(int i=0; i<scannedDeivcesList.size(); i++){
            if(scannedDeivcesList.get(i).contains(result.getDevice().getAddress())){
                //Device already added
                contains = true;
                //Replace the device with updated values in that position
                scannedDeivcesList.set(i, result.getRssi()+"  "+result.getDevice().getName()+ "\n       ("+result.getDevice().getAddress()+")");
                break;
            }
        }

        if(!contains){
            //Scanned device not found in the list. NEW => add to list
            scannedDeivcesList.add(result.getRssi()+"  "+result.getDevice().getName()+ "\n       ("+result.getDevice().getAddress()+")");
        }

        //After modify the list, notify the adapter that changes have been made so it updates the UI.
        //UI changes must be done in the main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
        JSONObject advData;
        switch (ASResultParser.getAdvertisingType(result)){
            case ASUtils.TYPE_IBEACON:
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" AdvTxPower = "+advData.getString("AdvTxPower")+" UUID = "+advData.getString("UUID")+" Major = "+advData.getString("Major")+" Minor = "+advData.getString("Minor"));
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - iBEACON - "+logstr);
                break;
            case ASUtils.TYPE_EDDYSTONE_UID:
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" AdvTxPower = "+advData.getString("AdvTxPower")+" Namespace = "+advData.getString("Namespace")+" Instance = "+advData.getString("Instance"));
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - UID - "+logstr);
                break;
            case ASUtils.TYPE_EDDYSTONE_URL:
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+"  AdvTxPower = "+advData.getString("AdvTxPower")+" Url = "+advData.getString("Url"));
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - URL - "+logstr);

                break;
            case ASUtils.TYPE_EDDYSTONE_TLM:
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 if(advData.getString("Version").equals("0")){
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" Version = "+advData.getString("Version")+" Vbatt = "+advData.getString("Vbatt")+" Temp = "+advData.getString("Temp")+" AdvCount = "+advData.getString("AdvCount")+" TimeUp = "+advData.getString("TimeUp"));
                 }
                 else{
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" Version = "+advData.getString("Version")+" EncryptedTLMData = "+advData.getString("EncryptedTLMData")+" Salt = "+advData.getString("Salt")+" IntegrityCheck = "+advData.getString("IntegrityCheck"));
                 }
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - TLM - "+logstr);
                break;
            case ASUtils.TYPE_EDDYSTONE_EID:
                /**** Example to get EID in Clear by the air ***
                 if(!readingEID) {
                 readingEID = true;
                 new ASEDSTService(null,this,10);
                 ASEDSTService.setClient_ProjectId(client, getPrefs.getString("projectId", null));
                 ASEDSTService.getEIDInClearByTheAir(result);
                 }
                 /**************************************************/
                /**** Example to get data from advertising ***
                 advData = ASResultParser.getDataFromAdvertising(result);
                 try {
                 Log.i(TAG, "FrameType = " +advData.getString("FrameType")+" AdvTxPower = "+advData.getString("AdvTxPower")+" EID = "+advData.getString("EID"));
                 }catch (Exception ex){
                 Log.i(TAG,"Error parsing JSON");
                 }
                 /*******************************************/
                Log.i(TAG,result.getDevice().getName()+" - EID - "+logstr);
                break;
            case ASUtils.TYPE_DEVICE_CONNECTABLE:
                Log.i(TAG,result.getDevice().getName()+" - CONNECTABLE - "+logstr);
                break;
            case ASUtils.TYPE_UNKNOWN:
                Log.i(TAG,result.getDevice().getName()+" - UNKNOWN - "+logstr);
                break;
            default:
                Log.i(TAG,"ADVERTISING TYPE: "+ "ERROR PARSING");
                break;
        }
    }

    @Override
    //implementation of ASConDeviceCallback
    public void onChangeStatusConnection(int result, BluetoothGatt blgatt){
        switch (result){
            case ASUtils.GATT_DEV_CONNECTED:
                Log.i(TAG,"onChangeStatusConnection - DEVICE CONNECTED: "+blgatt.getDevice().getName());
                break;
            case ASUtils.GATT_DEV_DISCONNECTED:
                Log.i(TAG,"onChangeStatusConnection - DEVICE DISCONNECTED: "+blgatt.getDevice().getName());
                if (connDialog != null && connDialog.isShowing()) {
                    connDialog.dismiss();
                }
                break;
            default:
                Log.i(TAG,"onChangeStatusConnection - ERROR PARSING");
                break;
        }

    }

    //implementation of ASConDeviceCallback
    public void onServicesCharDiscovered(int result, BluetoothGatt blgatt, ArrayList<BluetoothGattService> services, ArrayList<BluetoothGattCharacteristic> characteristics)
    {
        switch (result){
            case ASUtils.GATT_SERV_DISCOVERED_OK:
                int err;
                Log.i(TAG, "onServicesCharDiscovered - SERVICES DISCOVERED OK: "+blgatt.getDevice().getName());

                /**** Example to read a characteristic ***
                 myCharRead = ASConDevice.findCharacteristic("00002a28");
                 ASConDevice.readCharacteristic(myCharRead);
                 /*****************************************/


                /**** Example to set Eddystone Slots a characteristic ***

                 ASEDSTSlot[] slots = new ASEDSTSlot[4];
                 ASEDSTService.setClient_ProjectId(client,getPrefs.getString("projectId", null));

                 slots[0] = new ASEDSTSlot(ASEDSTDefs.FT_EDDYSTONE_UID,800,-4,-35,"0102030405060708090a0b0c0d0e0f11");
                 slots[1] = new ASEDSTSlot(ASEDSTDefs.FT_EDDYSTONE_EID,950,-4,-35,"1112131415161718191a1b1c1d1e1f200a");
                 slots[2] = new ASEDSTSlot(ASEDSTDefs.FT_EDDYSTONE_URL,650,0,-21,"http://goo.gl/yb6Mgt");
                 slots[3] = new ASEDSTSlot(ASEDSTDefs.FT_EDDYSTONE_TLM,60000,4,-17,null);
                 ASEDSTService.setEDSTSlots(slots);
                 /********************************************************/

                /**** Example to set iBeacon Slots a characteristic ***
                 ASiBeaconSlot[] slotsib = new ASiBeaconSlot[2];
                 slotsib[0] = new ASiBeaconSlot(false,800,0,-21,"01010101010101010101010101010101","0002","0003",false);
                 slotsib[1] = new ASiBeaconSlot(false,400,-8,-40,"01010101010101010101010101010102","0004","0005",true);
                 ASiBeaconService.setiBeaconSlots(slotsib);
                 /******************************************************/


                /*** Example to get EDST or iBeacon Slots ***/
               // ASEDSTService.setClient_ProjectId(client,getPrefs.getString("projectId", null));
                ASEDSTService.getEDSTSlots();
                //ASiBeaconService.getiBeaconSlots();
                /********************************************/

                /*** Example to set Characteristics ***
                 ASEDSTService.setActiveSlot(2);
                 //ASEDSTService.setRadioTxPower(-4);
                 //ASiBeaconService.setExtraByte(true);
                 //ASiBeaconService.setUUIDMajorMinor(false,"0102030405060708090a0b0c0d0e0f10","0001","0002");
                 //ASGlobalService.setONOFFAdvertising(9,22);
                 //ASGlobalService.setDeviceName("iBKS-TEST");
                 /*****************************************************/

                /*** Example to get Characteristics ***
                 ASEDSTService.getLockState();
                 //ASiBeaconService.getActiveSlot();
                 //ASGlobalService.getDeviceName();
                 /*****************************************************/

                break;
            case ASUtils.GATT_SERV_DISCOVERED_ERROR:
                Log.i(TAG, "onServicesCharDiscovered - SERVICES DISCOVERED ERROR: "+blgatt.getDevice().getName());
                break;
            default:
                Log.i(TAG, "onServicesCharDiscovered - ERROR PARSING");
                break;
        }
    }

    //implementation of ASConDeviceCallback
    public void onReadDeviceValues(int result, BluetoothGattCharacteristic characteristic, String value){
        switch (result){
            case ASUtils.GATT_READ_SUCCESSFULL:
                Log.i(TAG, "onReadDeviceValues - READ VALUE: " + value);
                break;
            case ASUtils.GATT_READ_ERROR:
                Log.i(TAG, "onReadDeviceValues - READ ERROR");
                break;
            case ASUtils.GATT_NOTIFICATION_RCV:
                Log.i(TAG, "onReadDeviceValues - READ NOTIFICATION: " + value);
                break;
            case ASUtils.GATT_RSSI_OK:
                Log.i(TAG, "onReadDeviceValues - READ RSSI: " + value);
                break;
            case ASUtils.GATT_RSSI_ERROR:
                Log.i(TAG, "onReadDeviceValues - READ RSSI ERROR");
                break;
            default:
                Log.i(TAG, "onReadDeviceValues - ERROR PARSING");
                break;
        }
    }

    //implementation of ASConDeviceCallback
    public void onWriteDeviceChar(int result, BluetoothGattCharacteristic characteristic) {
        switch (result) {
            case ASUtils.GATT_WRITE_SUCCESSFULL:
                Log.i(TAG, "onWriteDeviceChar - WRITE SUCCESSFULL on: " + characteristic.getUuid().toString() );
                break;
            case ASUtils.GATT_WRITE_ERROR:
                Log.i(TAG, "onWriteDeviceChar - WRITE ERROR on: " + characteristic.getUuid().toString() );
                break;
            default:
                Log.i(TAG, "onWriteDeviceChar - ERROR PARSING");
                break;
        }
    }

    //implementation of ASEDSTCallback
    public void onReadEDSTCharacteristic(int result, BluetoothGattCharacteristic characteristic, byte[] readval)
    {
        Log.i(TAG,"onReadEDSTCharacteristic - result = " + result + " characteristic = " + characteristic.getUuid() +" readval = " + ASResultParser.byteArrayToHex(readval));
    }
    //implementation of ASEDSTCallback
    public void onWriteEDSTCharacteristic(int result, BluetoothGattCharacteristic characteristic)
    {
        Log.i(TAG,"onWriteEDSTCharacteristic - result = " + result /*+ " characteristic = " + characteristic.getUuid()*/ );
    }
    //implementation of ASEDSTCallback
    public void onEDSTSlotsWrite(int result)
    {
        if(result == ASUtils.WRITE_OK) {
            Log.i(TAG, "onEDSTSlotsWrite - Write OK!");
        }
        else
            Log.i(TAG,"onEDSTSlotsWrite - Error (" + Integer.toString(result) + ")");

    }
    //implementation of ASEDSTCallback
    public void onGetEDSTSlots(int result, ASEDSTSlot[] slots){
        if(result == ASUtils.READ_OK)
        {
            /**** Reading EID In Clear (if there's a slot configured as EID) ****
            for(int i=0;i<slots.length;i++) {
                if(slots[i].frame_type == ASEDSTDefs.FT_EDDYSTONE_EID) {
                    ASEDSTService.setClient_ProjectId(client, getPrefs.getString("projectId", null));
                    ASEDSTService.getEIDInClear(i);
                }
            }
            /********************************************************************/
            for(int i=0;i<slots.length;i++){
                Log.i(TAG,"onGetEDSTSlots - slot "+i+" advint = "+ Integer.toString(slots[i].adv_int)+ " txpower = "+ slots[i].tx_power + " advtxpower = "+ slots[i].adv_tx_power +" frame type = 0x"+ Integer.toHexString(slots[i].frame_type)+" data = "+ slots[i].data );
            }

        }
        else
            Log.i(TAG,"onGetEDSTSlots - Error (" + Integer.toString(result) + ")");

        //Close dialog
        if (connDialog != null && connDialog.isShowing()) {
            connDialog.dismiss();
        }
    }
    //implementation of ASEDSTCallback
    public void onGetEIDInClear(int result, String EID, String msg){
        if(result == ASUtils.READ_OK) {
            Log.i(TAG, "onGetEIDInClear - EID read OK = "+ EID);
        }
        else
            Log.i(TAG,"onGetEIDInClear - Error reading EID (" + Integer.toString(result) + "): "+ msg);

    }


    //implementation of ASiBeaconCallback
    public void onReadiBeaconCharacteristic(int result, BluetoothGattCharacteristic characteristic, byte[] readval)
    {
        Log.i(TAG,"onReadiBeaconCharacteristic - result = " + result/* + " characteristic = " + characteristic.getUuid() +" readval = " + ASResultParser.byteArrayToHex(readval)*/);
    }
    public void onWriteiBeaconCharacteristic(int result, BluetoothGattCharacteristic characteristic)
    {
        Log.i(TAG,"onWriteiBeaconCharacteristic - result = " + result /*+ " characteristic = " + characteristic.getUuid()*/ );
    }

    //implementation of ASiBeaconCallback
    public void oniBeaconSlotsWrite(int result)
    {
        if(result == ASUtils.WRITE_OK) {
            Log.i(TAG, "oniBeaconSlotsWrite - Write OK!");
        }
        else
            Log.i(TAG,"oniBeaconSlotsWrite - Error (" + Integer.toString(result) + ")");

    }
    //implementation of ASiBeaconCallback
    public void onGetiBeaconSlots(int result, ASiBeaconSlot[] slots){
        if(result == ASUtils.READ_OK)
        {
            for(int i=0;i<slots.length;i++){
                Log.i(TAG,"onGetiBeaconSlots - slot "+i+" clear slot = "+ slots[i].clearslot+" advint = "+ Integer.toString(slots[i].adv_int)+" txpower = "+ slots[i].tx_power+" advtxpower = "+ slots[i].adv_tx_power+" uuid = "+ slots[i].UUID+" major = "+ slots[i].Major+" minor = "+ slots[i].Minor+" extra byte = "+ slots[i].ExtraByte);
            }
        }
        else
            Log.i(TAG,"onGetiBeaconSlots - Error (" + Integer.toString(result) + ")");
    }

    //implementation of ASGlobalCallback
    public void onReadGlobalCharacteristic(int result, BluetoothGattCharacteristic characteristic, byte[] readval)
    {
        if(result == ASUtils.READ_OK) {
            Log.i(TAG, "onReadGlobalCharacteristic - read OK!");
            if(characteristic.getUuid().toString().contains(ASGlobalDefs.DEVICE_NAME)){
                Log.i(TAG,"Device Name is "+ASResultParser.StringHexToAscii(ASResultParser.byteArrayToHex(readval)));
            }
        }
        else
            Log.i(TAG,"onReadGlobalCharacteristic - Error (" + Integer.toString(result) + ")");
    }
    //implementation of ASGlobalCallback
    public void onWriteGlobalCharacteristic(int result, BluetoothGattCharacteristic characteristic)
    {
        if(result == ASUtils.WRITE_OK) {
            Log.i(TAG, "onWriteGlobalCharacteristic - Write OK!");
        }
        else
            Log.i(TAG,"onWriteGlobalCharacteristic - Error (" + Integer.toString(result) + ")");
    }

}