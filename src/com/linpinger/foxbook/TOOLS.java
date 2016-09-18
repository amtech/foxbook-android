package com.linpinger.foxbook;

import java.io.File;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.ClipboardManager;


public class TOOLS {
	
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list(); // �ݹ�ɾ��Ŀ¼�е���Ŀ¼��
			for (int i = 0; i < children.length; i++) {
				if (! deleteDir(new File(dir, children[i])) ) {
					return false;
				}
			}
		} // Ŀ¼��ʱΪ�գ�����ɾ��
		boolean bDeleted = false ;
		try {
			bDeleted = dir.delete();
		} catch (Exception e) {
			e.toString();
		}
		return bDeleted;
	}
    /** 
     * ��spֵת��Ϊpxֵ����֤���ִ�С���� 
     *  
     * @param spValue 
     * @param fontScale 
     *            ��DisplayMetrics��������scaledDensity�� 
     * @return 
     */  
    public static int sp2px(Context context, float spValue) {  
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
        return (int) (spValue * fontScale + 0.5f);  
    }

	/** 
     * ��pxֵת��Ϊspֵ����֤���ִ�С���� 
     *  
     * @param pxValue 
     * @param fontScale 
     *            ��DisplayMetrics��������scaledDensity�� 
     * @return 
     */  
    public static int px2sp(Context context, float pxValue) {  
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;  
        return (int) (pxValue / fontScale + 0.5f);  
    }

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void download(String iURL, String saveName, Context ctx) {
		DownloadManager downloadManager = (DownloadManager)ctx.getSystemService(Context.DOWNLOAD_SERVICE);
		DownloadManager.Request request = new DownloadManager.Request(Uri.parse(iURL));
		request.setDestinationInExternalPublicDir("99_sync", saveName);
		request.setTitle("����: " + saveName);
		request.setDescription(saveName);
		// request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		// request.setMimeType("application/cn.trinea.download.file");
		downloadManager.enqueue(request);
	}
	
	public static void setcliptext(String content, Context ctx){ // �����ı���������
		ClipboardManager cmb = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE); 
		cmb.setText(content.trim()); 
	} 

	public static String getcliptext(Context ctx) { // �Ӽ��������ı�
		ClipboardManager cmb = (ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE); 
		return cmb.getText().toString().trim(); 
	}

/*
import android.annotation.TargetApi;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.Build;
	// �°�Clip
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void copyToClipboard(String iText, Context ctx) {
		((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("hello", iText));
	}
*/

}
