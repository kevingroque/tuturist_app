package com.huich.roque.app.tuturist_app;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseFirestore mFirestore;

    private String current_user_id;

    private TextView mNombreText, mNombreText2, mCoinsText, mNivelText;
    private ImageView mFotoPerfil;
    private RoundCornerProgressBar mLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNombreText = (TextView) findViewById(R.id.main_txt_collapsing_nombre);
        mNombreText2= (TextView) findViewById(R.id.main_txt_nombre_toolbar);
        mCoinsText =(TextView) findViewById(R.id.main_txt_coins);
        mNivelText = (TextView) findViewById(R.id.main_txt_level);
        mFotoPerfil = (ImageView) findViewById(R.id.main_img_profile_user);
        mLevel = (RoundCornerProgressBar) findViewById(R.id.main_progressbar_level);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient =  new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = mFirebaseAuth.getCurrentUser();
                if (user != null){
                    mFirestore.collection("users").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                if(!task.getResult().exists()){
                                    Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                                    startActivity(setupIntent);
                                    finish();
                                }else {
                                    String userName = task.getResult().getString("name");
                                    String userImage = task.getResult().getString("avatar");
                                    Double userLevel = task.getResult().getDouble("level");
                                    Double userCoins = task.getResult().getDouble("coins");

                                    Integer level = userLevel.intValue();
                                    Integer coins = userCoins.intValue();

                                    mNombreText.setText(userName);
                                    mNombreText2.setText(userName);
                                    mNivelText.setText(level.toString());
                                    mCoinsText.setText(coins.toString());
                                    Glide.with(getApplicationContext()).load(userImage).into(mFotoPerfil);
                                }

                            } else {
                                //Firebase Exception
                            }
                        }
                    });
                }else {
                    goLoginScreen();
                }
            }
        };

        mLevel.setProgressColor(Color.parseColor("#F6BD00"));
        mLevel.setProgressBackgroundColor(Color.parseColor("#FAFAFA"));
        mLevel.setMax(100);
        mLevel.setProgress(10);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null){
            goLoginScreen();
        } else {

            current_user_id = mFirebaseAuth.getCurrentUser().getUid();

            mFirestore.collection("users").document(current_user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        if(!task.getResult().exists()){
                            Intent setupIntent = new Intent(MainActivity.this, SetupActivity.class);
                            startActivity(setupIntent);
                            finish();
                        }
                    } else {
                        String errorMessage = task.getException().getMessage();
                        Toast.makeText(MainActivity.this, "Error : " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    }

    private void goLoginScreen() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthStateListener != null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    public void logOut(final View view){
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Cerrar Sesión");
        alertDialog.setMessage("Esta seguro de que quiere cerrar sesion??");
        // Alert dialog button
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Aceptar",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mFirebaseAuth.signOut();
                        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()){
                                    goLoginScreen();
                                }else{
                                    Toast.makeText(MainActivity.this,"Error al cerrrar sesion", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancelar",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Alert dialog action goes here
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    //funcion para ir a las secciones

    public void goLomas(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void goPosts(View view){
        Intent intent = new Intent(this, PostActivity.class);
        startActivity(intent);
    }

    public void goEventos(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
