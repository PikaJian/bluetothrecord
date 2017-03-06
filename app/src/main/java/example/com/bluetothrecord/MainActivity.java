package example.com.bluetothrecord;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Button;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;
import android.support.annotation.NonNull;
import android.media.AudioFormat;
import android.media.AudioRecord;

import java.lang.CharSequence;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {
    private String TAG = "BluetoothRecord";
    boolean isRecording=false;
    private short[] mBuffer;
    AudioRecord mARecorder;
    AudioTrack mATracker;
    private MediaRecorder mMRecorder = null;
    private static String mFileName = null;
    private AudioManager mAudioManager = null;
    private Button startRecordButton;
    private Button stopRecordButton;
    private Button MicRecordButton;
    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions =
            {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.BROADCAST_STICKY,
                    Manifest.permission.BLUETOOTH

            };

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 201;
    private static final int REQUEST_MODIFY_AUDIO_SETTINGS =  202;
    private static final int REQUEST_BROADCAST_STICKY = 203;
    private static final int REQUEST_BLUETOOTH = 204;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_MODIFY_AUDIO_SETTINGS:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_BROADCAST_STICKY:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_BLUETOOTH:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/record.pcm";
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        startRecordButton = (Button)findViewById(R.id.button1);
        stopRecordButton = (Button)findViewById(R.id.button2);
        MicRecordButton = (Button)findViewById(R.id.button3);
        startRecordButton.setText("start record");
        stopRecordButton.setText("stop record");
        MicRecordButton.setText("mic start");

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);

                if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                    Log.i(TAG, "AudioManager.SCO_AUDIO_STATE_CONNECTED");
                    mAudioManager.setSpeakerphoneOn(false);
                    mAudioManager.setBluetoothScoOn(true);  //打开SCO
                    Log.i(TAG, "Routing:" + mAudioManager.isBluetoothScoOn());
                    mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    //mRecorder.start();//开始录音
                } else if(AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state){
                    Log.i(TAG, "AudioManager.SCO_AUDIO_STATE_DISCONNECTED");
                    unregisterReceiver(this);  //别遗漏
                } else if(AudioManager.SCO_AUDIO_STATE_CONNECTING == state) {
                    Log.i(TAG, "AudioManager.SCO_AUDIO_STATE_CONNECTING");
                } else{//等待一秒后再尝试启动SCO
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mAudioManager.startBluetoothSco();
                    Log.i(TAG, "再次startBluetoothSco()");

                }
            }
        }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

        MicRecordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(isRecording)
                {
                    MicRecordButton.setText("mic start");
                    isRecording=false;
                    mARecorder.stop();
                    mATracker.flush();
                    mATracker.stop();
                    mATracker.release();
                    if (mAudioManager.isBluetoothScoOn()) {
                        Log.i(TAG, "stop sco recording");
                        mAudioManager.setBluetoothScoOn(false);
                        mAudioManager.stopBluetoothSco();
                        mAudioManager.setSpeakerphoneOn(true);
                    }
                } else{
                    initRecorder();
                    if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
                        Log.i(TAG, "system not support bluetooth record");
                        return;
                    }
                    Log.i(TAG, "system support bluetooth record");
                    mAudioManager.stopBluetoothSco();
                    mAudioManager.startBluetoothSco();//蓝牙录音的关键，启动SCO连接，耳机话筒才起作用
                    MicRecordButton.setText("mic stop");
                    isRecording=true;
                    mARecorder.startRecording();
                    mATracker.play();
                    Log.i(TAG, mFileName);
                    startBufferedWrite(new File(mFileName));
                }
            }
        });
        startRecordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                startRecording();
            }
        });
        stopRecordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                stopRecording();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }    //record
    private void startRecording(){
        //mRecorder = new MediaRecorder();
        //mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        //mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        //mRecorder.setOutputFile(mFileName);
        //mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        Context context = getApplicationContext();
        CharSequence text = "Hello toast!";
        int duration = Toast.LENGTH_SHORT;
        //try {
        //    mRecorder.prepare();
        //} catch (Exception e) {
        //    // TODO: handle exception
        //    Toast toast = Toast.makeText(context, "prepare() failed!", duration);
        //    toast.show();
        //    Log.i(TAG, "prepare() failed!");
        //}
        if (!mAudioManager.isBluetoothScoAvailableOffCall()) {
            Toast toast = Toast.makeText(context, "system not support bluetooth record", duration);
            toast.show();
            Log.i(TAG, "system not support bluetooth record");
            return;
        }
        Log.i(TAG, "system support bluetooth record");
        mAudioManager.stopBluetoothSco();
        mAudioManager.startBluetoothSco();//蓝牙录音的关键，启动SCO连接，耳机话筒才起作用
    }


    private void stopRecording(){
        if (mAudioManager.isBluetoothScoOn()) {
            Log.i(TAG, "stop sco recording");
            mAudioManager.setBluetoothScoOn(false);
            mAudioManager.stopBluetoothSco();
            mAudioManager.setSpeakerphoneOn(true);
        }
        /*
                mAudioManager.stopBluetoothSco();
                if ( mRecorder == null)
                return;
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                */
    }
    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    while (isRecording) {
                        int readSize = mARecorder.read(mBuffer, 0, mBuffer.length);
                        mATracker.write(mBuffer, 0, mBuffer.length);
                        for (int i = 0; i < readSize; i++) {
                            output.writeShort(mBuffer[i]);
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            try {
                                Log.i(TAG, "close file");
                                output.close();
                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        }).start();
    }
    private void initRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mBuffer = new short[bufferSize];

        mARecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 8000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        mATracker = new AudioTrack(AudioManager.MODE_IN_COMMUNICATION, 8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        mATracker.setPlaybackRate(8000);
    }
    private void DisplayToast(String msg)
    {
         Toast.makeText(getBaseContext(), msg,
                 Toast.LENGTH_SHORT).show();
    }    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mARecorder.release();
    }
}
