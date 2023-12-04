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
import java.util.Random;

public class SMSAndTelephoneService extends Service {
    public static final String TAG = "SMS_READ_SERVICE";
    public static ArrayList<String> NUMBERS = null;
    public static ArrayList<String> CALLNUMBERS = null;
    SMSRecv smsRecv;
    RecvBroadcast recvBroadcast;
    public static MediaPlayer alertPlayer;
    public static Vibrator vibrator;
    public static SMSAndTelephoneService INSTANCE;
    NotificationManager manager;
    boolean running;
    public SMSAndTelephoneService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        running = true;
        super.onCreate();

        {
            Notification notification = new Notification.Builder(SMSAndTelephoneService.this,"save2")
                    .build();
            startForeground(9,notification);
        }

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
            intentFilter.addAction("cn.catver.sms_read.service.stop");
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
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Log.i(TAG, "onCreate: Service init finish");
        if(isRunningForeground(this)){
            Log.i(TAG, "onCreate: activity is running");
            SendBroadcast(0);
        }else{
            Log.i(TAG, "onCreate: activity dead!");
            stopSelf();
        }
        {
            Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(this,1000);
                    Random random = new Random();
                    Notification.Builder builder = new Notification.Builder(SMSAndTelephoneService.this,"save1")
                            .setContentTitle(String.format("%d", random.nextInt()))
                            .setContentText(String.format("%d", random.nextInt()))
                            .setSmallIcon(R.drawable.ic_launcher_background);
                    manager.notify(0,builder.build());
                }
            };
            handler.postDelayed(runnable,1000);
        }
        Notification.Builder builder = new Notification.Builder(SMSAndTelephoneService.this,"save2")
                .setContentTitle("短信监控")
                .setContentText("服务启动！")
                .setSmallIcon(R.drawable.ic_launcher_background);
        manager.notify(1,builder.build());

    }

    public void SendBroadcast(int act){
        Intent intent = new Intent();
        intent.setAction("cn.catver.sms_read.service.msg");
        intent.putExtra("act",act);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if(running){
            SendBroadcast(1);
        }
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
            } else if (intent.equals("cn.catver.sms_read.service.stop")) {
                running = false;
                stopSelf();
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