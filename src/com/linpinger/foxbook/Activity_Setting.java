package com.linpinger.foxbook;


import java.io.File;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

public class Activity_Setting extends PreferenceActivity {
	SharedPreferences settings;
	private boolean isWhiteActionBar = false; // ��ɫ������
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		isWhiteActionBar = settings.getBoolean("isWhiteActionBar", isWhiteActionBar);
		if ( isWhiteActionBar ) {
			this.setTheme(android.R.style.Theme_DeviceDefault_Light);
		}

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences); // ��ʹ��PreferenceActivityʱ
		// getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragement()).commit(); 

		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
		
	}


	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if ( preference.getKey().equalsIgnoreCase("selectfont") ) {
			Intent itt = new Intent(Activity_Setting.this, Activity_FileChooser.class);
			itt.putExtra("dir", "/sdcard/fonts/");
			startActivityForResult(itt, 9);
			//				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			//				intent.setType("*/*"); 
			//				intent.addCategory(Intent.CATEGORY_OPENABLE);
			//			    try {
			//			        startActivityForResult( Intent.createChooser(intent, "ѡ��һ�������ļ�: *.ttf/*.ttc"), 99);
			//			    } catch (android.content.ActivityNotFoundException ex) {
			//			    	Toast.makeText(this.getActivity(), "��װһ���ļ��������ð�", Toast.LENGTH_SHORT).show();
			//			    }
			return true;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 9:  // ��Ӧ�ļ�ѡ������ѡ��
			if (resultCode == RESULT_OK) {
				Uri uri = data.getData();
				String newFont = new File(uri.getPath()).getAbsolutePath();
				String nowPATH = newFont.toLowerCase() ;
				if ( nowPATH.endsWith(".ttf") | nowPATH.endsWith(".ttc") | nowPATH.endsWith(".otf") ) {
					SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
					editor.putString("selectfont", newFont);
					editor.commit();
					Toast.makeText(this, newFont, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, "Ҫѡ���׺Ϊ.ttf/.ttc/.otf�������ļ�", Toast.LENGTH_SHORT).show();
				}
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	
	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case android.R.id.home: // ����ͼ��
			this.finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	

}
