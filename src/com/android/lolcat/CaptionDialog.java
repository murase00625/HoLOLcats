package com.android.lolcat;

import org.holoeverywhere.app.AlertDialog;

import org.holoeverywhere.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import org.holoeverywhere.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class CaptionDialog extends DialogFragment {
	
	private static final String TAG = "LolcatActivity.CaptionDialog";
	
	private String mTop, mBottom;
	
	public interface CaptionDialogListener {
		void onFinishEditCaptions(String topText, String bottomText);
	}
	
	public CaptionDialog() { }
	
		/* (non-Javadoc)
	 * @see android.support.v4.app.DialogFragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo);
	}
	
	

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater factory = LayoutInflater.from(getActivity());
		final View textEntryView = factory.inflate(R.layout.lolcat_caption_dialog, null);
		
		Bundle b = getArguments();
		if (b != null) {
			if (b.containsKey("topText")) {
				EditText top = (EditText) textEntryView.findViewById(R.id.top_edittext);
				top.setText(b.getString("topText"));
			}
			if (b.containsKey("bottomText")) {
				EditText bottom = (EditText) textEntryView.findViewById(R.id.bottom_edittext);
				bottom.setText(b.getString("bottomText"));
			}
		}
		
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey("topText")) {
				EditText top = (EditText) textEntryView.findViewById(R.id.top_edittext);
				top.setText(savedInstanceState.getString("topText"));
			}
			if (savedInstanceState.containsKey("bottomText")) {
				EditText bottom = (EditText) textEntryView.findViewById(R.id.bottom_edittext);
				bottom.setText(savedInstanceState.getString("bottomText"));
			}
		}
		((EditText) textEntryView.findViewById(R.id.top_edittext)).requestFocus();
		
		return new AlertDialog.Builder(getActivity())
        .setTitle(R.string.lolcat_caption_dialog_title)
        .setIcon(0)
        .setView(textEntryView)
        .setPositiveButton(
                R.string.lolcat_caption_dialog_ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.i(TAG, "Caption dialog: OK...");
                        EditText topText = 
                        		(EditText) textEntryView.findViewById(R.id.top_edittext);
                        EditText bottomText = 
                        		(EditText) textEntryView.findViewById(R.id.bottom_edittext);
                        CaptionDialogListener activity =
                        		(CaptionDialogListener) getActivity();
                        mTop = topText.getText().toString();
                        mBottom = bottomText.getText().toString();
                        activity.onFinishEditCaptions(mTop, mBottom);
                        dialog.dismiss();
                    }
                })
        .setNegativeButton(
                R.string.lolcat_caption_dialog_cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.i(TAG, "Caption dialog: CANCEL...");
                        dialog.dismiss();
                        // Nothing to do here (for now at least)
                    }
                })
        .create();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (mTop != null) outState.putString("topText", mTop);
		if (mBottom != null) outState.putString("bottomText", mBottom);
		super.onSaveInstanceState(outState);
	}
	
	

}
