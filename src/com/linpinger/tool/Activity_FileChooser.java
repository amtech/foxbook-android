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
		dlg.show();
	}

	private void init_LV_item_Long_click() { // ��ʼ�� ���� ��Ŀ ����Ϊ
		final Builder builder = new AlertDialog.Builder(this);
		OnItemLongClickListener longlistener = new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> adptv, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> hm = (HashMap<String, Object>) adptv.getItemAtPosition(position);
				final String lcName = (String) hm.get("name");
				final int lcPos = position;
				
				// builder.setIcon(R.drawable.ic_launcher);
				builder.setTitle("����:" + lcName);
				builder.setItems(new String[] { "ˢ��", "������", "����", "����", "ճ��", "�����ļ���", "ճ���������ı�����Txt��" , "ɾ��"},
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,int which) {
								switch (which) {
								case 0: // ˢ��
									showFileList(nowDir);
									break;
								case 1: // ������
									renameDialog(new File(nowDir, lcName));
									break;
								case 2: // ����
									haveFileToCopy = true;
									fileFromMark = new File(nowDir, lcName);
									break;
								case 3: // ����
									haveFileToMove = true;
									fileFromMark = new File(nowDir, lcName);
									foxtip("׼���ƶ�: " + lcName + "\n������ճ����Ŀ¼ճ��\nע��Ҫ��ͬһ�ļ�ϵͳ��");
									break;
								case 4: // ճ��
									File fileTo = new File(nowDir, fileFromMark.getName());
									ToolJava.renameIfExist(fileTo);
									if (haveFileToCopy) { // ���ļ�ϵͳ����
										if ( fileFromMark.length() == ToolJava.copyFile(fileFromMark, fileTo) ) {
											haveFileToCopy = false ;
											showFileList(nowDir);
										} else {
											foxtip("����ʧ��: " + fileTo.getName());
										}
									}
									if ( haveFileToMove ) { // ͬ�ļ�ϵͳ�ƶ�
										if ( fileFromMark.renameTo(fileTo) ) {
											haveFileToMove = false;
											showFileList(nowDir);
										} else {
											foxtip("�ƶ�ʧ�ܣ�ע��Ҫ��ͬһ�ļ�ϵͳ���ƶ�\n" + fileFromMark.getName());
										}
									}
									break;
								case 5: // �����ļ���
									ToolAndroid.setClipText(lcName, getApplicationContext());
									foxtip("������:\n" + lcName);
									break;
								case 6:
									String xx = ToolAndroid.getClipText(getApplicationContext());
									String txtName = (new java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss")).format(new java.util.Date()) + ".txt";
									ToolJava.writeText(xx, new File(nowDir, txtName).getPath());
									showFileList(nowDir);
									foxtip("���浽: " + txtName);
									break;
								case 7:  // ɾ��
									if ( ToolJava.deleteDir(new File(nowDir, lcName)) ) {
										data.remove(lcPos);
										adapter.notifyDataSetChanged();
										foxtip("��ɾ��:\n" + lcName);
									} else {
										foxtip("ɾ��ʧ��:\n" + lcName);
									}
									break;
								default:
									foxtip("һ����Ȧ");
									break;
								}
							}
						});
				builder.create().show();

				return true;
			}

		};
		lv.setOnItemLongClickListener(longlistener);
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
