package com.example.simplegallery

import android.accounts.Account
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.http.FileContent
import com.google.gson.Gson
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
    var mGoogleSignInAccount : GoogleSignInAccount? = null

    var mContext : Context? = null

    var mHandler : Handler? = null

    var currentDialog : AlertDialog? = null

    var mSharedPreference : SharedPreferences? = null
//    var mGoogleSignInClient : GoogleSignInAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        mContext = this
        buildSignInClient()

        mHandler = object : Handler(this.mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    ConstantsDefine().DRIVE_CREATE_FOLDER_AND_FILE_TOAST -> {
                        showToastLong(msg.obj as String)
                    }

                    ConstantsDefine().DRIVE_CREATE_FOLDER_AND_FILE -> {
                        createFolder()
                    }

                    ConstantsDefine().DRIVE_CREATE_FOLDER_AND_FILE_SUCCEED -> {
                        mHandler?.sendEmptyMessage(ConstantsDefine().DRIVE_ACCOUNT_REQUEST_FINISH)
                    }

                    ConstantsDefine().DRIVE_ACCOUNT_REQUEST -> {
                        val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(mContext)
                        val view = layoutInflater.inflate(R.layout.loading_fragment, null)
                        view.findViewById<TextView>(R.id.text_info).setText("Signing in :) . . .")
                        alertDialogBuilder.setView(view)
                        currentDialog = alertDialogBuilder.show()
                        currentDialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        requestSignIn()
                    }

                    ConstantsDefine().DRIVE_ACCOUNT_REQUEST_FINISH -> {
                        val view = View.inflate(this@SignInActivity2, R.layout.loading_fragment, null)
                        view.findViewById<TextView>(R.id.text_info).setText("Signed In !")
                        currentDialog?.setView(view)
                        saveToSharedPreference()
                        val intent = Intent(mContext, GalleryActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }

        sign_in_button.setOnClickListener {
            // request sign in
            mHandler?.sendEmptyMessage(ConstantsDefine().DRIVE_ACCOUNT_REQUEST)
        }
        if(isSignedInAndInitializeAccount()){
            // signed-in
            setButtonSignedIn()
            val alertDialogBuilder : AlertDialog.Builder = AlertDialog.Builder(mContext)
            val view = layoutInflater.inflate(R.layout.loading_fragment, null)
            view.findViewById<TextView>(R.id.text_info).setText("Signing in :) . . .")
            alertDialogBuilder.setView(view)
            val dialog = alertDialogBuilder.show()
            dialog.setCanceledOnTouchOutside(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        log_out.setOnClickListener {
            logOut()
            setButtonSignedOut()
        }



        test_manipulate_drive.setOnClickListener {
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
                // signed in
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
        mGoogleSignInAccount = it
        credential.selectedAccount = it.account

        val driveService = Drive.Builder(NetHttpTransport(), GsonFactory(),
            credential)
            .setApplicationName("Simple Gallery")
            .build()
        mDriveService = driveService
        mHandler?.sendEmptyMessage(ConstantsDefine().DRIVE_CREATE_FOLDER_AND_FILE)
    }

    fun buildSignInClient(){
        val signInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val signInClient = GoogleSignIn.getClient(this.applicationContext, signInOption)
        mGoogleSignInClient = signInClient
    }

    fun requestSignIn(){
        startActivityForResult(mGoogleSignInClient?.signInIntent, 400)
    }

    fun createFolder(){

        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {

            // list all the folder
            var success : Boolean = true
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

            if(targetFolderName == null) {
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
                    success = false
                }
            }
            if(success){
                mHandler?.sendEmptyMessage(ConstantsDefine().DRIVE_CREATE_FOLDER_AND_FILE_SUCCEED)
            }
        }
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

    fun saveToSharedPreference(){
//        mSharedPreference = getPreferences(Context.MODE_PRIVATE)
//        val editor = mSharedPreference!!.edit()
//        val mGson = Gson()
//
//        val driveService = mGson.toJson(mDriveService)
//        val googleSignInClient = mGson.toJson(mGoogleSignInClient)
//        editor.putString(ConstantsDefine().DRIVE_SERVICE_OBJECT_SHARED_PREF, driveService)
//        editor.putString(ConstantsDefine().GOOGLE_SIGNIN_CLIENT_OBJECT_SHARED_PREF, googleSignInClient)
//        editor.apply()
        DriveSingleton.saveObject(mDriveService as Drive,
            mGoogleSignInClient as GoogleSignInClient,
            mGoogleSignInAccount?.account as Account)
    }

}
