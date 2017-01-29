package com.mobile.persson.agrohorta.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
//import com.facebook.FacebookSdk;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mobile.persson.agrohorta.R;
import com.mobile.persson.agrohorta.models.PlantModel;
import com.mobile.persson.agrohorta.utils.ImageHelper;
import com.mobile.persson.agrohorta.utils.StringHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.bitmap;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private GoogleApiClient mGoogleApiClient;

    private ProgressDialog mProgressDialog;
    private final static String TAG = "LFSP_DEBUG";
    FirebaseStorage mStorage;
    StorageReference mStorageRef;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private String mDeviceLanguage;
    private String mNodeDatabase;
    private String mNodeLanguage;
    private String mNodePlantList;


    private List<PlantModel> mPlantList = new ArrayList<>();
    private List<Bitmap> mImageList = new ArrayList<>();

    @Bean
    ConfigApp configApp;
    @ViewById
    Toolbar toolbar;
    @ViewById
    TextView tvToolbarTitle;
    @ViewById
    de.hdodenhof.circleimageview.CircleImageView ivProfile;

    @AfterViews
    void initialize() {

        startDialog();
        initializeFirebase();
        setAuthStateListener();
        googleAuthConfig();
        loadToolbar();

        //FacebookSdk.sdkInitialize(getApplicationContext());

        //teste();

        mProgressDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            UserInfo user = FirebaseAuth.getInstance().getCurrentUser();
            loadImageProfile(user.getPhotoUrl());
            Toast.makeText(this, user.getDisplayName(), Toast.LENGTH_SHORT).show();
        } else
            ivProfile.setImageResource(R.drawable.ic_account_circle_white_48dp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        tvToolbarTitle.setText("Catálogo de plantas");
    }

    private void initializeFirebase() {
        //set Firebase nodes
        mNodeDatabase = getString(R.string.node_database);
        mNodePlantList = getString(R.string.node_plant_list);
        mNodeLanguage = getString(R.string.node_language) + configApp.getLanguageDevice();

        //set Firebase references
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();


        Bitmap bitmap = new ImageHelper(getApplicationContext()).
                setFileName("tomato.png").
                setDirectoryName("images").
                load();

        int i = bitmap.getHeight();
        ImageView image = (ImageView) findViewById(R.id.ivTeste);

        image.setImageBitmap(Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(),
                bitmap.getHeight(), false));


        //Set Firebase Storage
        mStorageRef = mStorage.getReferenceFromUrl(getString(R.string.firebase_storage_url));

        //getPlantList();
        //getImagesFromFirebase();


        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

/*        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInAnonymously", task.getException());
                            Toast.makeText(getApplicationContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });*/

    }

    private void googleAuthConfig() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    private void setAuthStateListener() {
        mAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    if (user.getDisplayName() != null)
                        Toast.makeText(getApplicationContext(), user.getDisplayName(), Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    private void startDialog() {
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setTitle("buscando data");
        mProgressDialog.setMessage("aguarde");
        mProgressDialog.show();
    }

    private void loadImageProfile(Uri url) {
        Glide.with(ivProfile.getContext())
                .load(url)
                .into(ivProfile);
    }

    private void teste() {
        StorageReference imagesRef = mStorageRef.child("images");
        imagesRef = imagesRef.child("onion.png");

        final long ONE_MEGABYTE = 1024 * 1024;
        imagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
                int i = 0;

                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                ImageView image = (ImageView) findViewById(R.id.ivIcon);

                image.setImageBitmap(Bitmap.createScaledBitmap(bmp, image.getWidth(),
                        image.getHeight(), false));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                int i = 0;
            }
        });
    }

    @Background
    protected void getPlantList() {
        mDatabase.child(mNodeDatabase).child(mNodeLanguage).child(mNodePlantList)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            PlantModel item = data.getValue(PlantModel.class);

                            PlantModel plant = new PlantModel();
                            plant.setPlantName(item.getPlantName());
                            plant.setPlantImage(item.getPlantImage());

                            mPlantList.add(plant);
                        }

                        getImagesFromFirebase();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //TODO tratar erros
                    }
                });
    }

    private void getImagesFromFirebase() {
        final long ONE_MEGABYTE = 1024 * 1024;

        for (final PlantModel plant : mPlantList) {
            StorageReference imagesRef = mStorageRef.child(getString(R.string.folder_images));
            imagesRef = imagesRef.child(plant.getPlantImage());

            imagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    // Data for "images/island.jpg" is returns, use this as needed
                    int i = 0;

                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    new ImageHelper(getApplicationContext()).
                            setFileName(plant.getPlantImage()).
                            setDirectoryName("images").
                            save(bitmap);

                    //keep image in a array
                    mImageList.add(BitmapFactory.decodeResource(getResources(),
                            getResources().getIdentifier("itm" + i, "drawable", getPackageName())));

/*                    ImageView image = (ImageView) findViewById(R.id.ivIcon);

                    image.setImageBitmap(Bitmap.createScaledBitmap(bmp, image.getWidth(),
                            image.getHeight(), false));*/
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //TODO Handle any errors
                    int i = 0;
                }
            });
        }

    }

    @Click
    void ivProfile() {
        LoginActivity_.intent(getApplicationContext())
                .flags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .start();
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
