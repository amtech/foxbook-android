package com.linpinger.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import android.app.ListActivity;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class Activity_FileChooser extends Ext_ListActivity_4Eink {
	final String[] jumpToPathList = new String[] {
			"/storage/sdcard1/",
			"/sdcard/",
			"/sdcard/10_usr/",
			"/sdcard/20_mov/",
			"/sdcard/99_sync/"
	}; // ��ת·���б�����������
	ListView lv ;
	SimpleAdapter adapter ;
	List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
	File nowDir ;
	private boolean haveFileToCopy = false ;
	private boolean haveFileToMove = false ; // �Ƿ����ļ����ƶ�
	File fileFromMark ; // ���ƶ��ļ�
	private boolean isHideDotFiles = false ;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showHomeUp() {
		getActionBar().setDisplayHomeAsUpEnabled(true);  // ����������ӷ���ͼ��
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		showHomeUp();

		lv = this.getListView();
		adapter = new SimpleAdapter(this, data,
				android.R.layout.simple_list_item_2,
				new String[] { "name", "info" },
				new int[] { android.R.id.text1, android.R.id.text2 });
		lv.setAdapter(adapter);
		init_LV_item_Long_click();

		Intent itt = getIntent(); // ��ȡ���������
		if ( itt.getStringExtra("dir") != null ) {
			File inDir = new File(itt.getStringExtra("dir")); // ����ʱ����Ŀ¼
			if ( inDir.exists() & inDir.isDirectory() )
				nowDir = inDir;
			else
				nowDir = Environment.getExternalStorageDirectory();
		} else {
			nowDir = Environment.getExternalStorageDirectory();
		}

		showFileList(nowDir);
	}  // onCreate End

	public class ComparatorName implements Comparator<Object>{
		@SuppressWarnings("unchecked")
		@Override
		public int compare(Object arg0, Object arg1) {
			HashMap<String, Object> hm0 = (HashMap<String, Object>)arg0;
			HashMap<String, Object> hm1 = (HashMap<String, Object>)arg1;
			Integer order0 = (Integer) hm0.get("order");
			Integer order1 = (Integer) hm1.get("order");
			if ( order0 < order1)
				return -1;
			if ( order0 > order1 )
				return 1;
			if ( order0 == order1 ) {  // ��������ͬʱ���Ƚ�����
				String name0 = (String) hm0.get("name");
				String name1 = (String) hm1.get("name");
				return name0.compareTo(name1);
			}
			return 0;
		}
	}

	@SuppressLint("SimpleDateFormat")
	private void showFileList(File nowDir) {
		this.setTitle(nowDir.getPath());

		data.clear();
		HashMap<String, Object> hm = new HashMap<String, Object>();
		hm.put("name", "..");
		hm.put("info", "����");
		hm.put("order", 0);   // �б�������
		data.add(hm);

		for ( File xx : nowDir.listFiles() ) {
			if (isHideDotFiles) {
				if ( xx.getName().startsWith(".") )
					continue;
			}
			hm = new HashMap<String, Object>();
			hm.put("name", xx.getName());
			if ( xx.isDirectory() ) {
				hm.put("order", 1);
				hm.put("info", (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")).format(xx.lastModified()) + "���ļ���");
			} else {
				hm.put("order", 2);
				hm.put("info", (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")).format(xx.lastModified()) + "���ļ���С��" + xx.length());
			}
			data.add(hm);
		}
		Collections.sort(data, new ComparatorName()); // ����

		adapter.notifyDataSetChanged();
		this.setItemPos4Eink(); // ����λ�÷ŵ�ͷ��
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		@SuppressWarnings("unchecked")
		HashMap<String, Object> nhm = (HashMap<String, Object>) l.getItemAtPosition(position);
		String nowName = (String)nhm.get("name");
		File clickFile;
		if ( nowName.equalsIgnoreCase("..") ) {
			if ( nowDir.getPath().equals("/") ){
				foxtip("��ŷ���Ѿ�����Ŀ¼��");
				return;
			} else {
				clickFile = nowDir.getParentFile();
			}
		} else {
			clickFile = new File(nowDir, nowName);
		}
		if ( clickFile.isDirectory() ) {
			nowDir = clickFile ;
			showFileList(nowDir);
		} else {
			this.setResult(RESULT_OK, new Intent().setData(Uri.fromFile(clickFile))); // �����ļ�·��
			this.finish();
		}
		super.onListItemClick(l, v, position, id);
	}

	private void init_LV_item_Long_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		final Builder builder = new AlertDialog.Builder(this);
		OnItemLongClickListener longlistener = new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> adptv, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> hm = (HashMap<String, Object>) adptv.getItemAtPosition(position);
				final String lcName = (String) hm.get("name");
//				final int lcPos = position;

				builder.setTitle("����:" + lcName);
				final String[] aList = new String[] { "��ת��", "ˢ���б�", "������", "����", "����", "ճ��", "�����ļ���", "ճ���������ı�����Txt��" , "ɾ��"};
				builder.setItems( aList, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int which) {
						String aa = aList[which];
						if ( aa.equalsIgnoreCase("��ת��") ) {
							jumpDialog();
						} else if ( aa.equalsIgnoreCase("ˢ���б�") ) {
							showFileList(nowDir);
						} else if ( aa.equalsIgnoreCase("������") ) {
							renameDialog(new File(nowDir, lcName));
						} else if ( aa.equalsIgnoreCase("����") ) {
							haveFileToCopy = true;
							fileFromMark = new File(nowDir, lcName);
							foxtip("׼������: " + lcName + "\n������ճ����Ŀ¼ճ��");
						} else if ( aa.equalsIgnoreCase("����") ) {
							haveFileToMove = true;
							fileFromMark = new File(nowDir, lcName);
							foxtip("׼���ƶ�: " + lcName + "\n������ճ����Ŀ¼ճ��");
						} else if ( aa.equalsIgnoreCase("ճ��") ) {
							if ( fileFromMark == null ) {
								foxtip("����֪��Ҫ�����ĸ��ļ���");
								return;
							}
							File fileTo = new File(nowDir, fileFromMark.getName());
							ToolJava.renameIfExist(fileTo); // ��������
							if ( haveFileToMove ) {
								if ( fileFromMark.renameTo(fileTo) ) { // ͬ�ļ�ϵͳ�ƶ�
									haveFileToMove = false;
									showFileList(nowDir);
								} else { // ʧ�ܿ����ǿ��ļ�ϵͳ�ˣ��ȸ��ƣ���ɾ��
									haveFileToCopy = true;
								}
							}
							if ( haveFileToCopy ) { // ���ļ�ϵͳ���ƣ����Ҫ�ŵ�����һ�㣬ǰ���ƶ����ܻ��õ�
								if ( fileFromMark.length() == ToolJava.copyFile(fileFromMark, fileTo) ) {
									haveFileToCopy = false ;
									showFileList(nowDir);
								} else {
									foxtip("����ʧ��: " + fileTo.getName());
								}
							}
							if ( haveFileToMove ) { // �Ѿ����ƣ�׼��ɾ��
								if ( fileFromMark.length() == fileTo.length() )
									fileFromMark.delete();
								haveFileToMove = false ;
							}
						} else if ( aa.equalsIgnoreCase("�����ļ���") ) {
							ToolAndroid.setClipText(lcName, getApplicationContext());
							foxtip("������:\n" + lcName);
						} else if ( aa.equalsIgnoreCase("ճ���������ı�����Txt��") ) {
							String xx = ToolAndroid.getClipText(getApplicationContext());
							String txtName = (new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss")).format(new java.util.Date()) + ".txt";
							ToolJava.writeText(xx, new File(nowDir, txtName).getPath());
							showFileList(nowDir);
							foxtip("���浽: " + txtName);
						} else if ( aa.equalsIgnoreCase("ɾ��") ) {
							deleteDialog(new File(nowDir, lcName));
						} else {
							foxtip("һ����Ȧ");
						}
					}
				});
				builder.create().show();
				return true;
			}
		};
		lv.setOnItemLongClickListener(longlistener);
	}

	private void renameDialog(final File fRename) {
		final EditText newName = new EditText(this);
		newName.setText(fRename.getName());  // �༭��������Ϊԭ����
		Builder dlg = new AlertDialog.Builder(this);
		dlg.setTitle("������: " + fRename.getName());
		dlg.setView(newName);
		dlg.setPositiveButton("ȷ��", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				File newFile = new File(fRename.getParentFile(), newName.getText().toString());
				if ( newFile.exists() ) {
					foxtip("�ļ��Ѵ���: " + newFile.getName());
				} else {
					fRename.renameTo(newFile);
					foxtip("������ " + fRename.getName() + " Ϊ: " + newFile.getName());
					showFileList(nowDir);
				}
			}
		});
		dlg.setNegativeButton("ȡ��", null);
		dlg.create().show();
	}

	private void deleteDialog(final File fDelete) {
		new AlertDialog.Builder(this)
		.setTitle("ȷ���Ƿ�ɾ���ļ�")
		.setMessage("ȷ��ɾ��: " + fDelete.getName())
		.setPositiveButton("ȷ��", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface arg0, int arg1) {
				if ( ToolJava.deleteDir(fDelete) ) {
					showFileList(nowDir);
					foxtip("��ɾ��:\n" + fDelete.getName());
				} else {
					foxtip("ɾ��ʧ��:\n" + fDelete.getName());
				}
			}
		})
		.setNegativeButton("ȡ��", null)
		.create().show();
	}

	private void jumpDialog() { // ȫ��: jumpToPathList, nowDir
		new AlertDialog.Builder(this)
		.setTitle("��ת������Ŀ¼")
		.setItems( jumpToPathList, new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface arg0, int which) {
				File ff = new File(jumpToPathList[which]);
				if ( ff.exists() ) {
					if ( ff.isDirectory() ) {
						nowDir = ff ;
						showFileList(ff);
					} else {
						foxtip("��Ŀ¼: " + ff.getPath());
					}
				} else {
					foxtip("������: " + ff.getPath());
				}
			}
		})
		.create().show();
	}

	public boolean onOptionsItemSelected(MenuItem item) { // ��Ӧѡ��˵��Ķ���
		switch (item.getItemId()) {
		case android.R.id.home:
			this.setResult(RESULT_CANCELED);
			this.finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void foxtip(String sinfo) { // Toast��Ϣ
		Toast.makeText(getApplicationContext(), sinfo, Toast.LENGTH_SHORT).show();
	}

}
