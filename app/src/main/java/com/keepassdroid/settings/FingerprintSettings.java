package com.keepassdroid.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.LockingActivity;
import com.keepassdroid.LockingClosePreferenceActivity;
import com.keepassdroid.utils.Fingerprint;

import org.w3c.dom.Text;

import javax.crypto.Cipher;

public class FingerprintSettings extends LockingClosePreferenceActivity {

    SharedPreferences prefs;
    private Fingerprint fingerprint;
//    private int mode;
//    private static final String PREF_KEY_PREFIX = "keyFor_"; // key is a combination of db file name and this prefix
    private Switch fingerprintSwitch;
    private TextView  fingerprintInfo;
    private View fingerprintOption;
//    private TextView confirmationView;
    private EditText passwordView;
    private Context context;


    public static void Launch(Context ctx) {
        Intent i = new Intent(ctx, AppSettingsActivity.class);

        ctx.startActivity(i);
    }

    private void onCreate(){

        fingerprintSwitch = (Switch) findViewById(R.id.fingerprint_switch);
        fingerprintInfo = (TextView) findViewById(R.id.fingerprint_info);
        fingerprintOption = findViewById(R.id.add_fingerprint);
        if (!fingerprint.hardwareSupport){
            //TODO make sure it is enabled otherwise
            fingerprintOption.setEnabled(false);
        } else if (!fingerprint.requirementsMet){
            fingerprintSwitch.setEnabled(false);
            fingerprintInfo.setText(fingerprint.errorMessage);
            //TODO hide password and confirm buttons
        } else{
            //TODO when radio button is pressed start registerFingeprint
            fingerprintSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b){
                        //TODO show buttons
                        registerFingerprint();
                    }else {
                        //TODO hide buttons
                    }
                }
            });

//            fingerprintView.setVisibility(View.VISIBLE);
//            confirmationView.setVisibility(View.VISIBLE);
            // all is set here so we can confirm to user and start listening for fingerprints
//            confirmationView.setText(R.string.scanning_fingerprint);
            // listen for decryption by default
//            toggleMode(Cipher.DECRYPT_MODE);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void registerFingerprint() {
        passwordView = (EditText) findViewById(R.id.fingerprint_password);
        fingerprint.startListening();

        fingerprint.listenForAuthenticationCallBack(new FingerprintManager.AuthenticationCallback(){

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {

        //I’m going to display the results of fingerprint authentication as a series of toasts.
        //Here, I’m creating the message that’ll be displayed if an error occurs//

        Toast.makeText(context, "Authentication error\n" + errString, Toast.LENGTH_LONG).show();
    }

    @Override

    //onAuthenticationFailed is called when the fingerprint doesn’t match with any of the fingerprints registered on the device//

    public void onAuthenticationFailed() {
        Toast.makeText(context, "Authentication failed", Toast.LENGTH_LONG).show();
    }

    @Override
    //onAuthenticationHelp is called when a non-fatal error has occurred. This method provides additional information about the error,
    //so to provide the user with as much feedback as possible I’m incorporating this information into my toast//
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        Toast.makeText(context, "Authentication help\n" + helpString, Toast.LENGTH_LONG).show();
    }

    @Override

    //onAuthenticationSucceeded is called when a fingerprint has been successfully matched to one of the fingerprints stored on the user’s device//
    public void onAuthenticationSucceeded(
            FingerprintManager.AuthenticationResult result) {

        Toast.makeText(context, "Success!", Toast.LENGTH_LONG).show();

        final String password = passwordView.getText().toString();
        fingerprint.storePassword(password);

    }
});
//        if accepted
//        else prompt wrong
//        passwordView.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(
//                    final CharSequence s,
//                    final int start,
//                    final int count,
//                    final int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(
//                    final CharSequence s,
//                    final int start,
//                    final int before,
//                    final int count) {
//
//            }
//
//            @Override
//            public void afterTextChanged(final Editable s) {
//                final boolean passwordEntered = s.length() > 0;
//                // encrypt or decrypt mode based on how much input or not
//                if (passwordEntered){
//                }
//
//            }
//        });
        //when fingerprint is pressed, get password value
        //in fingerprint class use method to encrypt value and store it
    }


//    initforfingerprint();
    // if fp is available also start listening for it
//    checkAvailability();
}
