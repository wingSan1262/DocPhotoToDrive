package com.example.simplegallery

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.android.synthetic.main.activity_sign_in.*
import java.util.*
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.*
import java.lang.Exception


class SignInActivity2 : AppCompatActivity() {

    /**
     * Important Params
     * 3 big flow for intializing drive account connection
     * 1. Create mGoogleSignInClient
     * 2. Create mGoogleSignInClient 2 condition, when not logged in or already logged
     */
    var mDriveService : Drive? = null
    var mGoogleSignInClient : GoogleSignInClient? = null

    var mContext : Context? = null

    var mHandler : Handler? = null
//    var mGoogleSignInAccount : GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        mContext = this
        buildSignInClient()

        sign_in_button.setOnClickListener {
            requestSignIn()
        }
        if(isSignedInAndInitializeAccount()){
            setButtonSignedIn()
        }

        log_out.setOnClickListener {
            logOut()
            setButtonSignedOut()
        }

        mHandler = object : Handler(this.mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    ConstantsDefine().DRIVE_CREATE_FOLDER_AND_FILE -> {
                        showToastLong(msg.obj as String)
                    }
                }
            }
        }

        test_manipulate_drive.setOnClickListener {

            val scope = CoroutineScope(Job() + Dispatchers.IO)
            scope.launch {
                val fileMetadata = File()
                fileMetadata.setName("QuickDocumentDrive")
                fileMetadata.setMimeType("application/vnd.google-apps.folder")

                var mFile : File? = null
                try {
                    val file : File = mDriveService?.files()?.create(fileMetadata)
                        ?.setFields("id")
                        ?.execute() as File
                    mFile = file
                } catch (e : Exception){
                    print(e)
                }
                val message = mHandler?.obtainMessage(ConstantsDefine().DRIVE_CREATE_FOLDER_AND_FILE, "File id : " + mFile?.id)
                message?.let { it1 -> mHandler?.sendMessage(it1) }
            }
        }

        upload_file.setOnClickListener {
            val scope = CoroutineScope(Job() + Dispatchers.IO)
            scope.launch {

                // list all the folder
                var targetFolderName : String? = null
                var targetFolderId : String? = null

                var pageToken : String? = null
                do {
                    val result = mDriveService?.files()?.list()
                        ?.setQ("mimeType='application/vnd.google-apps.folder'")
                        ?.setSpaces("drive")
                        ?.setFields("nextPageToken, files(id, name)")
                        ?.setPageToken(pageToken)
                        ?.execute()
                    if (result != null) {
                        for (file in result.files){
                            System.out.printf("Found file: %s (%s)\n",
                                file.getName(), file.getId());
                            if (file.name.equals("QuickDocumentDrive")){
                                targetFolderId = file.id
                                targetFolderName = file.name
                                pageToken = null
                                break
                            } else {
                                pageToken = result.nextPageToken
                            }
                        }
                    }
                } while (pageToken != null)

                // create file
                val fileMetaData = File()
                fileMetaData.setName("testPhoto.jpg")
                fileMetaData.setParents(Collections.singletonList(targetFolderId))
                val filePath = java.io.File(getExternalFilesDir(null).toString()+"/20190821214901.jpg")
                val mediaContent = FileContent("image/jpeg", filePath)
                val file = mDriveService?.files()?.create(fileMetaData, mediaContent)
                    ?.setFields("id, parents")
                    ?.execute()
                System.out.println("File ID: " + file?.getId());
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            400 -> {
                if (resultCode == Activity.RESULT_OK){
                    handleSignInIntent(data as Intent)
                }
            }
        }
    }

    fun handleSignInIntent(intent : Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(intent)
            .addOnSuccessListener {
                createDriveService(it)
                setButtonSignedIn()
            }
            .addOnFailureListener {
            }
    }

    fun createDriveService(it: GoogleSignInAccount){
        val credential = GoogleAccountCredential.usingOAuth2(this,
            Collections.singleton(DriveScopes.DRIVE_FILE))
        val checkAccount = it.account
        credential.selectedAccount = it.account

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(),
            credential)
            .setApplicationName("Simple Gallery")
            .build()
        mDriveService = driveService
    }

    fun buildSignInClient(){
        val signInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val signInClient = GoogleSignIn.getClient(this, signInOption)
        mGoogleSignInClient = signInClient
    }

    fun requestSignIn(){
        startActivityForResult(mGoogleSignInClient?.signInIntent, 400)
    }

    fun createFolder(){
    }

    fun showToastLong(message: String){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun logOut() {
        mGoogleSignInClient?.signOut()
        mDriveService = null
    }

    fun isSignedInAndInitializeAccount (): Boolean {
        GoogleSignIn.getLastSignedInAccount(this).let {
            if (it == null){
                return false
            }
            createDriveService(it)
            return true
        }
    }

    fun setButtonSignedOut(){
        sign_in_button.visibility = View.VISIBLE
        sign_in_status.setText("You're signed out.")
    }


    fun setButtonSignedIn(){
        sign_in_button.visibility = View.INVISIBLE
        sign_in_status.setText("You're signed in.")
    }

}
