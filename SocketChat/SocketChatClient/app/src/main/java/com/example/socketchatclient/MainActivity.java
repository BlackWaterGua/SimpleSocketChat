package com.example.socketchatclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Socket socket;
    TextView tvMessages, serverNameOut;
    String tmp, SERVER_IP, serverName, ipName, portName;
    EditText nameInput, IPinput, portInput, messageInput;
    DataOutputStream out;
    BufferedReader reader;
    boolean link = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nameInput = (EditText) findViewById(R.id.nameInput);
        IPinput = (EditText) findViewById(R.id.IPinput);
        portInput = (EditText) findViewById(R.id.portInput);
    }

    public void btn_connect(View view) {
        setContentView(R.layout.chat_room);
        messageInput = (EditText) findViewById(R.id.messageInput);
        serverNameOut = (TextView) findViewById(R.id.serverNameOut);
        tvMessages = (TextView) findViewById(R.id.tvMessages);
        serverNameOut.setText(serverName);
        SERVER_IP = "10.0.2.2";
        serverName = nameInput.getText().toString();
        ipName = IPinput.getText().toString();
        portName = portInput.getText().toString();
        serverNameOut.setText(serverName);
        link = true;
        clientThread server = new clientThread("client");
        Thread m1=new Thread(server);
        m1.start();

    }

    public void btn_leave(View view) {
        SendData sendThread = new SendData("leave");
        Thread m2 = new Thread(sendThread);
        m2.start();
        setContentView(R.layout.activity_main);
        nameInput = (EditText) findViewById(R.id.nameInput);
        IPinput = (EditText) findViewById(R.id.IPinput);
        portInput = (EditText) findViewById(R.id.portInput);
        link = false;
    }

    public void btn_send(View view) {
        SendData sendThread = new SendData(messageInput.getText().toString());
        Thread m2 = new Thread(sendThread);
        m2.start();
    }

    class clientThread implements Runnable {
        private String name;
        public clientThread(String str)
        {
            name=str;
        }
        public void run() {
            try{
                socket = new Socket(SERVER_IP, 6100);
                if(socket.isConnected()){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvMessages.setText("");
                        }
                    });
                    //取得網路輸入串流
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //取得網路輸出串流
                    out = new DataOutputStream(socket.getOutputStream());
                    SendData sendThread = new SendData("Connected");
                    Thread m2 = new Thread(sendThread);
                    m2.start();
                    while((tmp = reader.readLine()) != null){
                        JSONObject jsonObj = new JSONObject(tmp);
                        runOnUiThread(new Runnable() {
                            JSONObject _jsonObj;
                            @Override
                            public void run() {
                                try{
                                    String name = jsonObj.get("name").toString();
                                    String msg = jsonObj.get("message").toString();
                                    tvMessages.append(name+": "+msg+"\n");
//                                tvMessages.append((socket.getInetAddress().getHostAddress()+": "+tmp+"\n"));
                                }catch(JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            public Runnable init(JSONObject jsonObj){
                                _jsonObj = jsonObj;
                                return this;
                            }
                        }.init(jsonObj));
                    }
                }
            }catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // get server IP
    public static String getLocalIpAddress(){
        try{
            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for(Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()){
                        return inetAddress.getHostAddress();
                    }
                }
            }
        }catch (SocketException ex){
            Log.e("WifiPreference IpAddress", ex.toString());
        }
        return null;
    }

    class SendData implements Runnable{
        private String message;
        SendData(String message) {
            this.message = message;
        }
        @Override
        public  void run() {
            Map<String, Object> map = new HashMap();
            map.put("message", message);
            map.put("name", serverName);
            try{
                JSONObject json = new JSONObject(map);
                byte[] jsonByte = (json.toString() + "\n").getBytes();
                out.write(jsonByte);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMessages.append(serverName + ": " + message + "\n");
                }
            });
            if(message.equals("leave")){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}