package com.example.croll;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    String msg1, msg2, msg3, msg4, msg5, weatherMsg;
    TextView textView1, textView2, textView3, textView4, locationText;
    ImageView weatherImg;
    String TAG = "MainActivity";
    UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    TextView textStatus, stime;
    Button btnParied, button;
    Button upBtn, downBtn, stopBtn;
    ImageButton btnCroll, alarm8;
    Integer alarm8Int;
    ListView listView;
    ArrayAdapter<CharSequence> m_adtType;
    Switch weatherSwitch;
    Resources system;
    MediaPlayer mediaPlayer;
    boolean flag;


    ScrollView scrollview_list, mainScroll;


    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    private final static int REQUEST_ENABLE_BT = 1;
    BluetoothSocket btSocket = null;
    ConnectedThread connectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("스무디 (유지태, 한장훈, 홍송의)");


        stopBtn = (Button)findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connectedThread!=null){ connectedThread.write("ST"); }
            }
        });

        upBtn = (Button)findViewById(R.id.upBtn);
        downBtn = (Button)findViewById(R.id.downBtn);
        upBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connectedThread!=null){ connectedThread.write("U"); }
            }
        });
        downBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connectedThread!=null){ connectedThread.write("D"); }
            }
        });


        button = (Button) findViewById(R.id.button);
        stime = (TextView) findViewById(R.id.stime);

        final TimePicker picker = (TimePicker) findViewById(R.id.timePicker);
        picker.setIs24HourView(true);


        // 앞서 설정한 값으로 보여주기
        // 없으면 디폴트 값은 현재시간
        SharedPreferences sharedPreferences = getSharedPreferences("daily alarm", MODE_PRIVATE);
        long millis = sharedPreferences.getLong("nextNotifyTime", Calendar.getInstance().getTimeInMillis());

        Calendar nextNotifyTime = new GregorianCalendar();
        nextNotifyTime.setTimeInMillis(millis);

        Date nextDate = nextNotifyTime.getTime();
        String date_text = new SimpleDateFormat("HH시 mm분 ", Locale.getDefault()).format(nextDate);

        stime.setText("알람 시간: " + date_text);
        Toast.makeText(getApplicationContext(), "다음 알람은 " + date_text, Toast.LENGTH_SHORT).show();


        // 이전 설정값으로 TimePicker 초기화
        Date currentTime = nextNotifyTime.getTime();
        SimpleDateFormat HourFormat = new SimpleDateFormat("kk", Locale.getDefault());
        SimpleDateFormat MinuteFormat = new SimpleDateFormat("mm", Locale.getDefault());

        int pre_hour = Integer.parseInt(HourFormat.format(currentTime));
        int pre_minute = Integer.parseInt(MinuteFormat.format(currentTime));


        if (Build.VERSION.SDK_INT >= 23) {
            picker.setHour(pre_hour);
            picker.setMinute(pre_minute);
        } else {
            picker.setCurrentHour(pre_hour);
            picker.setCurrentMinute(pre_minute);
        }


        //알람설정
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {

                int hour, hour_24, minute;
                String am_pm;
                if (Build.VERSION.SDK_INT >= 23) {
                    hour_24 = picker.getHour();
                    minute = picker.getMinute();
                } else {
                    hour_24 = picker.getCurrentHour();
                    minute = picker.getCurrentMinute();
                }
                if (hour_24 > 12) {
                    am_pm = "PM";
                    hour = hour_24 - 12;
                } else {
                    hour = hour_24;
                    am_pm = "AM";
                }

                // 현재 지정된 시간으로 알람 시간 설정
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, hour_24);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                // 이미 지난 시간을 지정했다면 다음날 같은 시간으로 설정
                if (calendar.before(Calendar.getInstance())) {
                    calendar.add(Calendar.DATE, 1);
                }

                Date currentDateTime = calendar.getTime();
                String date_text = new SimpleDateFormat("HH시 mm분 ", Locale.getDefault()).format(currentDateTime);
                stime.setText("알람 시간: " + date_text);
                Toast.makeText(getApplicationContext(), date_text + "으로 알람이 설정되었습니다!", Toast.LENGTH_SHORT).show();

                //  Preference에 설정한 값 저장
                SharedPreferences.Editor editor = getSharedPreferences("daily alarm", MODE_PRIVATE).edit();
                editor.putLong("nextNotifyTime", (long) calendar.getTimeInMillis());
                editor.apply();


                diaryNotification(calendar);
            }

        });

        //이중 스크롤뷰 활성화
        mainScroll = (ScrollView) findViewById(R.id.mainScroll);
        scrollview_list = (ScrollView) findViewById(R.id.scrollView_list);
        listView = (ListView) findViewById(R.id.listview);
        scrollview_list.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    mainScroll.requestDisallowInterceptTouchEvent(false);
                else
                    mainScroll.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scrollview_list.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        weatherImg = (ImageView) findViewById(R.id.weatherImg); //날씨그림
        alarm8 = (ImageButton) findViewById(R.id.alarmB);//알람설정 활성화버튼
        alarm8Int = 1;
        //   txtHour = (TextView)findViewById(R.id.txtHour);
        // txtMinute = (TextView)findViewById(R.id.txtMinute);

        //  Spinner spinnerAdd = (Spinner)findViewById(R.id.spinnerAdd);
        m_adtType = ArrayAdapter.createFromResource(this, R.array.요일, R.layout.row_spinner);
        m_adtType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //알람설정 활성화버튼
        alarm8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarm8Int += 1;
                if (alarm8Int % 2 != 1) {
                    button.setVisibility(View.VISIBLE);
                    picker.setVisibility(View.VISIBLE);
                } else {
                    button.setVisibility(View.GONE);
                    picker.setVisibility(View.GONE);
                }
            }
        });
     /*   spinnerAdd.setAdapter( m_adtType );

      //  btnAlarm = (Button)findViewById(R.id.btnAlarm);  //알람
       // edtHour = (EditText)findViewById(R.id.edtHour);  //시간
       // edtMinute = (EditText)findViewById(R.id.edtMinute);  //분
        spinnerAdd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {   //스피너 이벤트
                if(position == 0)
                    day = "0";
                else if(position == 1)
                    day = "1";
                else if(position == 2)
                    day = "2";
                else if(position == 3)
                    day = "3";
                else if(position == 4)
                    day = "4";
                else if(position == 5)
                    day = "5";
                else if(position == 6)
                    day = "6";
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        //알람버튼
        btnAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarm = day + "/" + edtHour.getText().toString() + "/" + edtMinute.getText().toString();
                if(connectedThread!=null){ connectedThread.write(alarm); }
            }
        });*/

        // Get permission
        String[] permission_list = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(MainActivity.this, permission_list, 1);

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        textStatus = (TextView) findViewById(R.id.text_status);
        btnParied = (Button) findViewById(R.id.btn_paired);
        //btnSend = (Button) findViewById(R.id.btn_send);


        // 페어링 장치 출력
        btArrayAdapter = new ArrayAdapter<>(this, R.layout.listview_item);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener(new myOnItemClickListener());

        textView1 = (TextView) findViewById(R.id.weather1);
        textView2 = (TextView) findViewById(R.id.weather2);
        textView3 = (TextView) findViewById(R.id.weather3);
        textView4 = (TextView) findViewById(R.id.weather4);
        locationText = (TextView) findViewById(R.id.locationText);
        final Bundle bundle = new Bundle();

        //앱 실행 시 날씨 크롤링
        new Thread() {  //날씨 크롤링
            @Override
            public void run() {
                Document doc = null;
                try {
                    doc = Jsoup.connect("https://weather.naver.com/today/02410105").get(); //숫자가 위치
                    Element contents = doc.select(".weather_area strong").first();          //현재 온도
                    Element contents2 = doc.select(".temperature").first();              //어제보다 온도
                    Element contents3 = doc.select(".weather_area dl").first();             //강수 습도 풍향 체감
                    Element contents4 = doc.select(".weather.before_slash").first();        //현재 날씨
                    Element contents5 = doc.select(".location_name").first();

                    msg1 = contents.text(); //현재 온도
                    msg2 = "어제보다 " + contents2.text(); //어제보다 온도
                    msg3 = contents3.text(); //강수 습도 북동풍 체감
                    msg4 = contents4.text(); //현재날씨
                    msg5 = contents5.text(); //위치

                    bundle.putString("weather1", msg1);                               //핸들러를 이용해서 Thread()에서 가져온 데이터를 메인 쓰레드에 보내준다.
                    bundle.putString("weather2", msg2);
                    bundle.putString("weather3", msg3);
                    bundle.putString("weather4", msg4);
                    bundle.putString("weather5", msg5);
                    Message msg = handler.obtainMessage();
                    msg.setData(bundle);
                    handler.sendMessage(msg);


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        //날씨 새로고침 버튼
        btnCroll = (ImageButton) findViewById(R.id.btnCroll);
        btnCroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "날씨 새로고침합니다.", Toast.LENGTH_SHORT).show();
                new Thread() {  //날씨 크롤링
                    @Override
                    public void run() {
                        Document doc = null;
                        try {
                            doc = Jsoup.connect("https://weather.naver.com/today/02410105").get(); //숫자가 위치
                            Element contents = doc.select(".weather_area strong").first();          //현재 온도
                            Element contents2 = doc.select(".temperature").first();              //어제보다 온도
                            Element contents3 = doc.select(".weather_area dl").first();             //강수 습도 풍향 체감
                            Element contents4 = doc.select(".weather.before_slash").first();        //현재 날씨
                            Element contents5 = doc.select(".location_name").first();

                            msg1 = contents.text(); //현재 온도
                            msg2 = "어제보다 " + contents2.text(); //어제보다 온도
                            msg3 = contents3.text(); //강수 습도 북동풍 체감
                            msg4 = contents4.text(); //현재날씨
                            msg5 = contents5.text(); //위치

                            bundle.putString("weather1", msg1);                               //핸들러를 이용해서 Thread()에서 가져온 데이터를 메인 쓰레드에 보내준다.
                            bundle.putString("weather2", msg2);
                            bundle.putString("weather3", msg3);
                            bundle.putString("weather4", msg4);
                            bundle.putString("weather5", msg5);
                            Message msg = handler.obtainMessage();
                            msg.setData(bundle);
                            handler.sendMessage(msg);


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });

        //LED ON/OFF & 날씨정보 보내기
        weatherSwitch = (Switch) findViewById(R.id.ledPower);
        weatherSwitch.setOnCheckedChangeListener(new weatherSwitchListener());

    }

    void diaryNotification(Calendar calendar) {
//        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        Boolean dailyNotify = sharedPref.getBoolean(SettingsActivity.KEY_PREF_DAILY_NOTIFICATION, true);
        Boolean dailyNotify = true; // 무조건 알람을 사용

        PackageManager pm = this.getPackageManager();
        ComponentName receiver = new ComponentName(this, DeviceBootReceiver.class);
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);


        // 사용자가 매일 알람을 허용했다면
        if (dailyNotify) {


            if (alarmManager != null) {

                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pendingIntent);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }

            // 부팅 후 실행되는 리시버 사용가능하게 설정
            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            textView1.setText(bundle.getString("weather1"));                      //이런식으로 View를 메인 쓰레드에서 뿌려줘야한다.
            textView2.setText(bundle.getString("weather2"));
            textView3.setText(bundle.getString("weather3"));
            textView4.setText(bundle.getString("weather4"));
            locationText.setText(bundle.getString("weather5"));
            if (textView4.getText().equals("맑음"))
                weatherImg.setImageResource(R.drawable.sun);
            else if (textView4.getText().equals("구름많음"))
                weatherImg.setImageResource(R.drawable.suncloud);
            else if (textView4.getText().equals("흐림"))
                weatherImg.setImageResource(R.drawable.cloud);
            else if (textView4.getText().equals("눈"))
                weatherImg.setImageResource(R.drawable.snow);
            else if (textView4.getText().equals("비"))
                weatherImg.setImageResource(R.drawable.rain);
        }
    };

    public void onClickButtonPaired(View view) {  //블루투스 연결
        btArrayAdapter.clear();
        scrollview_list.setVisibility(View.VISIBLE);

        if (deviceAddressArray != null && !deviceAddressArray.isEmpty()) {
            deviceAddressArray.clear();
        }
        pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // 페어링 된 장치의 이름과 주소 가져오기
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC 주소
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
            }
        }
    }

/*
    // 날씨정보보내기 버튼
    public void onClickButtonSend(View view){
        try{
            String weatherMsg = "0";
            if(msg4.equals("맑음"))
                weatherMsg = "S";
            else if(msg4.equals("구름많음"))
                weatherMsg = "SC";
            else if(msg4.equals("흐림"))
                weatherMsg = "C";
            else if(msg4.equals("눈"))
                weatherMsg = "SN";
            else if(msg4.equals("비"))
                weatherMsg = "R";
            if(connectedThread!=null){ connectedThread.write(weatherMsg); }
        }catch (Exception e){
            Toast.makeText(MainActivity.this, "날씨정보를 새로고침 해주세요.", Toast.LENGTH_SHORT).show();
        }

    }*/

    public class myOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(getApplicationContext(), btArrayAdapter.getItem(position), Toast.LENGTH_SHORT).show();

            textStatus.setText("try...");

            final String name = btArrayAdapter.getItem(position); // 이름 가져오기
            final String address = deviceAddressArray.get(position); // 주소 가져오기
            boolean flag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            // 소켓 생성 및 연결
            try {
                btSocket = createBluetoothSocket(device);
                btSocket.connect();
            } catch (IOException e) {
                flag = false;
                textStatus.setText("연결실패!!");
                e.printStackTrace();
            }

            // 블루투스 통신 시작
            if (flag) {
                textStatus.setText(name + "가 연결되었습니다.");
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();
            }

        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    //알람소리
    public void onClickButton(View view){
        if(flag) {
            mediaPlayer.pause();
            flag = false;
        } else {
            mediaPlayer.start();
            flag = true;
        }
    }

    //LED ON/OFF & 전원 리스너 이벤트
    class weatherSwitchListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            weatherMsg = "0";
            if (isChecked)
                try {
                    if (msg4.equals("맑음"))
                        weatherMsg = "S";
                    else if (msg4.equals("구름많음"))
                        weatherMsg = "SC";
                    else if (msg4.equals("흐림"))
                        weatherMsg = "C";
                    else if (msg4.equals("눈"))
                        weatherMsg = "SN";
                    else if (msg4.equals("비"))
                        weatherMsg = "R";
                    if (connectedThread != null) {
                        connectedThread.write(weatherMsg);
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "날씨정보를 새로고침 해주세요.", Toast.LENGTH_SHORT).show();
                }
            else {
                weatherMsg = "0";
                if (connectedThread != null) {
                    connectedThread.write(weatherMsg);
                }
                Toast.makeText(MainActivity.this, "종료", Toast.LENGTH_SHORT).show();
            }

        }
    }
}
