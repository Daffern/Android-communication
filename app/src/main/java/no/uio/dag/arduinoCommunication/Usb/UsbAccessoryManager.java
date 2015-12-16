package no.uio.dag.arduinoCommunication.Usb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import no.uio.dag.arduinoCommunication.Util.MessageListener;
import no.uio.dag.arduinoCommunication.Util.MyMessage;


public class UsbAccessoryManager implements Runnable{

    private final String TAG="UsbWrapper";

    private UsbManager usbManager;
    private UsbAccessory accessory;
    private PendingIntent mPermissionIntent;

    ParcelFileDescriptor mFD;

    private static final String ACTION_USB_PERMISSION =
            "no.uio.dag.arduinoCommunication.USB_PERMISSION";

    private FileInputStream inStream;
    private FileOutputStream outStream;

    private static final int MESSAGE_IN=1;
    private static final int MESSAGE_OUT=2;

    private MessageListener listener;


    private Activity arduinoCommunication;

    public UsbAccessoryManager(Activity arduinoCommunication){
        this.arduinoCommunication = arduinoCommunication;

        //register receiver
        usbManager = (UsbManager) arduinoCommunication.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(arduinoCommunication, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        arduinoCommunication.registerReceiver(mUsbReceiver, filter);
    }



    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory newAccessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(newAccessory != null){
                            openAccessory(newAccessory);
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for accessory " + accessory);
                    }
                }
            }
            else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory newAccessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (newAccessory != null && newAccessory.equals(accessory))
                    closeAccessory();
            }
        }
    };

    public UsbAccessory getAccessory(){
        if (accessory == null){
            if (usbManager.getAccessoryList() != null && usbManager.getAccessoryList().length > 0){
                return usbManager.getAccessoryList()[0];
            }

        }

        return accessory;
    }

    private boolean openAccessory(UsbAccessory newAccessory){
        mFD = usbManager.openAccessory(newAccessory);
        if (mFD != null) {
            accessory = newAccessory;
            FileDescriptor fd = mFD.getFileDescriptor();

            inStream = new FileInputStream(fd);  // use this to receive messages
            outStream = new FileOutputStream(fd); // use this to send commands

            Thread thread = new Thread(null, this, "AccessoryThread");
            thread.start();

            return true;
        }
        else return false;
    }

    private void closeAccessory(){
        try{
            if (mFD != null){
                mFD.close();
            }
        }catch(IOException e){

        }
        finally {
            mFD = null;
            accessory = null;
        }
    }

    public void run(){
        int returned = 0;
        byte[] buffer = new byte[16384];


        while (returned >= 0){
            try{
                returned = inStream.read(buffer);
            }catch(IOException e){
                break;
            }
            if ( returned > 0 ) {
                String receiveString = new String(buffer, 0, returned, Charset.forName("UTF-8"));
                Message m = Message.obtain(mHandler, MESSAGE_IN);
                m.obj = new MyMessage(receiveString);
                mHandler.sendMessage(m);
            }
        }
    }


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_IN:

                    listener.onReceive((byte[])msg.obj);

                    break;

                case MESSAGE_OUT:
                    try {
                        MyMessage myMessage = (MyMessage)msg.obj;
                        byte[] b = myMessage.getMessage().getBytes();
                        outStream.write(b);
                        outStream.flush();
                    }
                    catch(Exception e){
                        Log.e(TAG,"Exception in message");
                    }

                    break;

                default:/*
                    try {
                        byte[] v = (byte[]) msg.obj;
                        mOutputStream.write(v);
                        mOutputStream.flush();
                    } catch (IOException e) {
                        Log.w("AccessoryController", "Error writing vibe output");
                    }*/
                    break;
            }
        }
    };



    public void setMessageListener(MessageListener messageListener) {
        this.listener = messageListener;
    }


    public void sendMessage(MyMessage myMessage) {
        Message m = Message.obtain(null, MESSAGE_OUT);
        m.obj = myMessage;
        mHandler.sendMessage(m);

    }
}
