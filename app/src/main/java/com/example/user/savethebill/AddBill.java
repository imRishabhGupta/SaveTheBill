package com.example.user.savethebill;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddBill extends AppCompatActivity {

    Firebase firebase,ref;
    private  EditText a,c;
    private Bitmap bitmap;
    String base64Image;
    private   AutoCompleteTextView b;
    long count=7;
    String[] arr={"electricity bill","water bill","consumer bill","telephone bill","other bill"};
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;

    // directory name to store captured images and videos
    private static final String IMAGE_DIRECTORY_NAME = "Saved Bills";
    private EditText fromDateEtxt;
    private EditText toDateEtxt;

    private DatePickerDialog fromDatePickerDialog;
    private DatePickerDialog toDatePickerDialog;

    private SimpleDateFormat dateFormatter;
    Calendar cal1,cal2;

    private Uri fileUri; // file url to store image/video

    private ImageView imgPreview;
    private Button btnCapturePicture;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bill);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        b = (AutoCompleteTextView)
                findViewById(R.id.autoCompleteTextView);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this,android.R.layout.select_dialog_item, arr);

        b.setThreshold(1);
        b.setAdapter(adapter);
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);


        fromDateEtxt = (EditText) findViewById(R.id.etxt_fromdate);
        fromDateEtxt.setInputType(InputType.TYPE_NULL);

        toDateEtxt = (EditText) findViewById(R.id.etxt_todate);
        toDateEtxt.setInputType(InputType.TYPE_NULL);
        setDateTimeField();

        firebase = new Firebase("https://savethebill.firebaseio.com");
        ref = new Firebase("https://savethebill.firebaseio.com/" + firebase.getAuth().getUid());
        ref.addValueEventListener(new ValueEventListener() {
            long cd;

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                cd = snapshot.getChildrenCount();
                System.out.println("The read success: " + cd);
                getData(cd);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });

        imgPreview = (ImageView) findViewById(R.id.imageView);
        btnCapturePicture = (Button) findViewById(R.id.bu);
        btnCapturePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(AddBill.this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {


                        ActivityCompat.requestPermissions(AddBill.this,
                                new String[]{Manifest.permission.CAMERA},1);

                    }
                else if(ContextCompat.checkSelfPermission(AddBill.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(AddBill.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},3);
                }
                else
                captureImage();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    private void setDateTimeField() {
        fromDateEtxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fromDatePickerDialog.show();
            }
        });
        toDateEtxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toDatePickerDialog.show();
            }
        });

        Calendar newCalendar = Calendar.getInstance();
        fromDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                cal1 = Calendar.getInstance();

                cal1.set(year, monthOfYear, dayOfMonth);
                fromDateEtxt.setText(dateFormatter.format(cal1.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        toDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                cal2 = Calendar.getInstance();
                cal2.set(year, monthOfYear, dayOfMonth);
                toDateEtxt.setText(dateFormatter.format(cal2.getTime()));
            }

        },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if the result is capturing Image
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // successfully captured the image
                // display it in image view
                previewCapturedImage();
            } else if (resultCode == RESULT_CANCELED) {
                // user cancelled Image capture
                Toast.makeText(getApplicationContext(),
                        "User cancelled image capture", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // failed to capture image
                Toast.makeText(getApplicationContext(),
                        "Sorry! Failed to capture image", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

        // start the image capture Intent
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    public void storeImageToFirebase() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();
        base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);
        System.out.println(base64Image);

    }

    private void previewCapturedImage() {
        try {


            imgPreview.setVisibility(View.VISIBLE);

            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inSampleSize = 4;

             bitmap = BitmapFactory.decodeFile(fileUri.getPath(),
                    options);

            imgPreview.setImageBitmap(bitmap);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save file url in bundle as it will be null on screen orientation
        // changes
        outState.putParcelable("file_uri", fileUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // get the file url
        fileUri = savedInstanceState.getParcelable("file_uri");
    }

    public void add(View view){
        a=(EditText)findViewById(R.id.bill_name);
        b=(AutoCompleteTextView)findViewById(R.id.autoCompleteTextView);
        c=(EditText)findViewById(R.id.owner);

         final Movie bill=new Movie();
        bill.setBillName(a.getText().toString());
        bill.setType(b.getText().toString());
        bill.setNameofowner(c.getText().toString());
        bill.setLastdate(fromDateEtxt.getText().toString());
        bill.setGuarantee(toDateEtxt.getText().toString());
        if(bitmap!=null)
        {
        storeImageToFirebase();
        bill.setImagestring(base64Image);
        }
        else
            bill.setImagestring("");

        Intent notificationIntent = new Intent(this,AlarmReceiver.class);
        notificationIntent.setAction("android.media.action.DISPLAY_NOTIFICATION");
        notificationIntent.putExtra("name",a.getText().toString());
        notificationIntent.putExtra("type",b.getText().toString());

        setDateTimeField();

        if(cal1!=null&&cal1.getTimeInMillis()<=System.currentTimeMillis()){
            Toast.makeText(AddBill.this, "Date should be greater than current time.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(fromDateEtxt.getText().toString().length()>0) {
          setRepeatingAlarm(notificationIntent,cal1);
        }
        if(cal2!=null&&cal2.getTimeInMillis()<=System.currentTimeMillis()){
            Toast.makeText(AddBill.this, "Date should be greater than current time.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(toDateEtxt.getText().toString().length()>0) {
            setRepeatingAlarm(notificationIntent,cal2);
        }
        System.out.println("got"+count);

        ref.child("Bill"+count).setValue(bill);
        Toast.makeText(getApplicationContext(),"Bill added successfully.",Toast.LENGTH_SHORT).show();

        Intent i=new Intent(getApplicationContext(),AllBills.class);
        startActivity(i);
    }

    public void setRepeatingAlarm(Intent notificationIntent, Calendar cal) {

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        notificationIntent.putExtra("daysleft",cal.getTime().getTime());
        Log.d("addbill",String.valueOf(cal.getTime().getTime()));
        int id = (int) System.currentTimeMillis();
        notificationIntent.putExtra("id",id);

        PendingIntent broadcast2 = PendingIntent.getBroadcast(getApplicationContext(), id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calx = Calendar.getInstance();
        calx.set(Calendar.HOUR_OF_DAY, 10);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calx.getTimeInMillis(), 4 * 24 * 60 * 60 * 1000, broadcast2);

        Intent cancellationIntent = new Intent(this, CancelAlarmBroadcastReceiver.class);
        cancellationIntent.setAction("android.media.action.DISPLAY_NOTIFICATION");

        Log.d("AddBill","ABOUT TO REACH");
        cancellationIntent.putExtra("key",broadcast2);
        cal.set(Calendar.HOUR_OF_DAY,10);

        PendingIntent cancellationPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), id, cancellationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.set(AlarmManager.RTC, cal.getTimeInMillis(),cancellationPendingIntent);
    }
    public void getData(long c){
        count=c;
    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        }
        else {
            return null;
        }

        return mediaFile;
    }
}
