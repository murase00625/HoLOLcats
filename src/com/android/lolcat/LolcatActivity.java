/*
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.lolcat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.holoeverywhere.app.Activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.android.lolcat.CaptionDialog.CaptionDialogListener;

/**
 * Lolcat builder activity.
 *
 * Instructions:
 * (1) Take photo of cat using Camera
 * (2) Run LolcatActivity
 * (3) Pick photo
 * (4) Add caption(s)
 * (5) Save and share
 *
 * See README.txt for a list of currently-missing features and known bugs.
 * 
 * To do list (overview):
 *   - Instead of a ProgressDialog, swap the save icon with an indeterminate progress
 *     animation:
 *  http://stackoverflow.com/questions/10676517/actionbar-progress-indicator-and-refresh-button
 *   - After the thread is finished saving the file, launch a new Fragment with the
 *      data of the new lolpic.
 * 
 * Change log by Morio Murase:
 * 7/09/2013: Converted captioning dialog to a DialogFragment.
 * 7/11/2013: Completed migration of the app's main Activity to HoloAnywhere,
 * 				complete with icons. Confirmed to run on Motorola Droid.
 * 7/11/2013: All refactoring to most recent standards completed.
 * 
 */
public class LolcatActivity extends Activity implements CaptionDialogListener {

	private static final String TAG = "LolcatActivity";

    // Location on the SD card for saving lolcat images
    private static final String LOLCAT_SAVE_DIRECTORY = "lolcats/";

    // Mime type / format / extension we use (must be self-consistent!)
    private static final String SAVED_IMAGE_EXTENSION = ".png";
    private static final Bitmap.CompressFormat SAVED_IMAGE_COMPRESS_FORMAT =
            Bitmap.CompressFormat.PNG;

    // UI Elements
    private LolcatView mLolcatView;
    
    private Handler mHandler;

    private Uri mPhotoUri;

    private String mSavedImageFilename;
    private Uri mSavedImageUri;
    
    private Menu mABSMenu;

    private MediaScannerConnection mMediaScannerConnection;

    // Request codes used with startActivityForResult()
    private static final int PHOTO_PICKED = 1;
    
    // Keys used with onSaveInstanceState()
    private static final String PHOTO_URI_KEY = "photo_uri";
    private static final String SAVED_IMAGE_FILENAME_KEY = "saved_image_filename";
    private static final String SAVED_IMAGE_URI_KEY = "saved_image_uri";
    private static final String TOP_CAPTION_KEY = "top_caption";
    private static final String BOTTOM_CAPTION_KEY = "bottom_caption";
    private static final String CAPTION_POSITIONS_KEY = "caption_positions";
    
    
    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(TAG, "onCreate()...  icicle = " + icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(icicle);

        setContentView(R.layout.lolcat_holo);

        // Look up various UI elements
        mLolcatView = (LolcatView) findViewById(R.id.main_image);

        // Need one of these to call back to the UI thread
        // (and run AlertDialog.show(), for that matter)
        mHandler = new Handler();

        mMediaScannerConnection = new MediaScannerConnection(this, mMediaScanConnClient);

        if (icicle != null) {
            Log.i(TAG, "- reloading state from icicle!");
            restoreStateFromIcicle(icicle);
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()...");
        super.onResume();

        updateButtons();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()...");
        super.onSaveInstanceState(outState);

        // State from the Activity:
        outState.putParcelable(PHOTO_URI_KEY, mPhotoUri);
        outState.putString(SAVED_IMAGE_FILENAME_KEY, mSavedImageFilename);
        outState.putParcelable(SAVED_IMAGE_URI_KEY, mSavedImageUri);

        // State from the LolcatView:
        // Revoked todo: Yes, parceling the whole View is overkill.
        outState.putString(TOP_CAPTION_KEY, mLolcatView.getTopCaption());
        outState.putString(BOTTOM_CAPTION_KEY, mLolcatView.getBottomCaption());
        outState.putIntArray(CAPTION_POSITIONS_KEY, mLolcatView.getCaptionPositions());
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		mABSMenu = menu;
		addonSherlock().setProgressBarIndeterminateVisibility(false);
		updateButtons();
		return true;
	}

    /**
     * Restores the activity state from the specified icicle.
     * @see onCreate()
     * @see onSaveInstanceState()
     */
    private void restoreStateFromIcicle(Bundle icicle) {
        Log.i(TAG, "restoreStateFromIcicle()...");

        // State of the Activity:

        Uri photoUri = icicle.getParcelable(PHOTO_URI_KEY);
        Log.i(TAG, "  - photoUri: " + photoUri);
        if (photoUri != null) {
            loadPhoto(photoUri);
        }

        mSavedImageFilename = icicle.getString(SAVED_IMAGE_FILENAME_KEY);
        mSavedImageUri = icicle.getParcelable(SAVED_IMAGE_URI_KEY);

        // State of the LolcatView:

        String topCaption = icicle.getString(TOP_CAPTION_KEY);
        String bottomCaption = icicle.getString(BOTTOM_CAPTION_KEY);
        int[] captionPositions = icicle.getIntArray(CAPTION_POSITIONS_KEY);
        Log.i(TAG, "  - captions: '" + topCaption + "', '" + bottomCaption + "'");
        if (!TextUtils.isEmpty(topCaption) || !TextUtils.isEmpty(bottomCaption)) {
            mLolcatView.setCaptions(topCaption, bottomCaption);
            mLolcatView.setCaptionPositions(captionPositions);
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()...");
        super.onDestroy();
        clearPhoto();  // Free up some resources, and force a GC
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch(item.getItemId()) {
    	case R.id.ab_menu_create_pic:
    		Log.i(TAG, "Menu item open pic selected.");
    		Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            startActivityForResult(intent, PHOTO_PICKED);
    		return true;
    		
    	case R.id.ab_menu_add_captions:
    		Log.i(TAG, "Menu item add captions selected.");
    		showCaptionDialog();
    		return true;
    		
    	case R.id.ab_menu_save_pic:
    		Log.i(TAG, "Menu item save lolpic selected.");
    		saveImage();
    		return true;
    		
    	case R.id.ab_menu_clear_captions:
    		Log.i(TAG, "Menu item clear captions selected.");
    		clearCaptions();
            updateButtons();
    		return true;
    		
    	case R.id.ab_menu_clear_photo:
    		Log.i(TAG, "Menu item discard lolpic selected.");
    		clearPhoto();  // Also does clearCaptions()
            updateButtons();
    		return true;
    		
    	default:
    		Log.w(TAG, "Something totally unexpected was clicked; punting to superclass.");
    		return super.onOptionsItemSelected(item);
    		
    	}
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult(request " + requestCode
              + ", result " + resultCode + ", data " + data + ")...");

        if (resultCode != RESULT_OK) {
            Log.i(TAG, "==> result " + resultCode + " from subactivity!  Ignoring...");
            Toast t = Toast.makeText(this, R.string.lolcat_nothing_picked, Toast.LENGTH_SHORT);
            t.show();
            return;
        }

        if (requestCode == PHOTO_PICKED) {
            // "data" is an Intent containing (presumably) a URI like
            // "content://media/external/images/media/3".

            if (data == null) {
                Log.w(TAG, "Null data, but RESULT_OK, from image picker!");
                Toast t = Toast.makeText(this, R.string.lolcat_nothing_picked,
                                         Toast.LENGTH_SHORT);
                t.show();
                return;
            }

            if (data.getData() == null) {
                Log.w(TAG, "'data' intent from image picker contained no data!");
                Toast t = Toast.makeText(this, R.string.lolcat_nothing_picked,
                                         Toast.LENGTH_SHORT);
                t.show();
                return;
            }

            loadPhoto(data.getData());
            updateButtons();
        }
    }

    /**
     * Updates the enabled/disabled state of the onscreen buttons.
     */
    private void updateButtons() {
        Log.i(TAG, "updateButtons()...");

        // Do we have a valid photo and/or caption(s) yet?
        Drawable d = mLolcatView.getDrawable();
        // Log.i(TAG, "===> current mLolcatView drawable: " + d);
        boolean validPhoto = (d != null);
        boolean validCaption = mLolcatView.hasValidCaption();

        // Initial idea to programmatically disable/enable menu buttons via Stack Overflow:
        // http://stackoverflow.com/questions/14169040/enable-disable-actionbar-menu-item
        //  ... For some reason, hanging onto MenuItem objects didn't work.
        if (mABSMenu != null) {
            // mPickButton is always enabled.
            // Therefore its ActionBar equivalent must also be enabled
            //  since it gets grayed out during the save process.
            // When the save is finished, this method is called.
            mABSMenu.findItem(R.id.ab_menu_create_pic)
            		.setEnabled(true)
            		.setIcon(R.drawable.newpic);
        	mABSMenu.findItem(R.id.ab_menu_add_captions).setEnabled(validPhoto)
        			.setIcon((validPhoto) ? R.drawable.edit : R.drawable.disablededit);
        	mABSMenu.findItem(R.id.ab_menu_save_pic)
        			.setEnabled(validPhoto && validCaption)
        			.setIcon((validPhoto && validCaption) ?
        						R.drawable.save : R.drawable.disabledsave);
        	mABSMenu.findItem(R.id.ab_menu_clear_captions)
        			.setEnabled(validPhoto && validCaption)
        			.setIcon((validPhoto && validCaption) ?
        						R.drawable.undo : R.drawable.disabledundo);
        	mABSMenu.findItem(R.id.ab_menu_clear_photo)
        			.setEnabled(validPhoto)
        			.setIcon((validPhoto) ? R.drawable.discard : R.drawable.disableddiscard);
        }
        
        
    }

    /**
     * Clears out any already-entered captions for this lolcat.
     */
    private void clearCaptions() {
        mLolcatView.clearCaptions();

        // This also invalidates any image we've previously written to the
        // SD card...
        mSavedImageFilename = null;
        mSavedImageUri = null;
    }

    /**
     * Completely resets the UI to its initial state, with no photo
     * loaded, and no captions.
     */
    private void clearPhoto() {
        mLolcatView.clear();

        mPhotoUri = null;
        mSavedImageFilename = null;
        mSavedImageUri = null;

        clearCaptions();

        // Force a gc (to be sure to reclaim the memory used by our
        // potentially huge bitmap):
        System.gc();
    }

    /**
     * Loads the image with the specified Uri into the UI.
     */
    private void loadPhoto(Uri uri) {
        Log.i(TAG, "loadPhoto: uri = " + uri);

        clearPhoto();  // Be sure to release the previous bitmap
                       // before creating another one
        mPhotoUri = uri;

        // A new photo always starts out uncaptioned.
        clearCaptions();

        // Load the selected photo into our ImageView.
        mLolcatView.loadFromUri(mPhotoUri);
    }

    private void showCaptionDialog() {
        // If the dialog already exists, always reset focus to the top
        // item each time it comes up.
        FragmentManager fm = getSupportFragmentManager();
        CaptionDialog dialog = new CaptionDialog();
        String top = mLolcatView.getTopCaption();
        String bottom = mLolcatView.getBottomCaption();

        if (top != null || bottom != null) {
        	Bundle args = new Bundle();
        	if (top != null) args.putString("topText", top);
        	if (bottom != null) args.putString("bottomText", bottom);
        	dialog.setArguments(args);
        }
        
        dialog.show(fm, "lolcat_caption_dialog");
        
    }
    

    private void showSaveSuccessDialog() {
        setProgressIndicator(false);
        updateButtons();

        Intent intent = new Intent(this, LolcatPicInfoActivity.class);
        intent.putExtra(LolcatPicInfoActivity.INTENT_FILENAME_KEY, mSavedImageFilename);
        intent.putExtra(LolcatPicInfoActivity.INTENT_URI_KEY, mSavedImageUri.toString());
        startActivity(intent);
        
    }

    private void setProgressIndicator(boolean b) {
    	addonSherlock().setProgressBarIndeterminateVisibility(b);
    }
    
    /**
     * Kicks off the process of saving the LolcatView's working Bitmap to
     * the SD card, in preparation for viewing it later and/or sharing it.
     */
    private void saveImage() {
        Log.i(TAG, "saveImage()...");

        setProgressIndicator(true);
        disableAllMenuItems();

        // We now need to save the bitmap to the SD card, and then ask the
        // MediaScanner to scan it.  Do the actual work of all this in a
        // helper thread, since it's fairly slow (and will occasionally
        // ANR if we do it here in the UI thread.)

        Thread t = new Thread() {
                public void run() {
                    Log.i(TAG, "Running worker thread...");
                    saveImageInternal();
                }
            };
        t.start();
        // Next steps:
        // - saveImageInternal()
        // - onMediaScannerConnected()
        // - onScanCompleted
    }

    /**
     * Saves the LolcatView's working Bitmap to the SD card, in
     * preparation for viewing it later and/or sharing it.
     *
     * The bitmap will be saved as a new file in the directory
     * LOLCAT_SAVE_DIRECTORY, with an automatically-generated filename
     * based on the current time.  It also connects to the
     * MediaScanner service, since we'll need to scan that new file (in
     * order to get a Uri we can then VIEW or share.)
     *
     * This method is run in a worker thread; @see saveImage().
     */
    private void saveImageInternal() {
        Log.i(TAG, "saveImageInternal()...");

        // TODO: Currently we save the bitmap to a file on the sdcard,
        // then ask the MediaScanner to scan it (which gives us a Uri we
        // can then do an ACTION_VIEW on.)  But rather than doing these
        // separate steps, maybe there's some easy way (given an
        // OutputStream) to directly talk to the MediaProvider
        // (i.e. com.android.provider.MediaStore) and say "here's an
        // image, please save it somwhere and return the URI to me"...

        // Save the bitmap to a file on the sdcard.
        // (Based on similar code in MusicUtils.java.)
        // TODO: Make this filename more human-readable?  Maybe "Lolcat-YYYY-MM-DD-HHMMSS.png"?
        String filename = Environment.getExternalStorageDirectory()
                + "/" + LOLCAT_SAVE_DIRECTORY
                + String.valueOf(System.currentTimeMillis() + SAVED_IMAGE_EXTENSION);
        Log.i(TAG, "- filename: '" + filename + "'");
        
        // We need to make sure the SD card is available here BEFORE we try to create files.
        // Code "borrowed" from developer guide on data storage:
        // http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();
        
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
        	Log.i(TAG, "Storage is read-write");
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
        	Log.w(TAG, "Storage is read-only");
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
        	Log.e(TAG, "Something is wrong with the SD card storage.");
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        // The method CREATES the file here in the if clause if it doesn't exist.
        if (mExternalStorageAvailable && mExternalStorageWriteable && ensureFileExists(filename)) {
            try {
                OutputStream outstream = new FileOutputStream(filename);
                Bitmap bitmap = mLolcatView.getWorkingBitmap();
                boolean success = bitmap.compress(SAVED_IMAGE_COMPRESS_FORMAT,
                                                  100, outstream);
                Log.i(TAG, "- success code from Bitmap.compress: " + success);
                outstream.close();

                if (success) {
                    Log.i(TAG, "- Saved!  filename = " + filename);
                    mSavedImageFilename = filename;

                    // Ok, now we need to get the MediaScanner to scan the
                    // file we just wrote.  Step 1 is to get our
                    // MediaScannerConnection object to connect to the
                    // MediaScanner service.
                    mMediaScannerConnection.connect();
                    // See onMediaScannerConnected() for the next step
                } else {
                    Log.w(TAG, "Bitmap.compress failed: bitmap " + bitmap
                          + ", filename '" + filename + "'");
                    onSaveFailed(R.string.lolcat_save_failed);
                }
            } catch (FileNotFoundException e) {
                Log.w(TAG, "error creating file", e);
                onSaveFailed(R.string.lolcat_save_failed);
            } catch (IOException e) {
                Log.w(TAG, "error creating file", e);
                onSaveFailed(R.string.lolcat_save_failed);
            }
        } else {
            Log.w(TAG, "ensureFileExists failed for filename '" + filename + "'");
            onSaveFailed(R.string.lolcat_save_failed);
        }
    }


    //
    // MediaScanner-related code
    //

    /**
     * android.media.MediaScannerConnection.MediaScannerConnectionClient implementation.
     */
    private MediaScannerConnection.MediaScannerConnectionClient mMediaScanConnClient =
        new MediaScannerConnection.MediaScannerConnectionClient() {
            /**
             * Called when a connection to the MediaScanner service has been established.
             */
            public void onMediaScannerConnected() {
                Log.i(TAG, "MediaScannerConnectionClient.onMediaScannerConnected...");
                // The next step happens in the UI thread:
                mHandler.post(new Runnable() {
                        public void run() {
                            LolcatActivity.this.onMediaScannerConnected();
                        }
                    });
            }

            /**
             * Called when the media scanner has finished scanning a file.
             * @param path the path to the file that has been scanned.
             * @param uri the Uri for the file if the scanning operation succeeded
             *        and the file was added to the media database, or null if scanning failed.
             */
            public void onScanCompleted(final String path, final Uri uri) {
                Log.i(TAG, "MediaScannerConnectionClient.onScanCompleted: path "
                      + path + ", uri " + uri);
                // Just run the "real" onScanCompleted() method in the UI thread:
                mHandler.post(new Runnable() {
                        public void run() {
                            LolcatActivity.this.onScanCompleted(path, uri);
                        }
                    });
            }
        };

    /**
     * This method is called when our MediaScannerConnection successfully
     * connects to the MediaScanner service.  At that point we fire off a
     * request to scan the lolcat image we just saved.
     *
     * This needs to run in the UI thread, so it's called from
     * mMediaScanConnClient's onMediaScannerConnected() method via our Handler.
     */
    private void onMediaScannerConnected() {
        Log.i(TAG, "onMediaScannerConnected()...");

        // Update the message in the progress dialog...
        // mSaveProgressDialog.setMessage(getResources().getString(R.string.lolcat_scanning));
        // Heh. Let's fire off a Toast instead.
        Toast.makeText(this, R.string.lolcat_scanning, Toast.LENGTH_SHORT).show();

        // Fire off a request to the MediaScanner service to scan this
        // file; we'll get notified when the scan completes.
        Log.i(TAG, "- Requesting scan for file: " + mSavedImageFilename);
        mMediaScannerConnection.scanFile(mSavedImageFilename,
                                         null /* mimeType */);

        // Next step: mMediaScanConnClient will get an onScanCompleted() callback,
        // which calls our own onScanCompleted() method via our Handler.
    }

    /**
     * Updates the UI after the media scanner finishes the scanFile()
     * request we issued from onMediaScannerConnected().
     *
     * This needs to run in the UI thread, so it's called from
     * mMediaScanConnClient's onScanCompleted() method via our Handler.
     */
    private void onScanCompleted(String path, final Uri uri) {
        Log.i(TAG, "onScanCompleted: path " + path + ", uri " + uri);
        mMediaScannerConnection.disconnect();

        if (uri == null) {
            Log.w(TAG, "onScanCompleted: scan failed.");
            mSavedImageUri = null;
            onSaveFailed(R.string.lolcat_scan_failed);
            return;
        }

        // Success!

        // dismissDialog(DIALOG_SAVE_PROGRESS);
        setProgressIndicator(false);
        updateButtons();

        // We can now access the saved lolcat image using the specified Uri.
        mSavedImageUri = uri;

        // Bring up a success dialog, giving the user the option to go to
        // the pictures app (so you can share the image).
        showSaveSuccessDialog();
    }


    //
    // Other misc utility methods
    //

    /**
     * Ensure that the specified file exists on the SD card, creating it
     * if necessary.
     *
     * Copied from MediaProvider / MusicUtils.
     *
     * @return true if the file already exists, or we
     *         successfully created it.
     */
    private static boolean ensureFileExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        } else {
            // we will not attempt to create the first directory in the path
            // (for example, do not create /sdcard if the SD card is not mounted)
            int secondSlash = path.indexOf('/', 1);
            if (secondSlash < 1) return false;
            String directoryPath = path.substring(0, secondSlash);
            File directory = new File(directoryPath);
            if (!directory.exists())
                return false;
            file.getParentFile().mkdirs();
            // Bug: Manifest lacked permission to write to SD card. FIXED.
            try {
                return file.createNewFile();
            } catch (IOException ioe) {
                Log.w(TAG, "File creation failed", ioe);
            }
            return false;
        }
    }

    /**
     * Updates the UI after a failure anywhere in the bitmap saving / scanning
     * sequence.
     */
    private void onSaveFailed(int errorMessageResId) {
        
//        dismissDialog(DIALOG_SAVE_PROGRESS);
        setProgressIndicator(false);
        updateButtons();
    	
        // Refactor: The original code would throw a RuntimeException because
        //   the Toast entered a race condition with the Thread execution.
        //   Sticking the Toast code in a runOnUiThread call solves this.
        //   Parameter and activity references finalized for thread safety.
        final Activity a = (Activity) this;
        final int resId = errorMessageResId;
        runOnUiThread(new Runnable() {
        	public void run() { Toast.makeText(a, resId, Toast.LENGTH_SHORT).show(); }
        });
        
    }

	@Override
	public void onFinishEditCaptions(String topText, String bottomText) {
		Log.i(TAG, "onFinishEditCaptions()...");
		
		Log.i(TAG, "Top caption: '" + topText + "'");
        Log.i(TAG, "Bottom caption: '" + bottomText + "'");

        mLolcatView.setCaptions(topText, bottomText);
        updateButtons();		
	}
	
	private void disableAllMenuItems() {
		mABSMenu.findItem(R.id.ab_menu_create_pic)
				.setEnabled(false)
				.setIcon(R.drawable.disablednewpic);
		mABSMenu.findItem(R.id.ab_menu_add_captions)
				.setEnabled(false)
				.setIcon(R.drawable.disablededit);
		mABSMenu.findItem(R.id.ab_menu_save_pic)
				.setEnabled(false)
				.setIcon(R.drawable.disabledsave);
		mABSMenu.findItem(R.id.ab_menu_clear_captions)
				.setEnabled(false)
				.setIcon(R.drawable.disabledundo);
		mABSMenu.findItem(R.id.ab_menu_clear_photo)
				.setEnabled(false)
				.setIcon(R.drawable.disableddiscard);
	}
	
}
