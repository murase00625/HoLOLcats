package com.android.lolcat;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.TextView;
import org.holoeverywhere.widget.Toast;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class LolcatPicInfoActivity extends Activity {
	
	public static final String INTENT_FILENAME_KEY = "lolpic-filename";
	public static final String INTENT_URI_KEY = "lolpic-uri";
	
	private static final String TAG = "LolcatActivity.LolcatPicInfoActivity";
	private static final String SAVED_IMAGE_MIME_TYPE = "image/png";
	
	private TextView mTitle, mFileName, mURI;
	private String mFName;
	private Uri mFURI;
	
	/* (non-Javadoc)
	 * @see org.holoeverywhere.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lolcat_file_saved);
		
		mTitle = (TextView) findViewById(R.id.lolpic_save_success_title);
		mTitle.setText(R.string.lolcat_save_succeeded_dialog_title);
		
		mFileName = (TextView) findViewById(R.id.lolpic_save_success_filename);
		mURI = (TextView) findViewById(R.id.lolpic_save_success_uri);
		
		Intent intent = getIntent();
		mFName = intent.getStringExtra(INTENT_FILENAME_KEY);
		mFileName.setText("File name: " + mFName);
		
		String s = intent.getStringExtra(INTENT_URI_KEY);
		if (s != null) mFURI = Uri.parse(s);
		mURI.setText("File URI: " + s);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_save, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Log.i(TAG, "Action Bar home selector tapped.");
			finish();
			return true;
		case R.id.ab_menu_view:
			Log.i(TAG, "View menu item tapped.");
			if (mFURI == null) {
				warnUser("Can't view in gallery app: URI is null.");
				return true;
			}
			Intent intent = new Intent(Intent.ACTION_VIEW, mFURI);
			Log.i(TAG, "Starting activity to view " + mFURI);
			startActivity(intent);
			return true;
		case R.id.ab_menu_share:
			if (mFURI == null) {
				warnUser("Can't share: URI is null.");
				return true;
			}
			Intent intentTwo = new Intent();
			intentTwo.setAction(Intent.ACTION_SEND);
			intentTwo.setType(SAVED_IMAGE_MIME_TYPE);
			intentTwo.putExtra(Intent.EXTRA_STREAM, mFURI);
	        try {
	            startActivity(
	                    Intent.createChooser(
	                            intentTwo,
	                            getResources().getString(R.string.lolcat_sendImage_label)));
	        } catch (android.content.ActivityNotFoundException ex) {
	            Log.w(TAG, "shareSavedImage: startActivity failed", ex);
	            Toast.makeText(this, R.string.lolcat_share_failed, Toast.LENGTH_SHORT).show();
	        }
			return true;
		case R.id.ab_menu_cancel:
			finish();
			return true;
		default: return false;
		}
	}
	
	private void warnUser(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

}
