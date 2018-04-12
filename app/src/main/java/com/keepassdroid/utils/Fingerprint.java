package com.keepassdroid.utils;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.widget.TextView;

import com.android.keepass.R;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;

public class Fingerprint {

    // Declare a string variable for the key we’re going to use in our fingerprint authentication
    private static final String KEY_NAME = "unlockKey";
    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private CancellationSignal cancellationSignal;
    private FingerprintManager.AuthenticationCallback authenticationCallback;
    private IvParameterSpec spec;
    private static final String IV_FILE = "iv-file";

    public Boolean hardwareSupport = false;
    public Boolean requirementsMet = false;
    public Boolean enabledFingerPrint = false;
    public String errorMessage = "unset";
    SharedPreferences prefs;
    private Context context;

    public Fingerprint(Context context) {
        this.context = context;
        // If you’ve set your app’s minSdkVersion to anything lower than 23, then you’ll need to verify that the device is running Marshmallow
        // or higher before executing any fingerprint-related code

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            errorMessage = "Android version too low";
        } else {
            //Get an instance of KeyguardManager and FingerprintManager//
            keyguardManager =
                    (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
            fingerprintManager =
                    (FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE);

            //Check whether the device has a fingerprint sensor//
            assert fingerprintManager != null;
            if (!fingerprintManager.isHardwareDetected()) {
                // If a fingerprint sensor isn’t available, then inform the user that they’ll be unable to use your app’s fingerprint functionality//
                errorMessage = "Your device doesn't support fingerprint authentication";
            } else {
                hardwareSupport = true;
                //Check whether the user has granted your app the USE_FINGERPRINT permission//
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                    // If your app doesn't have this permission, then display the following text//
                    errorMessage = "Please enable the fingerprint permission";
                }
                //Check that the user has registered at least one fingerprint//
                if (!fingerprintManager.hasEnrolledFingerprints()) {
                    // If the user hasn’t configured any fingerprints, then display the following message//
                    errorMessage = "No fingerprint configured. Please register at least one fingerprint in your device's Settings";
                }
                //Check that the lockscreen is secured//
                if (!keyguardManager.isKeyguardSecure()) {
                    // If the user hasn’t secured their lockscreen with a PIN password or pattern, then display the following text//
                    errorMessage = "Please enable lockscreen security in your device's Settings";
                } else {
                    try {
                        generateKey();
                    } catch (FingerprintException e) {
                        e.printStackTrace();
                    }
                    if (initCipher()) {
                        //If the cipher is initialized successfully, then create a CryptoObject instance//
                        cryptoObject = new FingerprintManager.CryptoObject(cipher);
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void startListening() {
        System.out.println("1");
        fingerprintManager = (FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE);
        cancellationSignal = new CancellationSignal();
        System.out.println("2");

        fingerprintManager.authenticate(cryptoObject, cancellationSignal, 0, authenticationCallback, null);
        System.out.println("3");

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void stopListening() {
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            cancellationSignal = null;
        }
    }

    public void listenForAuthenticationCallBack(final FingerprintManager.AuthenticationCallback authenticationCallback) {
        this.authenticationCallback = authenticationCallback;
    }

    public boolean storePassword(final String password) {
        try {
            // actual do encryption here
            byte[] encrypted = cipher.doFinal(password.getBytes());
            final String encryptedValue = Base64.encodeToString(encrypted, 0 /* flags */);

            spec = cipher.getParameters().getParameterSpec(IvParameterSpec.class);
            final FileOutputStream fileOutputStream = context.openFileOutput(IV_FILE, Context.MODE_PRIVATE);
            fileOutputStream.write(spec.getIV());
            fileOutputStream.close();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("password", encryptedValue);
            editor.putBoolean("FpAvailable", true);
            editor.commit();

            return true;
        } catch (final Exception e) {
            System.out.print(e);
            return false;
        }
    }

    public String decryptPassword(final String encryptedPassword) {

        try {
            // actual decryption here
            final byte[] encrypted = Base64.decode(encryptedPassword, 0);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);

        } catch (final Exception e) {
            System.out.print(e);
            return "0";
        }
    }

//Create the generateKey method that we’ll use to gain access to the Android keystore and generate the encryption key//

    @TargetApi(Build.VERSION_CODES.M)
    private void generateKey() throws FingerprintException {
        try {
            // Obtain a reference to the Keystore using the standard Android keystore container identifier (“AndroidKeystore”)//
            keyStore = KeyStore.getInstance("AndroidKeyStore");

            //Generate the key//
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            //Initialize an empty KeyStore//
            keyStore.load(null);

            //Initialize the KeyGenerator//
            keyGenerator.init(new
                    //Specify the operation(s) this key can be used for//
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)

                    //Configure this key so that the user has to confirm their identity with a fingerprint each time they want to use it//
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            //Generate the key//
            SecretKey key = keyGenerator.generateKey();
            System.out.println("Key + " + key.toString());

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();
            throw new FingerprintException(exc);
        }
    }

    //Create a new method that we’ll use to initialize our cipher//
    @TargetApi(Build.VERSION_CODES.M)
    public boolean initCipher() {
        try {
            //Obtain a cipher instance and configure it with the properties required for fingerprint authentication//
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            System.out.println("Key in cypher + " + key.toString());

            cipher.init(Cipher.ENCRYPT_MODE, key);
            System.out.println("cypher + " + cipher.toString());

            //Return true if the cipher has been initialized successfully//
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {

            //Return false if cipher initialization failed//
            return false;
        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    public boolean isAvailable() {
        return hardwareSupport && requirementsMet && enabledFingerPrint;
    }

    private class FingerprintException extends Exception {
        public FingerprintException(Exception e) {
            super(e);
        }
    }
}
