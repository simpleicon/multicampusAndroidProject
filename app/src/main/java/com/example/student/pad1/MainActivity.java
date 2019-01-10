package com.example.student.pad1;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    TextView tv_temp, tv_speed, tv_rpm, tv_battery;
    ImageView img_network;
    ImageButton btn_up, btn_down;
    Server server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        makeUI();
        try {
            server = new Server();
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void makeUI(){
        tv_temp = findViewById(R.id.tv_temp);
        tv_rpm = findViewById(R.id.tv_rpm);
        tv_speed = findViewById(R.id.tv_speed);
        tv_battery = findViewById(R.id.tv_battery);
        img_network = findViewById(R.id.img_network);
        btn_up = findViewById(R.id.btn_up);
        btn_down = findViewById(R.id.btn_down);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_up){
            int temp = Integer.parseInt(tv_temp.getText().toString());
            temp += 1;
            tv_temp.setText(temp+"");
            server.sendMsg(temp+"");
        } else if(v.getId() == R.id.btn_down){
            int temp= Integer.parseInt(tv_temp.getText().toString());
            temp -= 1;
            tv_temp.setText(temp+"");
            server.sendMsg(temp+"");
        }
    }

    public void setTemp(final String data){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String d = data + "°C ";
                        tv_temp.setText(d);
                    }
                });
            }
        };
        new Thread(runnable).start();
    }


    public void setSpeed(final int data){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_speed.setText(data+"");
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    public void setRpm(final int data){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_rpm.setText(data+"");
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    public void setBattery(final int data){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String d = data + "%";
                        tv_battery.setText(d);
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    public void setNetworkimg(final String data){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(data.equals("1")){
                            img_network.setImageResource(R.drawable.connection);
                        } else{
                            img_network.setImageResource(R.drawable.disconection);
                        }

                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    public void display(String msg){
        String cmd = msg.substring(0,2);
        int value = Integer.parseInt(msg.substring(2));

        if(cmd.equals("00")){
            setSpeed(value);
            SendTask sendTask = new SendTask("speed",value);
            sendTask.execute();
        } else if(cmd.equals("01")){
            setRpm(value);
            SendTask sendTask = new SendTask("RPM",value);
            sendTask.execute();
        } else if(cmd.equals("02")){
            setBattery(value);
            SendTask sendTask = new SendTask("Battery",value);
            sendTask.execute();
        }
    }

    //AsyncTask
    class SendTask extends AsyncTask<Void,Void,Void> {
        String id;
        int value;
        HttpURLConnection urlCon;
        String url = "http://70.12.50.148/a/test";

        public SendTask(String id, int value){
            this.id = id;
            this.value = value;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                url += "?id="+id+"&value="+value;
                URL serverUrl = new URL(url);
                urlCon = (HttpURLConnection) serverUrl.openConnection();
                urlCon.setReadTimeout(3000);
                urlCon.setRequestMethod("GET");
                urlCon.getInputStream();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    } // end sendTask

    //asynctask end


    /////SERVER CLASS
    public class Server extends Thread{

        ServerSocket serversocket;
        int port = 8888;

        boolean flag = true;

        ArrayList<DataOutputStream> list; //클라이언트 접속시마다 소켓생성 > 해당 소켓의 스트림을 계속 열어두기 위해서
        String client;

        public Server() throws IOException {
            list = new ArrayList<>();
            serversocket = new ServerSocket(port);
        }

        public void run()  {
            while(flag) {
                Socket socket = null; //클라이언트마다 소켓이 생성되어야 하기 때문에 전역변수로 이용하면 안됨
                try {
                    socket = serversocket.accept();
                    client = socket.getInetAddress().getHostAddress();
                    setNetworkimg("1");
                    new Receiver(socket, client).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void end(){
            if(serversocket != null){
                try {
                    serversocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        public void sendMsg(String msg) {
            // BroadCast msg
            Sender sender = new Sender();
            sender.setMsg(msg);
            sender.start();

        }

        class Receiver extends Thread{

            InputStream is;
            DataInputStream dis;
            String client;

            // For Sender
            OutputStream os;
            DataOutputStream dos;

            public Receiver(Socket socket, String client) throws IOException {
                this.client = client;
                is = socket.getInputStream();
                dis = new DataInputStream(is);

                os = socket.getOutputStream();
                dos = new DataOutputStream(os);
                list.add(dos);
            }

            @Override
            public void run() {
                while(dis != null) {
                    try {
                        String msg = dis.readUTF();
                        display(msg);
                    } catch (IOException e) {
//					e.printStackTrace();
                        break;
                    }
                }// end of while
                try {
                    setNetworkimg("2");
                    list.remove(dos);
                    Thread.sleep(1000);
                    if(dis != null) {
                        dis.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        class Sender extends Thread{
            String msg;

            public void setMsg(String msg) {
                this.msg = msg;
            }

            @Override
            public void run() {
                if(list.size() == 0) {
                    return;
                }
                for(DataOutputStream out : list) {
                    if(out != null) {
                        try {
                            out.writeUTF(msg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }//SERVER END

    //CLIENT START
    // Network Implements Class
    public class Client extends Thread{
        String host = "70.12.50.148";
        int port = 8888;

        Socket socket;

        boolean flag = true;
        Sender sender;

        public void run(){
            while(flag) {
                try {
                    socket = new Socket(host, port);
                    if(socket.isConnected()) {
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Retry ...");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            } // end while
            // Ready Receiver ...
            //connectedIcon(1);
            try {
                new Receiver(socket).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void end(){
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // send Msg
        public void sendMsg(String msg){
            if(socket != null && socket.isConnected()){
                try {
                    sender = new Sender(socket);
                    sender.setMsg(msg);
                    sender.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        class Sender extends Thread{
            OutputStream os;
            DataOutputStream dos;
            String msg;

            public Sender() {}
            public Sender(Socket socket) throws IOException {
                os = socket.getOutputStream();
                dos = new DataOutputStream(os);
            }
            public void setMsg(String msg) {
                this.msg = msg;
            }
            @Override
            public void run() {
                if(dos != null) {
                    try {
                        dos.writeUTF(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        class Receiver extends Thread{

            InputStream is;
            DataInputStream dis;

            public Receiver() {
            }
            public Receiver(Socket socket) throws IOException {
                is = socket.getInputStream();
                dis = new DataInputStream(is);
            }

            @Override
            public void run() {
                while(dis != null) {
                    String msg;
                    try {
                        msg = dis.readUTF();
                        setTemp(msg);
                    } catch (IOException e) {
                        //connectedIcon(2);
                        break;
                    }
                } // end while
                try {
                    Thread.sleep(1000);
                    if(dis != null) {
                        dis.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //CLIENT END

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        server.end();
        System.exit(0);

    }
}
