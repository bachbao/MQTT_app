package com.example.mttq_2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    ToggleButton toggleButton;
    MQTTHelper mqttHelper;
    TextView txtTmp, txtHum;
    private Button button1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupScheduler();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        txtTmp  = findViewById(R.id.txtTemp);
        txtHum = findViewById(R.id.txtHumid);

        txtTmp.setText("40" + "°C");
        txtHum.setText("80" +"%");

        button1 = (Button) findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity2();
            }
        });

        toggleButton = findViewById(R.id.toggleButton1);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked == true){
                    toggleButton.setVisibility(View.INVISIBLE);
                    Log.d("mqtt", "onCheckedChanged: true");
                    sendDataMQTT("bbace/f/bb-humid","1");
                }
                else{
                    toggleButton.setVisibility(View.INVISIBLE);
                    Log.d("mqtt", "onCheckedChanged: false");
                    sendDataMQTT("bbace/f/bb-humid","0");
                }
            }
        });
        startMQTT();
    }

    public void openActivity2(){
        Intent intent = new Intent(this,Main2Activity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        aTimer.cancel();
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void startMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext(), "aasfwf21fwfasc");
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("mqtt","Connection is successful");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d("mqtt","Connection is lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("mqtt","Received: "+ message.toString());
                Log.d("mqtt", "From topic"+ topic);
                if(topic.equals("bbace/f/bb-temp")){
                    txtTmp.setText(message.toString() + "°C");
                }
                else if(topic.equals("bbace/f/error-control")){
                    txtHum.setText(message.toString() + "%");
                    toggleButton.setVisibility(View.VISIBLE);
                    waiting_period = 0;
                    send_message_again = false;
                    resend_counter = 0;
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("mqtt","Complete delivery");
            }
        });
    }
    public class MQTTMessage{
        String topic;
        String value;
    }
    int waiting_period = 0;
    boolean send_message_again = false;
    List<MQTTMessage> list = new ArrayList<>();
    Timer aTimer = new Timer();
    int resend_counter = 0;
    private void setupScheduler(){
        TimerTask scheduler = new TimerTask() {
            @Override
            public void run(){
                Log.d("mqtt", "Timer is executed");
                if(waiting_period > 0){
                    waiting_period--;
                    if(waiting_period == 0){
                        send_message_again = true;
                    }
                }
                if(send_message_again == true){
                    sendDataMQTT(list.get(0).topic,list.get(0).value);
                    resend_counter +=1 ;
                    list.remove(0);
                }
                if(resend_counter == 3){
                    resend_counter = 0;
                    waiting_period = 0;
                }
            }
        };
        aTimer.schedule(scheduler,0, 1000);
    }

    private void sendDataMQTT(String topic, String value){
        waiting_period = 3;
        send_message_again = false;
        MainActivity.MQTTMessage buffer = new MainActivity.MQTTMessage();
        buffer.topic = topic; buffer.value = value;
        list.add(buffer);

        MqttMessage msg = new MqttMessage();
        msg.setId(12343);
        msg.setQos(0);
        msg.setRetained(true);

        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);

        }
        catch (MqttException e){
        }
    }
}
