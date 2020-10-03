package com.example.urbansocial;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.LoginFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


public class ProfileActivity extends AppCompatActivity {
    //Declare instances of the views
    private EditText profUserName;
    private ImageButton imageButton;
    private Button doneBtn;
    //Declare an instance of the firebase authentication
    private FirebaseAuth mAuth;
    //Declare an instance of the database reference where we will all be saving the
    // profile photo and custom display name
    private DatabaseReference mDatabaseUser;
    //declare an instance of the storage reference where we will upload the photo
    private StorageReference mStorageRef;
    //Declare an instance of URI for getting the image from our phone,
    //initialize it to null
    private Uri profileImageUri = null;
    /*since we want to get a result (getting and setting image) we will start the implicit
    * intent using the method startActivityDorResult() */

    //startActivityForResult() method will require two arguments the intent and the request code
    //Declare and initialize a private final static int that will serve as our request code
    private final static int GALLERY_REQ = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        //inflate the tool bar
        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        //initialize the instances of the views
        profUserName = findViewById(R.id.profUserName);
        imageButton = findViewById(R.id.imagebutton);
        doneBtn = findViewById(R.id.doneBtn);
        //Initialize the instance of firebase authentication
        mAuth = FirebaseAuth.getInstance();
        //We want to set the profile for specific, hence get user id of the current
        //user and assign it to a string variable
        final String userID = mAuth.getCurrentUser().getUid();
        //Initialize the database reference where you have your registered users and get the specific user reference using the user ID
        mDatabaseUser =
                FirebaseDatabase.getInstance().getReference().child("Users").child(userID);
//Initialze the firebase storage reference where you will profile photo images
        mStorageRef = FirebaseStorage.getInstance().getReference().child("profile_images");
        //set on click listener on the image button so as to allow users to pick their profile photo from their gallery
        imageButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //create an implicit intent for getting the images
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                //set the type to images only
                galleryIntent.setType("image/*");
                //since we need results, use the method startActivityForResult() and pass the intent and request code you initialized
                startActivityForResult(galleryIntent, GALLERY_REQ);

            }
        });
        //on clicking the images we want to get the name and the profile photo. then later save
        //this on a database reference for a specific user
        doneBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //get the custom display name entered by the user
                final String name = profUserName.getText().toString().trim();
                //validate to ensure that the name and profile image are not null
                if(!TextUtils.isEmpty(name) && profileImageUri !=null){
                    //create storage reference node, inside profile_iamge storage reference where
                    // you will save the profile image
                    StorageReference profileImagePath =
                            mStorageRef.child("profile_images").child(profileImageUri.getLastPathSegment());
                    /* call the putFile() method passing the profile image the user set on the storage
                    reference where you are uploading the image
                    further call addOnSuccessListener on the reference on kisten if the upload task was successful,
                    and get a snapshot of the task
                    * */

                    profileImagePath .putFile(profileImageUri ).addOnSuccessListener(new
                            OnSuccessListener<UploadTask.TaskSnapshot>(){
                        @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot){
                            //if the upload of the profile image was successful get the download uri
                            if(taskSnapshot.getMetadata()!=null){
                            if(taskSnapshot.getMetadata().getReference()!=null){
                                    //get the download url from your storage, use the methods getStorage() and getDownloadUrl()
                                   Task<Uri> result=taskSnapshot.getStorage().getDownloadUrl();
                                   //call the method addOnclickListener to determine if we get the download url
                                    result.addOnSuccessListener(new OnSuccessListener<Uri>(){
                                        @Override
                                        public void onSuccess(Uri uri){
                                            //convert the uri to a string on success
                                            final String profileImage = uri.toString();
                                            //call the method push() to add values on the database reference of a specific user
                                            mDatabaseUser.push();
                                            //call the method addValueEventListener to publish the additions in the database reference of a specific user
                                            mDatabaseUser.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    //add the profilePhoto and displayName for the current user
                                                    mDatabaseUser.child("displayName").setValue(name);
                                                    mDatabaseUser.child("profilePhoto").setValue(profileImage).addOnCompleteListener(new
                                                         OnCompleteListener<Void>() {
                                                         @Override
                                                         public void onComplete(@NonNull Task<Void> task) {
                                                             if(task.isSuccessful()){
                                                                 //show a toast to indicate the profile was updated
                                                                 Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                                                                 //launch the login activity
                                                                 Intent login = new Intent(ProfileActivity.this, LoginActivity.class);
                                                                 startActivity(login);

                                                                              }
                                                                          }
                                                                     });
                                                                  }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }
        });
    }


    @Override
    //Override this method to get the profile image set in the image button view
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GALLERY_REQ && resultCode == RESULT_OK){
            //get the image selected by the user
            profileImageUri = data.getData();
            //set in the image button view
            imageButton.setImageURI(profileImageUri);
        }
    }

}
