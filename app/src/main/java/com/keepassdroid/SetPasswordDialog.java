/*
 * Copyright 2009-2016 Brian Pellin.
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

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.keepass.R;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.keepassdroid.app.App;
import com.keepassdroid.database.edit.FileOnFinish;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.SetPassword;
import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.UriUtil;
import com.nulabinc.zxcvbn.Feedback;
import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;

import org.w3c.dom.Text;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SetPasswordDialog extends CancelDialog {

	private byte[] masterKey;
	private Uri mKeyfile;
	private FileOnFinish mFinish;
		
	public SetPasswordDialog(Context context) {
		super(context);
	}
	
	public SetPasswordDialog(Context context, FileOnFinish finish) {
		super(context);
		mFinish = finish;
	}
	
	public byte[] getKey() {
		return masterKey;
	}
	
	public Uri keyfile() {
		return mKeyfile;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.set_password);
		
		setTitle(R.string.password_title);

		//TODO check password strength
		Zxcvbn zxcvbn = new Zxcvbn();
		final EditText passView = (EditText) findViewById(R.id.pass_password);
		final ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
		final TextView feedback_text = (TextView) findViewById(R.id.pass_hint);
		Subscription subscription = RxTextView.textChanges(passView)
				// In case the events get emitted too fast
				.onBackpressureLatest()
				// Don't wanna block our UI
				.observeOn(Schedulers.computation())
				// EditText will return a CharSequence, zxcvbn needs a String
				.map(CharSequence::toString)
				// Do the magic
				.map(zxcvbn::measure)
				// read the score
				// Update our ProgressBar
				.subscribe(measure -> {
						//TODO which suggestion to sue
						Feedback feedback = measure.getFeedback();
						String suggestion = feedback.getSuggestions().get(0);
						//TODO test and see if strength can be measured instead of score
						Double strength = measure.getGuessesLog10();
						strength = Math.min(strength,12);
						int score = measure.getScore();
						progress.setProgress((int) Math.round(strength));
						feedback_text.setText(feedback.getSuggestions().get(0));

//						# Integer from 0-4 (useful for implementing a strength bar)
//# 0 Weak        （guesses < ^ 3 10）
//# 1 Fair        （guesses <^ 6 10）
//# 2 Good        （guesses <^ 8 10）
//# 3 Strong      （guesses < 10 ^ 10）
//# 4 Very strong （guesses >= 10 ^ 10）

					}
				);




		// Ok button
		Button okButton = (Button) findViewById(R.id.ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				String pass = passView.getText().toString();
				TextView passConfView = (TextView) findViewById(R.id.pass_conf_password);
				String confpass = passConfView.getText().toString();
				
				// Verify that passwords match
				if ( ! pass.equals(confpass) ) {
					// Passwords do not match
					Toast.makeText(getContext(), R.string.error_pass_match, Toast.LENGTH_LONG).show();
					return;
				}
				
				TextView keyfileView = (TextView) findViewById(R.id.pass_keyfile);
				Uri keyfile = UriUtil.parseDefaultFile(keyfileView.getText().toString());
				mKeyfile = keyfile;
				
				// Verify that a password or keyfile is set
				if ( pass.length() == 0 && EmptyUtils.isNullOrEmpty(keyfile)) {
					Toast.makeText(getContext(), R.string.error_nopass, Toast.LENGTH_LONG).show();
					return;
					
				}
				
				SetPassword sp = new SetPassword(getContext(), App.getDB(), pass, keyfile, new AfterSave(mFinish, new Handler()));
				final ProgressTask pt = new ProgressTask(getContext(), sp, R.string.saving_database);
				boolean valid = sp.validatePassword(getContext(), new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						pt.run();
					}
				});
				
				if (valid) {
				    pt.run();
				}
			}
			
		});
		
		// Cancel button
		Button cancel = (Button) findViewById(R.id.cancel);
		cancel.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				cancel();
				if ( mFinish != null ) {
					mFinish.run();
				}
			}
		});
	}

	private class AfterSave extends OnFinish {
		private FileOnFinish mFinish;
		
		public AfterSave(FileOnFinish finish, Handler handler) {
			super(finish, handler);
			mFinish = finish;
		}

		@Override
		public void run() {
			if ( mSuccess ) {
				if ( mFinish != null ) {
					mFinish.setFilename(mKeyfile);
				}
				dismiss();
			} else {
				displayMessage(getContext());
			}
			super.run();
		}
	}
}
