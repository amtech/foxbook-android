package com.linpinger.foxbook;


import java.io.File;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

public class Activity_Setting extends Activity {
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
		// addPreferencesFromResource(R.xml.preferences); // ��ʹ��PreferenceActivityʱ
		getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragement()).commit(); 

		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
		
	}
	

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class PrefsFragement extends PreferenceFragment{  
		@Override  
		public void onCreate(Bundle savedInstanceState) {  
			super.onCreate(savedInstanceState);  
			addPreferencesFromResource(R.xml.preferences);  
		}

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
			if ( preference.getKey().equalsIgnoreCase("selectfont") ) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("*/*"); 
				intent.addCategory(Intent.CATEGORY_OPENABLE);
			    try {
			        startActivityForResult( Intent.createChooser(intent, "ѡ��һ�������ļ�: *.ttf/*.ttc"), 99);
			    } catch (android.content.ActivityNotFoundException ex) {
			    	Toast.makeText(this.getActivity(), "��װһ���ļ��������ð�", Toast.LENGTH_SHORT).show();
			    }
				return true;
			}
			return super.onPreferenceTreeClick(preferenceScreen, preference);
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			switch (requestCode) {
			case 99:  // ��Ӧ�ļ�ѡ������ѡ��
				if (resultCode == RESULT_OK) {
					Uri uri = data.getData();
					String newFont = new File(uri.getPath()).getAbsolutePath();
					if ( newFont.toLowerCase().endsWith(".ttf") | newFont.toLowerCase().endsWith(".ttc") ) {
						SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).edit();
						editor.putString("selectfont", newFont);
						editor.commit();
						Toast.makeText(this.getActivity(), newFont, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(this.getActivity(), "Ҫѡ���׺Ϊ.ttf/.ttc�������ļ�", Toast.LENGTH_SHORT).show();
					}
				}
				break;
			}
			super.onActivityResult(requestCode, resultCode, data);
		}
		
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
