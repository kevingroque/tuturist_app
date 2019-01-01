package com.huich.roque.app.tuturist_app;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class SetupActivity extends AppCompatActivity {

    private EditText mName , mLastName, mBirthdate;
    private Spinner mChooseCountry;
    private RadioButton mRbGenderMale, mRbGenderFemale, mRbGenderOther, mRbGender;
    private RadioGroup mRgGender;
    private ImageButton mBtnSetup;
    private ProgressBar mSetupProgress;
    private CircleImageView mSetupImage;

    private Uri mMainImageURI = null;
    private String user_id, mGenderSelected;
    private int mYear, mMonth, mDay, mHour, mMinute;
    private boolean isChanged = false;

    private StorageReference mStorageReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseFirestore mFirebaseFirestore;

    private Bitmap compressedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mFirebaseAuth = FirebaseAuth.getInstance();
        user_id = mFirebaseAuth.getCurrentUser().getUid();

        mFirebaseFirestore = FirebaseFirestore.getInstance();
        mStorageReference = FirebaseStorage.getInstance().getReference();

        mName = (EditText) findViewById(R.id.edt_setup_name);
        mLastName = (EditText) findViewById(R.id.edt_setup_lastname);
        mBirthdate = (EditText) findViewById(R.id.edt_setup_bithday);
        mChooseCountry = (Spinner) findViewById(R.id.spinner_setup_choosecountry);
        mRgGender = (RadioGroup) findViewById(R.id.rg_setup_gender);
        mRbGenderMale = (RadioButton) findViewById(R.id.rb_setup_gender_male);
        mRbGenderFemale = (RadioButton) findViewById(R.id.rb_setup_gender_female);
        mRbGenderOther = (RadioButton) findViewById(R.id.rb_setup_gender_other);
        mBtnSetup = (ImageButton) findViewById(R.id.img_setup_start);
        mSetupImage = (CircleImageView) findViewById(R.id.img_setup_profile);
        mSetupProgress = (ProgressBar) findViewById(R.id.pb_setup_progressbar);

        mChooseCountry.setOnItemSelectedListener(new CustomOnItemSelectedListener());

        mSetupProgress.setVisibility(View.VISIBLE);
        mBtnSetup.setEnabled(false);

        mFirebaseFirestore.collection("users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    if (task.getResult().exists()){
                        String getName = task.getResult().getString("name");
                        String getImage = task.getResult().getString("avatar");
                        String getLastName = task.getResult().getString("lastname");
                        String getBirthdate = task.getResult().getString("birthdate");

                        mMainImageURI = Uri.parse(getImage);

                        mName.setText(getName);
                        mLastName.setText(getLastName);
                        mBirthdate.setText(getBirthdate);

                        RequestOptions placeholderRequest = new RequestOptions();
                        placeholderRequest.placeholder(R.drawable.default_image_profile);

                        Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholderRequest).load(getImage).into(mSetupImage);


                    }else {
                        Toast.makeText(SetupActivity.this, "(FIRESTORE Retrieve Error)", Toast.LENGTH_LONG).show();
                    }

                    mSetupProgress.setVisibility(View.INVISIBLE);
                    mBtnSetup.setEnabled(true);
                }
            }
        });

        mBtnSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String user_name = mName.getText().toString();
                final String user_lastname = mLastName.getText().toString();
                final String user_birthdate = mBirthdate.getText().toString();
                final String user_country = mChooseCountry.getSelectedItem().toString();
                final String user_gender = mGenderSelected;
                final int user_level = 0;
                final int user_coins = 0;

                if (!TextUtils.isEmpty(user_name)
                        && !TextUtils.isEmpty(user_lastname)
                        && !TextUtils.isEmpty(user_birthdate)
                        && mMainImageURI != null){

                    mSetupProgress.setVisibility(View.VISIBLE);

                    if (isChanged){
                        user_id = mFirebaseAuth.getCurrentUser().getUid();
                        File newImageFile = new File(mMainImageURI.getPath());
                        try {

                            compressedImageFile = new Compressor(SetupActivity.this)
                                    .setMaxHeight(125)
                                    .setMaxWidth(125)
                                    .setQuality(50)
                                    .compressToBitmap(newImageFile);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] thumbData = baos.toByteArray();

                        UploadTask image_path = mStorageReference.child("avatar").child(user_id + ".jpg").putBytes(thumbData);

                        image_path.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    storeFirestore(task, user_name, user_lastname, user_birthdate,user_country, user_gender, user_level, user_coins);

                                } else {

                                    String error = task.getException().getMessage();
                                    Toast.makeText(SetupActivity.this, "(IMAGE Error) : " + error, Toast.LENGTH_LONG).show();

                                    mSetupProgress.setVisibility(View.INVISIBLE);

                                }

                            }
                        });
                    }else {
                        storeFirestore(null, user_name, user_lastname, user_birthdate, user_country,user_gender, user_level,user_coins);
                    }
                }
                finish();
            }
        });

        mSetupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if(ContextCompat.checkSelfPermission(SetupActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                        Toast.makeText(SetupActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    } else {

                        BringImagePicker();

                    }

                } else {

                    BringImagePicker();

                }

            }

        });

    }

    public class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        String firstItem = String.valueOf(mChooseCountry.getSelectedItem());

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (firstItem.equals(String.valueOf(mChooseCountry.getSelectedItem()))) {
                // ToDo when first item is selected
            } else {
                Toast.makeText(parent.getContext(),
                        "You have selected : " + parent.getItemAtPosition(pos).toString(),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg) {
        }
    }

    public void radioButtonClicked(View view){
        boolean checked = ((RadioButton)view).isChecked();
        switch (view.getId()){
            case R.id.rb_setup_gender_male:
                mGenderSelected = "Male";
                break;
            case R.id.rb_setup_gender_female:
                mGenderSelected = "Female";
                break;
            case R.id.rb_setup_gender_other:
                mGenderSelected = "Other";
                break;
        }
    }

    private void storeFirestore(@NonNull Task<UploadTask.TaskSnapshot> task,
                                String user_name, String user_lastname, String user_birhtdate, String user_country,String user_gender, int user_level, int user_coins) {

        Uri download_uri;

        if(task != null) {
            download_uri = task.getResult().getDownloadUrl();
        } else {
            download_uri = mMainImageURI;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", user_name);
        userMap.put("lastname", user_lastname);
        userMap.put("birthdate", user_birhtdate);
        userMap.put("country", user_country);
        userMap.put("birthdate", user_birhtdate);
        userMap.put("avatar", download_uri.toString());
        userMap.put("level", user_level);
        userMap.put("coins", user_coins);

        mFirebaseFirestore.collection("users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful()){

                    Toast.makeText(SetupActivity.this, "The user Settings are updated.", Toast.LENGTH_LONG).show();
                    Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();

                } else {

                    String error = task.getException().getMessage();
                    Toast.makeText(SetupActivity.this, "(FIRESTORE Error) : " + error, Toast.LENGTH_LONG).show();

                }

                mSetupProgress.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void BringImagePicker() {

        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(SetupActivity.this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mMainImageURI = result.getUri();
                mSetupImage.setImageURI(mMainImageURI);

                isChanged = true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();

            }
        }

    }

    //Mostrar Dialog Fecha
    public void showDatePickerDialog(View view) {
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);


        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {

                        mBirthdate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);

                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }
}
