package com.example.safemedia;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PhotoCrypt extends AppCompatActivity {

    private Button photo_BTN_encrypt,photo_BTN_decrypt,photo_BTN_copy;
    private ImageView photo_IMG_back,photo_IMG_image;
    private EditText photo_EDT_text;
    private String imageText;
    private ClipboardManager clipboardManager;
    private ActivityResultLauncher<String> photoPickerLauncher;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_crypt);
        findViews();

        photo_EDT_text.setEnabled(false);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        createListeners();
    }

    private void createListeners() {
        photo_IMG_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PhotoCrypt.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        photo_BTN_encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(PhotoCrypt.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(PhotoCrypt.this, new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, 100);
                }
                else{
                   selectPhoto();
                }
            }
        });


        photo_BTN_decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = Base64.decode(photo_EDT_text.getText().toString(), Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                photo_IMG_image.setImageBitmap(bitmap);
            }
        });

        photo_BTN_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String codes = photo_EDT_text.getText().toString().trim();
                if(!codes.isEmpty()){
                    ClipData temp = ClipData.newPlainText("text", codes);
                    clipboardManager.setPrimaryClip(temp);
                    Toast.makeText(PhotoCrypt.this,"Copied to Clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        // Handle the selected photo URI here
                        Bitmap bitmap;
                        ImageDecoder.Source source = null;

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                            source = ImageDecoder.createSource(PhotoCrypt.this.getContentResolver(),uri);

                            try{
                                bitmap = ImageDecoder.decodeBitmap(source);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100,stream);
                                byte[] bytes = stream.toByteArray();
                                imageText = Base64.encodeToString(bytes, Base64.DEFAULT);
                                photo_EDT_text.setText(imageText);
                                photo_IMG_image.setImageDrawable(ContextCompat.getDrawable(PhotoCrypt.this, R.drawable.image));
                                Toast.makeText(PhotoCrypt.this,"Image encrypted!",Toast.LENGTH_SHORT).show();

                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
        );
    }

    private void selectPhoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        photoPickerLauncher.launch("image/*");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            selectPhoto();
        }else{
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void findViews() {
        photo_BTN_encrypt = findViewById(R.id.photo_BTN_encrypt);
        photo_BTN_decrypt = findViewById(R.id.photo_BTN_decrypt);
        photo_BTN_copy = findViewById(R.id.photo_BTN_copy);
        photo_IMG_back = findViewById(R.id.photo_IMG_back);
        photo_IMG_image = findViewById(R.id.photo_IMG_image);
        photo_EDT_text = findViewById(R.id.photo_EDT_text);
    }
}