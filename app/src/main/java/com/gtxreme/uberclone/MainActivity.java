package com.gtxreme.uberclone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gtxreme.uberclone.Model.User;
import com.rengwuxian.materialedittext.MaterialEditText;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

public class MainActivity extends AppCompatActivity {
    Button btnSignIn,btnRegister;
    RelativeLayout rootLayout;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder().setDefaultFontPath("fonts/Arkhip_font.ttf").setFontAttrId(R.attr.fontPath).build())).build());
        setContentView(R.layout.activity_main);

        btnSignIn=findViewById(R.id.btnSignIn);
        btnRegister=findViewById(R.id.btnRegister);
        rootLayout=findViewById(R.id.rootLayout);

        auth=FirebaseAuth.getInstance();
        db=FirebaseDatabase.getInstance();
        users=db.getReference("Users");

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterDialog();
            }
        });
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });


    }

    private void showLoginDialog() {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("SIGN IN");
        dialog.setMessage("Please Use Email to Sign In");

        LayoutInflater inflater=LayoutInflater.from(this);
        View login_layout=inflater.inflate(R.layout.layout_login,null);

        final MaterialEditText edtEmail=login_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword=login_layout.findViewById(R.id.edtPassword);


        dialog.setView(login_layout);

        dialog.setPositiveButton("SIGN IN ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                btnSignIn.setEnabled(false);

                if (TextUtils.isEmpty(edtEmail.getText().toString())) {
                    Snackbar.make(rootLayout, "Please Enter Email Address", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(edtPassword.getText().toString())) {
                    Snackbar.make(rootLayout, "Please Enter Password", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (edtPassword.getText().toString().length() < 6) {
                    Snackbar.make(rootLayout, "Password Too Short", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                auth.signInWithEmailAndPassword(edtEmail.getText().toString(),edtPassword.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        startActivity(new Intent(MainActivity.this,Weclome.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(rootLayout, "Failed"+e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        btnSignIn.setEnabled(true);
                    }
                });
            }});


        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }



    private void showRegisterDialog() {
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setTitle("REGISTER");
        dialog.setMessage("Please Use Email to Register");

        LayoutInflater inflater=LayoutInflater.from(this);
        View register_layout=inflater.inflate(R.layout.layout_register,null);

        final MaterialEditText edtEmail=register_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword=register_layout.findViewById(R.id.edtPassword);
        final MaterialEditText edtName=register_layout.findViewById(R.id.edtName);
        final MaterialEditText edtPhone=register_layout.findViewById(R.id.edtPhone);

        dialog.setView(register_layout);

        dialog.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    if(TextUtils.isEmpty(edtEmail.getText().toString()))
                {
                    Snackbar.make(rootLayout,"Please Enter Email Address",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(edtPassword.getText().toString()))
                {
                    Snackbar.make(rootLayout,"Please Enter Password",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(edtName.getText().toString()))
                {
                    Snackbar.make(rootLayout,"Please Enter Name",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(edtPhone.getText().toString()))
                {
                    Snackbar.make(rootLayout,"Please Enter Phone",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if(edtPassword.getText().toString().length()<6)
                {
                    Snackbar.make(rootLayout,"Password Too Short",Snackbar.LENGTH_SHORT).show();
                    return;
                }
                auth.createUserWithEmailAndPassword(edtEmail.getText().toString(),edtPassword.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        User user = new User();
                        user.setEmail(edtEmail.getText().toString());
                        user.setPassword(edtPassword.getText().toString());
                        user.setName(edtName.getText().toString());
                        user.setPhone(edtPhone.getText().toString());

                        users.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Snackbar.make(rootLayout,"Registered Successfully",Snackbar.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(rootLayout,"Register Unsuccesfull..Please Try Again! "+e.getMessage(),Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(rootLayout,"Failed "+e.getMessage(),Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
            }
        });
         dialog.show();

    }
}
