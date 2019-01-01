package com.huich.roque.app.tuturist_app;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText mEmail, mPassword, mConfirmPass;
    private ImageButton mBtnRegister;
    private Button mGoSignInBtn;
    private SpinKitView mProgressBar;

    private FirebaseAuth mFirebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mFirebaseAuth = FirebaseAuth.getInstance();

        mEmail = (EditText) findViewById(R.id.edt_register_email);
        mPassword = (EditText) findViewById(R.id.edt_register_password);
        mConfirmPass = (EditText) findViewById(R.id.edt_register_confirmpass);
        mBtnRegister = (ImageButton) findViewById(R.id.btn_register_signup);
        mGoSignInBtn = (Button) findViewById(R.id.btn_register_signin);
        mProgressBar = (SpinKitView) findViewById(R.id.spinkit_register_progress);

        mBtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
        if(currentUser != null){
            sendToMain();
        }
    }

    private void registerUser(){
        String email = mEmail.getText().toString().trim();
        String password = mPassword.getText().toString();
        String confirmpass = mConfirmPass.getText().toString();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(confirmpass)){
            if (password.equals(confirmpass)){
                mProgressBar.setVisibility(View.VISIBLE);
                mEmail.setEnabled(false);
                mPassword.setEnabled(false);
                mConfirmPass.setEnabled(false);
                mBtnRegister.setEnabled(false);
                mGoSignInBtn.setEnabled(false);

                mFirebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Intent setupIntet = new Intent(RegisterActivity.this,SetupActivity.class);
                            setupIntet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(setupIntet);
                            finish();
                        }else {
                            String errorMessage = task.getException().getMessage();
                            Toast.makeText(RegisterActivity.this, "Error : " + errorMessage, Toast.LENGTH_LONG).show();
                        }

                        mProgressBar.setVisibility(View.GONE);
                        mEmail.setEnabled(true);
                        mPassword.setEnabled(true);
                        mConfirmPass.setEnabled(true);
                        mBtnRegister.setEnabled(true);
                        mGoSignInBtn.setEnabled(true);
                    }
                });
            }else {
                Toast.makeText(RegisterActivity.this, "Confirm Password and Password Field doesn't match.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void goLoginScreen(View view) {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    private void sendToMain() {
        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}
