package cn.catver.sms_read;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SMSAndTelephoneService extends Service {
    public static final String TAG = "SMS_READ_SERVICE";
    public static ArrayList<String> NUMBERS = null;
    public static ArrayList<String> CALLNUMBERS = null;
    SMSRecv smsRecv;
    RecvBroadcast recvBroadcast;
    public static MediaPlayer alertPlayer;
    public static Vibrator vibrator;
    public static SMSAndTelephoneService INSTANCE;
    public SMSAndTelephoneService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        if (NUMBERS == null) {
            NUMBERS = new ArrayList<>();
            Log.i(TAG, "onCreate: init NUMBERS");
        }
        if (CALLNUMBERS == null) {
            CALLNUMBERS = new ArrayList<>();
            Log.i(TAG, "onCreate: init CALLNUMBERS");
        }

        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
            smsRecv = new SMSRecv();
            intentFilter.setPriority(Integer.MAX_VALUE);
            registerReceiver(smsRecv,intentFilter);
        }
        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("cn.catver.sms_read.update");
            intentFilter.addAction("cn.catver.sms_read.stop.alert");
            recvBroadcast = new RecvBroadcast();
            intentFilter.setPriority(Integer.MAX_VALUE);
            registerReceiver(recvBroadcast,intentFilter);
        }

        {
            vibrator = ((Vibrator) getSystemService(VIBRATOR_SERVICE));
            alertPlayer = new MediaPlayer();
            alertPlayer.setLooping(true);
            AssetFileDescriptor descriptor = getResources().openRawResourceFd(R.raw.alert3);
            try {
                alertPlayer.setDataSource(descriptor.getFileDescriptor(),descriptor.getStartOffset(),descriptor.getLength());
            } catch (Exception e) {
//                throw new RuntimeException(e);
                e.printStackTrace();
                alertPlayer = null;
                Toast.makeText(this, "无法初始化声音！", Toast.LENGTH_SHORT).show();
            }

        }

        Log.i(TAG, "onCreate: Service init finish");
        if(isRunningForeground(this)){
            Log.i(TAG, "onCreate: activity is running");
            SendBroadcast(0);
        }else{
            Log.i(TAG, "onCreate: activity dead!");
            { //读取配置文件（划掉
                StringBuilder sb = new StringBuilder();
                {
                    String temp;
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(openFileInput("nums.json")));
                        while ((temp = br.readLine()) != null){
                            sb.append(temp);
                        }
                    } catch (FileNotFoundException e) {
                        Toast.makeText(this, "你还没有设置号码！", Toast.LENGTH_SHORT).show();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
                try {
                    NUMBERS.clear();
                    CALLNUMBERS.clear();
                    JSONObject jsonObject = new JSONObject(sb.toString());
                    JSONArray numbers = jsonObject.getJSONArray("numbers");
                    for (int i = 0; i < numbers.length(); i++) {
                        NUMBERS.add(numbers.getString(i));
                    }
                    JSONArray callnumbers = jsonObject.getJSONArray("callnumbers");
                    for (int i = 0; i < callnumbers.length(); i++) {
                        CALLNUMBERS.add(callnumbers.getString(i));
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, "配置文件错误！", Toast.LENGTH_SHORT).show();
                }
            }
        }


    }

    public void SendBroadcast(int act){
        Intent intent = new Intent();
        intent.setAction("cn.catver.sms_read.service.msg");
        intent.putExtra("act",act);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsRecv);
        unregisterReceiver(recvBroadcast);
        if (NUMBERS == null) {
            Log.i(TAG, "onDestroy: null!");
        }
    }

    public static void PlayAlert(){
        if (alertPlayer == null) {
            return;
        }
        if(alertPlayer.isPlaying()) return;

        alertPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                alertPlayer.start();
            }
        });
        alertPlayer.prepareAsync();

        long[] patter = {1000,1000,2000,50};
        vibrator.vibrate(patter,0);
    }

    public static void StopAlert(){
        if (alertPlayer == null) {
            return;
        }
        if(alertPlayer.isPlaying()){
            alertPlayer.stop();
        }
        vibrator.cancel();
    }

    class RecvBroadcast extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("cn.catver.sms_read.update")){
                Log.i(TAG, "onReceive: Recv Activity Broadcast");
                Bundle bundle = intent.getExtras();
                NUMBERS = (ArrayList<String>) bundle.getStringArrayList("numbers").clone();
                CALLNUMBERS = (ArrayList<String>) bundle.getStringArrayList("callnumbers").clone();
                Log.i(TAG, "onReceive: NUMBERS:");
                for (String s : NUMBERS) {
                    Log.i(TAG, "onReceive: "+s);
                }
                Log.i(TAG, "onReceive: CALLNUMBERS");
                for (String s : CALLNUMBERS) {
                    Log.i(TAG, "onReceive: "+s);
                }
            } else if (intent.equals("cn.catver.sms_read.stop.alert")) {
                StopAlert();
            }

        }
    }

    public static boolean isRunningForeground (Context context) {
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if(!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals(context.getPackageName())) {
            return true ;
        }
        return false ;
    }
}