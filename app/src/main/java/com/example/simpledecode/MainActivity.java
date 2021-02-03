package com.example.simpledecode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.qrcode.detector.Detector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Button scan_btn;
    private Button upload;
    private ImageView img;
    private TextView text;
    private TextView messaege;
    private EditText username;
    private StorageReference mStorageRef;
    private Bitmap buffer;
    private int qrsize;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scan_btn = (Button)findViewById(R.id.scan_btn);
        upload = (Button)findViewById(R.id.upload_btn);
        img = (ImageView)findViewById(R.id.imageView);
        username =(EditText)findViewById(R.id.username);
        text =(TextView)findViewById(R.id.data);
        messaege = (TextView)findViewById(R.id.textView2);
        text.setMovementMethod(ScrollingMovementMethod.getInstance());
        upload.setOnClickListener(putimage);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        getPermissionsStorage();
        final Activity activity = this;
        scan_btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                text.setText("");
                messaege.setText("");
                IntentIntegrator integrator = new IntentIntegrator(activity);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt("Scan");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(true);
                integrator.initiateScan();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        if (result!= null)
        {
            if (result.getContents()==null)
            {
                Toast.makeText(this, "You cancelled the scanning", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Bitmap bitmap = BitmapFactory.decodeFile(result.getBarcodeImagePath());
                try {
                    readQRImage(bitmap);
                } catch (NotFoundException | IOException e) {
                    e.printStackTrace();
                }
                img.setImageBitmap(bitmap);
                buffer = bitmap;
                messaege.setText("Message: "+result.getContents());
                Toast.makeText(this,result.getContents(),Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void getPermissionsStorage(){
            if(ContextCompat.checkSelfPermission(this,"Manifest.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                Log.v("","");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                }
            }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[]permissions,@NonNull int[]grantresults){
        getPermissionsStorage();
    }

    public void readQRImage(Bitmap bMap) throws NotFoundException, IOException {

        int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
        //copy pixel data from the Bitmap into the 'intArray' array
        bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

        LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        BitMatrix bit;
        DetectorResult detectorResult = null;
        try {
            detectorResult = new Detector(bitmap.getBlackMatrix()).detect();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        bit=detectorResult.getBits();
        qrsize = bit.getHeight();

        for (int x =0;x < bit.getHeight();x++) {
            for (int y = 0; y < bit.getWidth(); y++) {
                if (bit.get(y, x) )
                    text.append("0");
                else
                    text.append("1");
            }
            text.append("\n");
        }
    }


    private View.OnClickListener putimage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            uploadimg(buffer);
            uploaddata();
        }
    };

    public void uploaddata(){
        String filename = username.getText().toString();
        String fileContents = text.getText().toString();
        fileContents = fileContents.replaceAll("\r|\n", "");

        Calendar mCal = Calendar.getInstance();
        CharSequence s = DateFormat.format("yyyy-MM-dd kk:mm:ss", mCal.getTime());
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference rootname = database.getReference(filename);
        DatabaseReference myRef = database.getReference(filename+"/decode");
        DatabaseReference day = database.getReference(filename+"/date");
        DatabaseReference size = database.getReference(filename+"/size");
        //rootname.child("decode").setValue(fileContents);
        //rootname.child("date").setValue(s.toString());
        size.setValue(qrsize);
        day.setValue(s.toString());
        myRef.setValue(fileContents);
    }

    public void uploadimg(Bitmap bitmap){

        String filename = username.getText().toString();

        //Uri file = Uri.fromFile(new File("path/to/images/rivers.jpg"));
        StorageReference riversRef = mStorageRef.child(filename+"/scan.jpg");

        Uri uri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, null,null));
       // Uri tempUri = getImageUri(getApplicationContext(), bitmap);

        riversRef.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                       // Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Toast.makeText(getApplicationContext(), "upload success", Toast.LENGTH_SHORT).show();
                    }
                });
                /*.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                    }
                });*/
     }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}