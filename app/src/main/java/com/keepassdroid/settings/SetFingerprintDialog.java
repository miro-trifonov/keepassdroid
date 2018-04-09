package com.keepassdroid.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.keepassdroid.CancelDialog;
import com.keepassdroid.LockingActivity;
import com.keepassdroid.LockingClosePreferenceActivity;
import com.keepassdroid.database.edit.FileOnFinish;
import com.keepassdroid.utils.Fingerprint;

import org.w3c.dom.Text;

import javax.crypto.Cipher;

public class SetFingerprintDialog extends CancelDialog {

    SharedPreferences prefs;
    public Fingerprint fingerprint;
//    private int mode;
//    private static final String PREF_KEY_PREFIX = "keyFor_"; // key is a combination of db file name and this prefix;
//    private TextView confirmationView;
    private EditText passwordView;
    private Context context;
    private FileOnFinish mFinish;


    public SetFingerprintDialog(Context context) {
        super(context);
    }

    public SetFingerprintDialog(Context context, FileOnFinish finish) {
        super(context);
        mFinish = finish;
    }

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.use_fingerprint);
        fingerprint = new Fingerprint(getContext());
        this.context = getContext();

        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                cancel();
                if ( mFinish != null ) {
                    mFinish.run();
                }
            }
        });

        System.out.println("dial2");
        System.out.println(fingerprint);
        System.out.println("dial3");

        final TextView fingerprintInfo = (TextView) findViewById(R.id.fingerprint_info);
        Button accept = (Button) findViewById(R.id.ok);
        EditText passField = (EditText) findViewById(R.id.fingerprint_password);

        ImageView fpIcon = (ImageView) findViewById(R.id.imageView);

        if (!fingerprint.hardwareSupport){
            fingerprintInfo.setText(fingerprint.errorMessage);
        } else if (!fingerprint.requirementsMet){
            fpIcon.setVisibility(View.GONE);
            fingerprintInfo.setText(fingerprint.errorMessage);
            accept.setVisibility(View.GONE);
            passField.setVisibility(View.GONE);
        } else{
            registerFingerprint();


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
        Button accept = (Button) findViewById(R.id.ok);

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
        fingerprint.enabledFingerPrint = true;
        cancel();
        if ( mFinish != null ) {
            mFinish.run();
        }
    }
});
        fingerprint.startListening();
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
