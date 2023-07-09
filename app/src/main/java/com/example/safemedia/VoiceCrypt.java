package com.example.safemedia;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class VoiceCrypt extends AppCompatActivity {

    private final int REQUEST_WRITE_PERMISSION = 1;
    private static final int FILE_PICKER_REQUEST_CODE = 2;
    private static final String TAG = "AudioEncryptionHelper";
    private String passw = "MyAdiPiPassword";
    private ActivityResultLauncher<String> audioFilePickerLauncher;
    private Uri selectedAudioUri;

    private Button voice_BTN_choose,voice_BTN_encrypt,voice_BTN_decrypt;
    private ImageView voice_IMG_back,voice_IMG_stop,voice_IMG_play;
    private TextView voice_TXT_title, voice_TXT_progress;
    private SeekBar voice_SEEK_progress_audio;
    private MediaPlayer player;
    private ScheduledExecutorService timer;
    private File file;
    private TextToSpeech textToSpeech;
    private String nameAudio;
    private String encrypt ;
    private String encryptedFilePath;
    private String duration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_crypt);
        findViews();
        player = new MediaPlayer();
        encrypt = null;
        encryptedFilePath = null;
        requestPermission();
        createListeners();
    }

    private void requestPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(new String[] {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
        }
    }

    private void createListeners() {
        voice_IMG_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(VoiceCrypt.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        voice_BTN_choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickSound();
            }
        });

        voice_IMG_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player != null){
                   if(player.isPlaying()){
                       player.pause();
                        timer.shutdown();
                   }else{
                       player.start();
                       timer = Executors.newScheduledThreadPool(1);
                       timer.scheduleAtFixedRate(new Runnable() {
                           @Override
                           public void run() {
                               if(player != null){
                                   if(!voice_SEEK_progress_audio.isPressed()){
                                       voice_SEEK_progress_audio.setProgress(player.getCurrentPosition());
                                   }
                               }
                           }
                       },10,10,TimeUnit.MILLISECONDS);
                   }
                }else{
                    if(selectedAudioUri != null){
                       // Toast.makeText(VoiceCrypt.this, "this = "+ selectedAudioUri, Toast.LENGTH_SHORT).show();
                        createMediaPlayer();
                    }
                }
            }
        });


        voice_IMG_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(player != null){
                    player.stop();
                    timer.shutdown();
                }
            }
        });

        voice_BTN_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                encryptAudio();
                textToSpeech.speak(encryptedFilePath.substring(2,30),TextToSpeech.QUEUE_FLUSH,null,null);

            }
        });

        voice_BTN_decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playDecryptedAudio(passw);
            }
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            }
        });

        audioFilePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri result) {
                        if (result != null) {
                            selectedAudioUri = result;
                            createMediaPlayer();
                        }
                    }
                });

        voice_SEEK_progress_audio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(player != null){
                    int millis = player.getCurrentPosition();
                    long totalSecs = TimeUnit.SECONDS.convert(millis,TimeUnit.MILLISECONDS);
                    long mins = TimeUnit.MINUTES.convert(totalSecs, TimeUnit.SECONDS);
                    long secs = totalSecs - (mins * 60);
                    voice_TXT_progress.setText(mins + ":" + secs + " / " + duration);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(player != null){
                    player.seekTo(voice_SEEK_progress_audio.getProgress());
                }
            }
        });

        voice_IMG_play.setEnabled(false);
        voice_IMG_stop.setEnabled(false);
        voice_BTN_encrypt.setEnabled(false);
        voice_BTN_decrypt.setEnabled(false);
    }


    private void encryptAudio() {
        try{
            // Read the selected audio file
            InputStream inputStream = getBaseContext().getContentResolver().openInputStream(selectedAudioUri);
            // Generate a random initialization vector (IV)
            byte[] iv = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // Generate a random salt
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);

            // Derive a secret key using PBKDF2
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec pbeKeySpec = new PBEKeySpec(passw.toCharArray(), salt, 10000, 256);
            SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

            // Create the cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            // Create the encrypted audio file
            encryptedFilePath = getEncryptedFilePath(getBaseContext());
            OutputStream outputStream = new FileOutputStream(encryptedFilePath);

            // Write the IV and salt to the encrypted file
            outputStream.write(iv);
            outputStream.write(salt);

            // Encrypt the audio data
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
            }
            cipherOutputStream.close();
            outputStream.close();

            Log.d(TAG, "Audio encryption completed. Encrypted file saved to: " + encryptedFilePath);




        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private String getEncryptedFilePath(Context baseContext) {
        File directory = baseContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
        return directory.getAbsolutePath() + File.separator + "encrypted_audio.aes";
    }

    public void playDecryptedAudio(String password) {
        try {
            // Read the encrypted audio file
            File encryptedFile = new File(encryptedFilePath);
            FileInputStream fileInputStream = new FileInputStream(encryptedFile);

            // Read the IV and salt from the encrypted file
            byte[] iv = new byte[16];
            byte[] salt = new byte[16];
            fileInputStream.read(iv);
            fileInputStream.read(salt);

            // Derive the secret key using PBKDF2
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
            SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

            // Create the cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

            // Create a temporary file to store the decrypted audio
            File tempFile = File.createTempFile("temp_audio", ".mp3", getBaseContext().getCacheDir());
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);

            // Decrypt the audio data
            CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, cipher);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            cipherInputStream.close();
            fileOutputStream.close();

            // Create a MediaPlayer instance and set the data source to the decrypted audio file
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());

            // Prepare the MediaPlayer for playback and start playing the decrypted audio
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mediaPlayer.prepareAsync();

            // Clean up the temporary file after playback is finished
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    tempFile.delete();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createMediaPlayer() {
        player = new MediaPlayer();
        player.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        try {
            player.setDataSource(getApplicationContext(),selectedAudioUri);
            player.prepare();
            getAudioName();
            voice_TXT_title.setText(nameAudio);

            voice_IMG_play.setEnabled(true);
            voice_IMG_stop.setEnabled(true);
            voice_BTN_encrypt.setEnabled(true);
            voice_BTN_decrypt.setEnabled(true);


            int milis = player.getDuration();
            long totalSec = TimeUnit.SECONDS.convert(milis,TimeUnit.MILLISECONDS);
            long mins = TimeUnit.MINUTES.convert(totalSec,TimeUnit.SECONDS);
            long secs = totalSec - (mins*60);
            duration = mins + ":" + secs;
            voice_TXT_progress.setText("00:00 / " + duration);
            voice_SEEK_progress_audio.setMax(milis);
            voice_SEEK_progress_audio.setProgress(0);

            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    releaseMediaPlayer();
                }
            });


        }catch (IOException e){
            e.printStackTrace();
        }
    }


    private void getAudioName(){
        String fileName = null;
        Cursor curser = null;
        curser = getContentResolver().query(selectedAudioUri, new String[]{
                MediaStore.Audio.AudioColumns.DISPLAY_NAME
        }, null, null, null);

        if (curser != null && curser.moveToFirst()) {
            int columnIndex = curser.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME);
            if (columnIndex >= 0) {
                fileName = curser.getString(columnIndex);
            }
        }
        if(curser != null){
            curser.close();
        }

        nameAudio = fileName;
       // Toast.makeText(this, "file name = " + nameAudio, Toast.LENGTH_SHORT).show();

    }


    private void releaseMediaPlayer() {
        if(timer != null){
            timer.shutdown();
        }
        if(player != null){
            player.release();
            player = null;
        }

        if(nameAudio != null && duration != null){
            voice_TXT_title.setText(nameAudio);
            voice_TXT_progress.setText("00:00 / " + duration);
        }else{
            voice_TXT_title.setText("Title");
            voice_TXT_progress.setText("00:00 / 00:00");
        }

        voice_SEEK_progress_audio.setMax(100);
        voice_SEEK_progress_audio.setProgress(0);
    }

    private void pickSound() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        audioFilePickerLauncher.launch("audio/*");
    }

    private void findViews() {
        voice_BTN_choose = findViewById(R.id.voice_BTN_choose);
        voice_BTN_encrypt = findViewById(R.id.voice_BTN_encrypt);
        voice_BTN_decrypt = findViewById(R.id.voice_BTN_decrypt);
        voice_IMG_back = findViewById(R.id.voice_IMG_back);
        voice_IMG_stop = findViewById(R.id.voice_IMG_stop);
        voice_IMG_play = findViewById(R.id.voice_IMG_play);
        voice_TXT_title = findViewById(R.id.voice_TXT_title);
        voice_TXT_progress = findViewById(R.id.voice_TXT_progress);
        voice_SEEK_progress_audio = findViewById(R.id.voice_SEEK_progress_audio);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        if(textToSpeech != null){
            textToSpeech.stop();
        }
    }
}