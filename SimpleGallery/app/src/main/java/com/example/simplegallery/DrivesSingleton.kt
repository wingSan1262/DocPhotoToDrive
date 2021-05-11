package com.example.simplegallery

import android.accounts.Account
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.api.services.drive.Drive

class DriveSingleton {

    companion object{
        var mDriveService : Drive? = null
        var mGoogleSignInClient : GoogleSignInClient? = null
        var obtainedAccount : Account? = null

        fun saveObject (driveService: Drive,
                        googleSignInClient: GoogleSignInClient,
                        account : Account) {
            mDriveService = driveService
            mGoogleSignInClient = googleSignInClient
            obtainedAccount = account
        }
    }
}