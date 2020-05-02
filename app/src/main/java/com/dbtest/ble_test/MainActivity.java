package com.dbtest.ble_test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothGattServer bluetoothGattServer;

    public static final String UUID_SERVICE = "00001803-0000-1000-8000-00805F9B34FB";
    public static final String UUID_CHARACTERISTIC = "00002A06-0000-1000-8000-00805F9B34FB";
    public static final String UUID_CHARACTERISTIC_CONFIG = "00001101-0000-1000-8000-00805F9B34FD";

    public static final String PUSH_BUTTON_SERVICE_UUID = "00009905-0000-1000-8000-00805f9b34fb";
    public static final String PUSH_BUTTON__SERVICE_CHARACTERISTIC_UUID = "00009906-0000-1000-8000-00805f9b34fb";
    public static final String PUSH_BUTTON_SERVICE_CHARACTERISTICS_CONFIG_UUID = "00009907-0000-1000-8000-00805f9b34fb";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            System.out.println("系统不支持蓝牙低功耗通信");
            return;
        } else {
            System.out.println("1：系统支持");
        }

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null){
            System.out.println("该设备不支持蓝牙低功耗通信");
            this.finish();
            return;
        } else {
            System.out.println("2: 系统支持");
        }

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        if (bluetoothLeAdvertiser == null) {
            System.out.println("该设备不支持蓝牙低功耗从设备通信");
            this.finish();
            return;
        } else {
            System.out.println("3：系统支持");
        }
    }

    public void onclick(View view){
        //广播设置
        AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();
        settingBuilder.setConnectable(true);
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingBuilder.setTimeout(0);
        settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        AdvertiseSettings settings = settingBuilder.build();

        //广播参数
        AdvertiseData.Builder dateBuilder = new AdvertiseData.Builder();
        bluetoothAdapter.setName("遥控器配件-01");
        dateBuilder.setIncludeDeviceName(true);
        dateBuilder.setIncludeTxPowerLevel(true);

        dateBuilder.addServiceUuid(ParcelUuid.fromString(UUID_SERVICE));
 //       dateBuilder.addServiceUuid(ParcelUuid.fromString(PUSH_BUTTON_SERVICE_UUID));

        AdvertiseData data = dateBuilder.build();

        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);

        Log.e("TAG", "onclick: 从设备开始广播", null);
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("1.1 AdvertiseCallback-onStartSuccess");
                }
            });

            bluetoothGattServer = bluetoothManager.openGattServer(getApplicationContext(), bluetoothGattServerCallback);

            BluetoothGattService service = new BluetoothGattService(UUID.fromString(UUID_SERVICE), BluetoothGattService.SERVICE_TYPE_PRIMARY);

            UUID UUID_CHARREAD = UUID.fromString(UUID_CHARACTERISTIC);

            //特征值写设置
            BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHARREAD, BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY , BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);

            UUID UUID_DESCRIPTOR = UUID.fromString(UUID_CHARACTERISTIC_CONFIG);

            BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);
            characteristicWrite.addDescriptor(descriptor);
            service.addCharacteristic(characteristicWrite);

            bluetoothGattServer.addService(service);
            //addService方法不能并列执行，需要等一个Service的onServiceAdded方法回调后才可添加下一个Service。此处根据UUID判断一个服务是否添加成功。
            while(bluetoothGattServer.getService(UUID.fromString(UUID_SERVICE)) == null) ;

            //警报特征值读写操作
            BluetoothGattService service1 = new BluetoothGattService(UUID.fromString(PUSH_BUTTON_SERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic characteristicPush = new BluetoothGattCharacteristic(UUID.fromString(PUSH_BUTTON__SERVICE_CHARACTERISTIC_UUID), BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
            BluetoothGattDescriptor descriptorPush = new BluetoothGattDescriptor(UUID.fromString(PUSH_BUTTON_SERVICE_CHARACTERISTICS_CONFIG_UUID), BluetoothGattDescriptor.PERMISSION_WRITE);
            characteristicPush.addDescriptor(descriptorPush);
            service1.addCharacteristic(characteristicPush);

            bluetoothGattServer.addService(service1);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("1.2. Service Builded Ok");
                }
            });
        }
    };

    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);

            final String info = service.getUuid().toString();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("1.3 BluetoothGattServerCallback-onServiceAdded" + info);
                }
            });
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            final String info = device.getAddress() + "|" + status + "->" + newState;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("1.4 onConnectionStateChange " + info);
                }
            });
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            final String deviceInfo = "Address:" + device.getAddress();
            final String info = "Request:" + requestId + "|Offset:" + offset + "|characteristic:" + characteristic.getUuid();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("=====================================");
                    System.out.println("设备信息" + deviceInfo);
                    System.out.println("数据信息" + info);
                    System.out.println("===onCharacteristicReadRequest=======");
                }
            });
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{50});
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, final BluetoothGattCharacteristic characteristic, boolean prepareWrite, boolean responseNeeded, final int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, prepareWrite, responseNeeded, offset, value);

            Log.e("TAG", "onCharacteristicWriteRequest: hs", null);

            String valueString = new String(value, US_ASCII);

            System.out.println(valueString);

            Log.e("TAG", "onCharacteristicWriteRequest: " + valueString + "else = " + value[0] , null);

            final String deviceInfo = "||Name:" + device.getName() + "\n||Address:" + device.getAddress();
            final String info = "||RequestID:" + requestId + "\n||Offset:" + offset + "\n||characteristic:" + characteristic.getUuid()+"\n||value：" + valueString;

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showInfo("====onCharacteristicWriteRequest====\n"+
                                    "-设备信息-\n" + deviceInfo + "\n\n" +
                                    "-数据信息-\n" + info + "\n\n" +
                                    "===============================");
                }
            });
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);

            final String info = "Address:" + device.getAddress() + "|status:" + status;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("onNotificationSent " + info);
                }
            });
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean prepareWrite, boolean responseNeeded, int offset, byte[] value) {
            final String deviceInfo = "Name:" + device.getAddress() + "|Address:" + device.getAddress();
            final String info = "Request:" + requestId + "|Offset:" + offset + "|characteristic:" + descriptor.getUuid();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("===================================");
                    System.out.println("设备信息" + deviceInfo);
                    System.out.println("数据信息" + info);
                    System.out.println("====onDescriptorWriteRequest====");
                }
            });
            //告诉连接设备坐好了
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            final String deviceInfo = "Name:" + device.getAddress() + "|Address:" + device.getAddress();
            final String info = "Request:" + requestId + "|Offset:" + offset + "|characteristic:" + descriptor.getUuid();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("===================================");
                    System.out.println("设备信息" + deviceInfo);
                    System.out.println("数据信息" + info);
                    System.out.println("====onDescriptorReadRequest====");
                }
            });
            //告诉连接设备坐好了
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

    };

    private void showInfo(String info) {
        ((TextView)findViewById(R.id.now_info)).setText(info);
    }
}
