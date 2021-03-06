/*
 * Copyright 2010 Tolga Onbay, Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.KeePass;
import com.android.keepass.R;
import com.keepassdroid.app.App;
import com.keepassdroid.database.edit.SetPassword;
import com.keepassdroid.password.PasswordGenerator;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.UriUtil;

import java.security.SecureRandom;

public class GeneratePasswordActivity extends LockCloseActivity {
	private static final int[] BUTTON_IDS = new int [] {R.id.btn_length6, R.id.btn_length8, R.id.btn_length12, R.id.btn_length16};
	private boolean passwordFromMasterPassword = false;
	private String masterPassword;
	private String salt;


	public static void Launch(Activity act) {
		Intent i = new Intent(act, GeneratePasswordActivity.class);
		
		act.startActivityForResult(i, 0);
	}
	
	private OnClickListener lengthButtonsListener = new OnClickListener() {
	    public void onClick(View v) {
	    	Button button = (Button) v;
	    	
	    	EditText editText = (EditText) findViewById(R.id.length);
	    	editText.setText(button.getText());
	    }
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.generate_password);
		setResult(KeePass.EXIT_NORMAL);
		
		for (int id : BUTTON_IDS) {
        	Button button = (Button) findViewById(id);
        	button.setOnClickListener(lengthButtonsListener);
		}
		
		Button genPassButton = (Button) findViewById(R.id.generate_password_button);
        genPassButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				fillPassword( false);
			}
		});

        Button genPassFromMPButton = (Button) findViewById(R.id.generate_password_from_MP);
		genPassFromMPButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				fillPassword(true);
			}
		});

        
        Button acceptButton = (Button) findViewById(R.id.accept_button);
        acceptButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				EditText password = (EditText) findViewById(R.id.password);
				
				Intent intent = new Intent();
				intent.putExtra("com.keepassdroid.password.generated_password", password.getText().toString());

				if (!passwordFromMasterPassword){
					setResult(EntryEditActivity.RESULT_OK_PASSWORD_GENERATOR, intent);
				} else {
					intent.putExtra("com.keepassdroid.password.generated_password", salt);
					setResult(EntryEditActivity.RESULT_OK_PASSWORD_GENERATOR_FROM_MASTERPASSWORD, intent);
				}

				finish();
			}
		});
        
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				
				finish();
			}
		});
        
        // Pre-populate a password to possibly save the user a few clicks
        fillPassword();
	}
	
	private void fillPassword() {
		EditText txtPassword = (EditText) findViewById(R.id.password);
		txtPassword.setText(generatePassword());
	}
	private void fillPassword(Boolean fromMP) {
		if (!fromMP){
			fillPassword();
		}else {
			AlertDialog alertDialog = new AlertDialog.Builder(this).create();
			alertDialog.setTitle("Enter password dialog");
			alertDialog.setMessage("In order to generate a password please type your master password");
			final EditText input= new EditText(this);
			input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			alertDialog.setView(input);
			alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							System.out.println("b6");
							masterPassword = input.getText().toString();
							passwordFromMasterPassword = true;
							dialog.dismiss();
							salt = generatePasswordFromMP(masterPassword);
						}
					});
			alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			alertDialog.show();
		}
	}
	
    public String generatePassword() {
    	String password = "";
    	try {
    		int length = Integer.valueOf(((EditText) findViewById(R.id.length)).getText().toString());
    		
    		((CheckBox) findViewById(R.id.cb_uppercase)).isChecked();
        	
        	PasswordGenerator generator = new PasswordGenerator(this);
	    	password = generator.generatePassword(length,
	    			((CheckBox) findViewById(R.id.cb_uppercase)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_lowercase)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_digits)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_minus)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_underline)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_space)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_specials)).isChecked(),
	    			((CheckBox) findViewById(R.id.cb_brackets)).isChecked());
    	} catch (NumberFormatException e) {
    		Toast.makeText(this, R.string.error_wrong_length, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
    	
    	return password;
    }
	public String generatePasswordFromMP(String masterPassword) {
		EditText txtPassword = (EditText) findViewById(R.id.password);
		int salt = 0;
		try {
			SecureRandom random = new SecureRandom(); // use more secure variant of Random!
			salt = random.nextInt();
			PasswordGenerator generator = new PasswordGenerator(this);
			String password = generator.generatePassword(masterPassword, salt);
			txtPassword.setText(password);
		} catch (NumberFormatException e) {
			Toast.makeText(this, R.string.error_wrong_length, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		return Integer.toString(salt);
	}
}
