package com.example.socketchatserver;

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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class MainActivity extends AppCompatActivity {
    EditText serverNameInput;
    EditText messageInput;
    String serverName;
    TextView serverNameOut, tvMessages;
//    Socket socket;
    DataOutputStream out;
    ArrayList<Socket> client = new ArrayList<Socket>();
    ArrayList<String> nameList = new ArrayList<String>();
    int skip = -1;
    boolean self=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViewElement();
    }

    private  void initViewElement() {
        serverNameInput = (EditText) findViewById(R.id.serverNameInput);
//        serverNameOut = (TextView) findViewById(R.id.serverNameOut);
    }

    public void btn_connect(View view) {
        setContentView(R.layout.chat_room);
//        initViewElement();
        serverNameOut = (TextView) findViewById(R.id.serverNameOut);
        serverName = serverNameInput.getText().toString();
        tvMessages = (TextView) findViewById(R.id.tvMessages);
        messageInput = (EditText) findViewById(R.id.messageInput);
        serverNameOut.setText(serverName);
        serverThread server = new serverThread("server");
        Thread m1=new Thread(server);
        m1.start();
    }

    public void btn_leave(View view) {
        SendData sendThread = new SendData("left");
        Thread m2 = new Thread(sendThread);
        m2.start();
        setContentView(R.layout.activity_main);
        serverNameInput = (EditText) findViewById(R.id.serverNameInput);
    }

    public void btn_send(View view) {
        self = true;
        SendData sendThread = new SendData(messageInput.getText().toString());
        Thread m2 = new Thread(sendThread);
        m2.start();
    }

    class serverThread implements Runnable{
        private String name;
        public serverThread(String str)
        {
            name=str;
        }
        @Override
        public void run(){
            try{
                ServerSocket serverSocket = new ServerSocket(7100);
                if(!serverSocket.isClosed()){
                    tvMessages.setText(serverName+ " started (" +getLocalIpAddress()+")\n");
                }
                try{
                    while(true) {
                        Socket socket = serverSocket.accept();
                        client.add(socket);
                        Handler server2 = new Handler(socket);
                        Thread m2 = new Thread(server2);
                        m2.start();
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                tvMessages.append("Connected\n");
//                            }
//                        });
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Handler implements Runnable{
        private Socket socket;
        public Handler(Socket socket){
            this.socket = socket;
        }
        public void run(){
            try {
                //取得網路輸入串流
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //取得網路輸出串流
//                out = new DataOutputStream(socket.getOutputStream());
                String tmp;
                while ((tmp = reader.readLine()) != null) {
                    JSONObject jsonObj = new JSONObject(tmp);
//                    String finalTmp = tmp;
                    runOnUiThread(new Runnable() {
                        JSONObject _jsonObj;
                        @Override
                        public void run() {
                            try{
                                String name = jsonObj.get("name").toString();
                                String msg = jsonObj.getString("message");
                                if(msg.equals("Connected")){
                                    tvMessages.append(name+"("+ socket.getRemoteSocketAddress().toString() +") "+msg+"\n");
                                    nameList.add(name);
                                    self = true;
                                    SendData sendThread = new SendData("welcome " + name + " join us!");
                                    Thread m2 = new Thread(sendThread);
                                    m2.start();
                                }
                                else if(msg.equals("leave")){
                                    int i;
                                    for(i=0;i<nameList.size();i++){
                                        if(nameList.get(i).equals(name)){
                                            break;
                                        }
                                    }
                                    client.get(i).close();
                                    self = true;
                                    SendData sendThread = new SendData(name + " has left");
                                    Thread m2 = new Thread(sendThread);
                                    m2.start();
                                }
                                else{
                                    tvMessages.append(name + ": "+msg+"\n");
                                    int i;
                                    for(i=0;i<nameList.size();i++){
                                        if(nameList.get(i).equals(name)){
                                            break;
                                        }
                                    }
                                    skip = i;
                                    self = false;
                                    SendData sendThread = new SendData(msg);
                                    Thread m2 = new Thread(sendThread);
                                    m2.start();
                                }
//                                tvMessages.append((socket.getInetAddress().getHostAddress() + ": " + finalTmp + "\n"));
                            }catch(JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        }

                        public Runnable init(JSONObject jsonObj) {
                            _jsonObj = jsonObj;
                            return this;
                        }
                    }.init(jsonObj));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class SendData implements Runnable{
        private String message;
        SendData(String message) {
            this.message = message;
        }
        @Override
        public  void run() {
            Socket[] cs = new Socket[client.size()];
            client.toArray(cs);
            int j=-1;
            String speaker;
            if(message.equals("left")){
                message = "server closed. Please press leave button.";
            }
            if(skip != -1)
                speaker = nameList.get(skip);
            else
                speaker = serverName;
            for(Socket socket :cs) {
                j++;
                if(j==skip)
                    continue;
                try {
                    out = new DataOutputStream(socket.getOutputStream());
                    Map<String, Object> map = new HashMap();
                    map.put("message", message);
                    map.put("name", speaker);
                    try {
                        JSONObject json = new JSONObject(map);
                        byte[] jsonByte = (json.toString() + "\n").getBytes();
                        out.write(jsonByte);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(self) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.append(serverName+": " + message + "\n");
                    }
                });
            }
            skip = -1;
            if(message.equals("server closed. Please press leave button.")){
                for(Socket socket :cs) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

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
}