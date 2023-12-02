package cn.catver.sms_read;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    public static String NUMBER = "123123";
    public static ArrayList<String > NUMBERS  = new ArrayList<>();
    public static ArrayList<String> CALLNUMBERS = new ArrayList<>();

    public static final String TAG = "CatVer-ReadSMS-MainActivity";
    ListView listView = null;
    ArrayList<String> list = new ArrayList<>();
    public static MediaPlayer alertPlayer;
    public static Vibrator vibrator;
    SMSRecv smsRecv = null;
    TelephoneStateRecv telephoneStateRecv = null;
    MainActivity INSTANCE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: !");
        INSTANCE = this;
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


        initUI();
        initBtu();

        {
            int Nok = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
            if(Nok != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECEIVE_SMS},10000);
            }else{
//                startMyService();
                getSMS();
            }
        }

        {
            int Nok = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
            if(Nok != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_PHONE_STATE},100001);
            }else{
//                startMyService();
                getTelephone();
            }
        }

    }
    public void updateSpinner(){ //更新下拉框
        Spinner sprsms = findViewById(R.id.smsspr);

        String[] nums = new String[NUMBERS.size()];
//        String[] calls = new String[CALLNUMBERS.size()];

        NUMBERS.toArray(nums);
//        CALLNUMBERS.toArray(calls);

        ArrayAdapter<String> smslist = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, nums);
        smslist.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        ArrayAdapter<String> callphonelist = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, calls);
//        callphonelist.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sprsms.setAdapter(smslist);
//        sprcallphone.setAdapter(callphonelist);
        save();
    }

    public void initUI(){
        { //init : 为俩个下拉框赋值
            updateSpinner();
        }
    }

    public void initBtu(){
        { //Button 2 : 停止铃声
            Button button = ((Button) findViewById(R.id.button2));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StopAlert();
                }
            });
        }

        { //addbtu1 : 添加按钮（关于sms
            Button button = findViewById(R.id.addbtu1);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    EditText editText = new EditText(MainActivity.this);
                    editText.setSingleLine();
                    builder.setTitle("短信：请输入：");
                    builder.setIcon(R.drawable.ic_launcher_background);
                    builder.setView(editText);
                    builder.setPositiveButton("完成", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.i(TAG, String.format("onClick: %s", editText.getText().toString()));
                            NUMBERS.add(editText.getText().toString());
                            updateSpinner();
                        }
                    });
                    builder.setNegativeButton("放弃",null);
                    builder.show();
                }
            });
        }

//        { //addbtu2 : 添加按钮（关于telephone
//            Button button = findViewById(R.id.addbtu2);
//            button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                    EditText editText = new EditText(MainActivity.this);
//                    editText.setSingleLine();
//                    builder.setTitle("电话：请输入：");
//                    builder.setIcon(R.drawable.ic_launcher_background);
//                    builder.setView(editText);
//                    builder.setPositiveButton("完成", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            Log.i(TAG, String.format("onClick: %s", editText.getText().toString()));
//                            CALLNUMBERS.add(editText.getText().toString());
//                            updateSpinner();
//                        }
//                    });
//                    builder.setNegativeButton("放弃",null);
//                    builder.show();
//                }
//            });
//        }

        { //rmbtu1 : 删除按钮（关于sms
            Button button = findViewById(R.id.rmbtu1);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Spinner spinner = findViewById(R.id.smsspr);
                    if(spinner.getSelectedItem() == null) return;
                    Log.i(TAG, String.format("onClick: %s", spinner.getSelectedItem().toString()));
                    NUMBERS.remove(spinner.getSelectedItem().toString());
                    updateSpinner();
                }
            });
        }

//        { //rmbtu2 : 删除按钮（关于telephone
//            Button button = findViewById(R.id.rmbtu2);
//            button.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Spinner spinner = findViewById(R.id.telephonespr);
//                    if(spinner.getSelectedItem() == null) return;
//                    Log.i(TAG, String.format("onClick: %s", spinner.getSelectedItem().toString()));
//                    CALLNUMBERS.remove(spinner.getSelectedItem().toString());
//                    updateSpinner();
//                }
//            });
//        }

        { //Licensebtu : 协议
            Button button = findViewById(R.id.Licensebtu);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                            .setTitle("License")
                            .setMessage(getResources().getText(R.string.mit_))
                            .setIcon(R.drawable.ic_launcher_background)
                            .setPositiveButton("我知道了",null)
                            .create();
                    dialog.show();
                }
            });
        }


    }

    public void save(){
        JSONArray numbers = new JSONArray();
        for (String s : NUMBERS) {
            numbers.put(s);
        }
        JSONArray callnumbers = new JSONArray();
        for (String s : CALLNUMBERS) {
            callnumbers.put(s);
        }
        JSONObject root = new JSONObject();
        try {
            root.put("numbers",numbers);
            root.put("callnumbers",callnumbers);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream fos = openFileOutput("nums.json",MODE_PRIVATE);
            fos.write(root.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (smsRecv != null) {
            unregisterReceiver(smsRecv);
        }
        if (telephoneStateRecv != null) {
            unregisterReceiver(telephoneStateRecv);
        }
        if(alertPlayer != null){
            if(alertPlayer.isPlaying()) alertPlayer.stop();
            alertPlayer.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 10000 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//            startMyService();
            getSMS();
        }
        if(requestCode == 100001 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            getTelephone();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

//    private void startMyService(){
//        Intent intent = new Intent();
//        intent.setClass(this,SMSRecv.class);
//        startService(intent);
//        Log.i(TAG, "startMyService: Service start!");
//    }

    private void getSMS(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        smsRecv = new SMSRecv();
        intentFilter.setPriority(Integer.MAX_VALUE);
        registerReceiver(smsRecv,intentFilter);
    }

    private void getTelephone(){
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("android.intent.action.PHONE_STATE");
//        telephoneStateRecv = new TelephoneStateRecv();
//        intentFilter.setPriority(Integer.MAX_VALUE);
//        registerReceiver(telephoneStateRecv,intentFilter);
    }
}