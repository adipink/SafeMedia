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
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
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
    private ActivityResultLauncher<String> audioFilePickerLauncher;
    private Uri selectedAudioUri;

    private Button voice_BTN_choose,voice_BTN_encrypt;
    private ImageView voice_IMG_back,voice_IMG_stop,voice_IMG_play;
    private MediaPlayer player;
    private File file;
    private TextToSpeech textToSpeech;
    private String nameAudio;
    private String encrypt ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_crypt);
        findViews();
        player = new MediaPlayer();
        encrypt = null;
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
                   }else{
                       player.start();
                   }
                }else{
                    if(selectedAudioUri != null){
                        Toast.makeText(VoiceCrypt.this, "this = "+ selectedAudioUri, Toast.LENGTH_SHORT).show();
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
                }
            }
        });

        voice_BTN_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //encryptAudio();

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

    }

/*
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
            PBEKeySpec pbeKeySpec = new PBEKeySpec("YourPassword".toCharArray(), salt, 10000, 256);
            SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

            // Create the cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

            // Create the encrypted audio file
            String encryptedFilePath = getEncryptedFilePath(getBaseContext());
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
*/

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

    }
    private void releaseMediaPlayer() {
        if(player != null){
            player.release();
            player = null;
        }
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
        voice_IMG_back = findViewById(R.id.voice_IMG_back);
        voice_IMG_stop = findViewById(R.id.voice_IMG_stop);
        voice_IMG_play = findViewById(R.id.voice_IMG_play);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }
}