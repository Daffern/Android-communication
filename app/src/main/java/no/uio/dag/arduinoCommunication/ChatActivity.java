package no.uio.dag.arduinoCommunication;


import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


import com.hoho.android.usbserial.driver.UsbSerialPort;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import no.uio.dag.R;
import no.uio.dag.arduinoCommunication.Chat.ChatAdapter;


import no.uio.dag.arduinoCommunication.Chat.OneComment;
import no.uio.dag.arduinoCommunication.Usb.UsbHostManager;
import no.uio.dag.arduinoCommunication.Util.MessageListener;


public class ChatActivity extends AppCompatActivity {

    private final static String TAG ="ArduinoCommunication";
    private static final String PORTNUMBER="portnumber";



    private ChatAdapter adapter;
    private ListView listView;
    private EditText editText;
    private Button button;

    //HwCustTelephonyProvider

    UsbHostManager usbHostManager;

    boolean connected=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int portnumber = getIntent().getIntExtra(PORTNUMBER, 0);

        setContentView(R.layout.activity_discuss);

        usbHostManager = new UsbHostManager(this);



        if (usbHostManager.startService(portnumber)){

            usbHostManager.setMessageListener(new MessageListener() {
                @Override
                public void onReceive(final byte[] data) {

                    ChatActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ChatActivity.this.receiveMessage(data);
                        }
                    });

                }

                @Override
                public void onRunError(Exception e) {

                    //Toast.makeText(ChatActivity.this, "Disconnected from Arduino", Toast.LENGTH_LONG);

                    Log.e(TAG, "error received: " + e.getMessage());

                    ChatActivity.this.finish();//close the app if there is an error

                }


            });

        }else{
            Log.i(TAG, "usb device not connected");
        }


        initChat();
    }


    private void initChat(){
        adapter = new ChatAdapter(this, R.id.form);
        listView = (ListView)findViewById(R.id.chatList);
        listView.setAdapter(adapter);
        listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setStackFromBottom(true);

        button = (Button)findViewById(R.id.submitButton);
        editText = (EditText)findViewById(R.id.editText);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                byte[] data = editText.getText().toString().getBytes();
                usbHostManager.sendMessage(data);

                adapter.add(new OneComment(true, new String(data)));
                editText.getText().clear();
            }
        });
    }






    public void receiveMessage(byte[] data){
        adapter.add(new OneComment(false, new String(data)));
    }



    @Override
    public void onPause(){
        super.onPause();
    }
    @Override
    public void onResume(){
        super.onResume();
    }



    static void show(Context context, int portnumber) {
        final Intent intent = new Intent(context, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra(PORTNUMBER, portnumber);
        context.startActivity(intent);
    }
}
