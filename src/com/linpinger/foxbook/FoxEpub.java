package com.linpinger.foxbook;


/**
 *
 * @author guanli
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FoxEpub {

    private FileOutputStream fos;
    private ZipOutputStream zos;
    String SavePath = "/xxx.epub";
    String TmpDir = "/";
    boolean isEpub = true;
    String BookUUID = UUID.randomUUID().toString();
    String BookName = "����֮��";
    String BookCreator = "������֮��";
    String DefNameNoExt = "FoxMake"; //Ĭ���ļ���
    String ImageExt = "png";
    String ImageMetaType = "image/png";
    ArrayList<HashMap<String, Object>> Chapter = new ArrayList<HashMap<String, Object>>(200); //�½ڽṹ:1:ID 2:Title
    int ChapterCount = 0; //�½���
    int ChapterID = 100; //�½�ID

    FoxEpub(String inBookName, String inSavePath) {
        this.BookName = inBookName;
        this.SavePath = inSavePath;
        if (inSavePath.toLowerCase().endsWith(".epub")) {
            this.isEpub = true;
        } else {
            this.isEpub = false;
        }
        File saveF = new File(this.SavePath);
        String inSaveDir = saveF.getParent();
        this.TmpDir = inSaveDir + File.separator + "FoxEpub_" + System.currentTimeMillis();

        if (isEpub) {
            if (saveF.exists()) { // �ļ�����
                saveF.renameTo(new File(this.SavePath + System.currentTimeMillis()));
            }
            try {
                fos = new FileOutputStream(this.SavePath);
            } catch (FileNotFoundException ex) {
                System.out.println("����epub�ļ�����: " + ex.toString());
            }
            zos = new ZipOutputStream(fos);
        } else { // mobi
            File td = new File(this.TmpDir);
            if (!td.exists()) {
                new File(this.TmpDir + File.separator + "html").mkdirs();
                new File(this.TmpDir + File.separator + "META-INF").mkdirs();
            } else {
                System.out.println("����:Ŀ¼����: " + this.TmpDir);
            }
        }
    }

    public void AddChapter(String Title, String Content, int iPageID) {
        if (iPageID < 0) {
            ++this.ChapterID;
        } else {
            this.ChapterID = iPageID;
        }

        HashMap<String, Object> cc = new HashMap<String, Object>();
        cc.put("id", this.ChapterID);
        cc.put("name", Title);
        Chapter.add(cc);

        this._CreateChapterHTML(Title, Content, this.ChapterID); //д���ļ�
    }

    public void SaveTo() {
        this._CreateIndexHTM();
        this._CreateNCX();
        this._CreateOPF();
        this._CreateEpubMiscFiles();

        if (isEpub) {
            try {
                zos.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { // ����mobi
            try {
                Process cmd = Runtime.getRuntime().exec("kindlegen " + DefNameNoExt + ".opf", null, new File(TmpDir));
                cmd.waitFor();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            File tmpF = new File(TmpDir + File.separator + DefNameNoExt + ".mobi");
            if (tmpF.exists() && tmpF.length() > 555) {
                tmpF.renameTo(new File(SavePath));
                DeleteFolder(TmpDir); // �Ƴ���ʱĿ¼
            }
        }
    }

    private void _CreateNCX() { //����NCX�ļ�
        StringBuffer NCXList = new StringBuffer(4096);
        int DisOrder = 1; //��ʼ ˳��, ���������playOrder����

        HashMap<String, Object> mm;
        int nowID = 0;
        String nowTitle = "";
        Iterator<HashMap<String, Object>> itr = Chapter.iterator();
        while (itr.hasNext()) {
            mm = itr.next();
            nowID = (Integer) mm.get("id");
            nowTitle = (String) mm.get("name");
            ++DisOrder;
            NCXList.append("\t<navPoint id=\"").append(nowID)
                    .append("\" playOrder=\"").append(DisOrder)
                    .append("\"><navLabel><text>").append(nowTitle)
                    .append("</text></navLabel><content src=\"html/").append(nowID)
                    .append(".html\" /></navPoint>\n");
        }

        StringBuffer XML = new StringBuffer(4096);
        XML.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE ncx PUBLIC \"-//NISO//DTD ncx 2005-1//EN\" \"http://www.daisy.org/z3986/2005/ncx-2005-1.dtd\">\n<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\" xml:lang=\"zh-cn\">\n<head>\n\t<meta name=\"dtb:uid\" content=\"")
                .append(BookUUID).append("\"/>\n\t<meta name=\"dtb:depth\" content=\"1\"/>\n\t<meta name=\"dtb:totalPageCount\" content=\"0\"/>\n\t<meta name=\"dtb:maxPageNumber\" content=\"0\"/>\n\t<meta name=\"dtb:generator\" content=\"")
                .append(BookCreator).append("\"/>\n</head>\n<docTitle><text>")
                .append(BookName).append("</text></docTitle>\n<docAuthor><text>")
                .append(BookCreator).append("</text></docAuthor>\n<navMap>\n\t<navPoint id=\"toc\" playOrder=\"1\"><navLabel><text>Ŀ¼:")
                .append(BookName).append("</text></navLabel><content src=\"").append(DefNameNoExt).append(".htm\"/></navPoint>\n")
                .append(NCXList).append("\n</navMap></ncx>\n");
        if (isEpub) {
            this.addTextToZip(DefNameNoExt + ".ncx", XML.toString());
        } else {
            createTxtFile(new File(this.TmpDir + File.separator + DefNameNoExt + ".ncx"), XML.toString());
        }
    }

    private void _CreateOPF() { //����OPF�ļ�
        String AddXMetaData = "";
        StringBuffer NowHTMLMenifest = new StringBuffer(4096);
        StringBuffer NowHTMLSpine = new StringBuffer(4096);

        HashMap<String, Object> mm;
        int nowID = 0;
//        String nowTitle = "";
        Iterator<HashMap<String, Object>> itr = Chapter.iterator();
        while (itr.hasNext()) {
            mm = itr.next();
            nowID = (Integer) mm.get("id");
//            nowTitle = (String)mm.get("name");

            NowHTMLMenifest.append("\t<item id=\"page").append(nowID).append("\" media-type=\"application/xhtml+xml\" href=\"html/").append(nowID).append(".html\" />\n");
            NowHTMLSpine.append("\t<itemref idref=\"page").append(nowID).append("\" />\n");
        }

        // ͼƬ�б��������
        String NowImgMenifest = "";
        if (!isEpub) {
            AddXMetaData = "\t<x-metadata><output encoding=\"utf-8\"></output></x-metadata>\n";
        }

        StringBuffer XML = new StringBuffer(4096);
        XML.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\" unique-identifier=\"FoxUUID\">\n<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n\t<dc:title>")
                .append(BookName).append("</dc:title>\n\t<dc:identifier opf:scheme=\"uuid\" id=\"FoxUUID\">").append(BookUUID)
                .append("</dc:identifier>\n\t<dc:creator>").append(BookCreator).append("</dc:creator>\n\t<dc:publisher>")
                .append(BookCreator).append("</dc:publisher>\n\t<dc:language>zh-cn</dc:language>\n").append(AddXMetaData)
                .append("</metadata>\n\n\n<manifest>\n\t<item id=\"FoxNCX\" media-type=\"application/x-dtbncx+xml\" href=\"")
                .append(DefNameNoExt).append(".ncx\" />\n\t<item id=\"FoxIDX\" media-type=\"application/xhtml+xml\" href=\"")
                .append(DefNameNoExt).append(".htm\" />\n\n").append(NowHTMLMenifest).append("\n\n")
                .append(NowImgMenifest).append("\n</manifest>\n\n<spine toc=\"FoxNCX\">\n\t<itemref idref=\"FoxIDX\"/>\n\n\n")
                .append(NowHTMLSpine).append("\n</spine>\n\n\n<guide>\n\t<reference type=\"text\" title=\"����\" href=\"")
                .append("html/").append(Chapter.get(0).get("id")).append(".html\"/>\n\t<reference type=\"toc\" title=\"Ŀ¼\" href=\"")
                .append(DefNameNoExt).append(".htm\"/>\n</guide>\n\n</package>\n\n");
        if (isEpub) {
            this.addTextToZip(DefNameNoExt + ".opf", XML.toString());
        } else {
            createTxtFile(new File(this.TmpDir + File.separator + DefNameNoExt + ".opf"), XML.toString());
        }
    }

    private void _CreateEpubMiscFiles() { //���� epub �����ļ� mimetype, container.xml
        StringBuffer XML = new StringBuffer(256);
        XML.append("<?xml version=\"1.0\"?>\n<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n\t<rootfiles>\n\t\t<rootfile full-path=\"")
                .append(this.DefNameNoExt).append(".opf")
                .append("\" media-type=\"application/oebps-package+xml\"/>\n\t</rootfiles>\n</container>\n");
        if (isEpub) {
            addMIMETYPE();
            addTextToZip("META-INF/container.xml", XML.toString());
        } else {
            createTxtFile(new File(this.TmpDir + File.separator + "mimetype"), "application/epub+zip");
            createTxtFile(new File(this.TmpDir + File.separator + "META-INF" + File.separator + "container.xml"), XML.toString());
        }
    }

    private void _CreateChapterHTML(String Title, String Content, int iPageID) { //�����½�ҳ��
        StringBuffer HTML = new StringBuffer(20480);

        HTML.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"zh-CN\">\n<head>\n\t<title>")
                .append(Title)
                .append("</title>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n\t<style type=\"text/css\">\n\t\th2,h3,h4{text-align:center;}\n\t\tp { text-indent: 2em; line-height: 0.5em; }\n\t</style>\n</head>\n<body>\n<h4>")
                .append(Title)
                .append("</h4>\n<div class=\"content\">\n\n\n")
                .append(Content)
                .append("\n\n\n</div>\n</body>\n</html>\n");

        if (isEpub) {
            this.addTextToZip("html" + File.separator + iPageID + ".html", HTML.toString());
        } else {
            createTxtFile(new File(this.TmpDir + File.separator + "html" + File.separator + iPageID + ".html"), HTML.toString());
        }
    }

    private void _CreateIndexHTM() { //��������ҳ
        StringBuffer NowTOC = new StringBuffer(4096);

        HashMap<String, Object> mm;
        int nowID = 0;
        String nowTitle = "";
        Iterator<HashMap<String, Object>> itr = Chapter.iterator();
        while (itr.hasNext()) {
            mm = itr.next();
            nowID = (Integer) mm.get("id");
            nowTitle = (String) mm.get("name");
            NowTOC.append("<div><a href=\"html/").append(nowID).append(".html\">").append(nowTitle).append("</a></div>\n");
        }

        StringBuffer XML = new StringBuffer(4096);
        XML.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"zh-CN\">\n<head>\n\t<title>")
                .append(BookName).append("</title>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n\t<style type=\"text/css\">h2,h3,h4{text-align:center;}</style>\n</head>\n<body>\n<h2>")
                .append(BookName).append("</h2>\n<div class=\"toc\">\n\n").append(NowTOC).append("\n\n</div>\n</body>\n</html>\n");
        if (isEpub) {
            this.addTextToZip(DefNameNoExt + ".htm", XML.toString());
        } else {
            createTxtFile(new File(this.TmpDir + File.separator + DefNameNoExt + ".htm"), XML.toString());
        }
    }

    public void addTextToZip(String saveName, String content) {
        try {
            byte[] b = content.getBytes("UTF-8");
            ZipEntry entry2 = new ZipEntry(saveName);
            zos.putNextEntry(entry2);
            zos.write(b, 0, b.length);
            zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMIMETYPE() {
        byte[] b = "application/epub+zip".getBytes();
        ZipEntry entry = new ZipEntry("mimetype");
        entry.setMethod(0);
        entry.setSize(b.length);
        entry.setCompressedSize(b.length);
        CRC32 crc = new CRC32();
        crc.update(b);
        entry.setCrc(crc.getValue());
        try {
            zos.putNextEntry(entry);
            zos.write(b, 0, b.length);
            zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // �������������������������һ��ͨ�õġ����������������������������������������
    
    public static void createTxtFile(File txtFile, String cc) { // ����Txt�ļ�
        try {
            txtFile.createNewFile();
            FileOutputStream outImgStream = new FileOutputStream(txtFile);
            outImgStream.write(cc.getBytes("UTF-8"));
            outImgStream.close();
        } catch (Exception e) {
            e.toString();
        }
    }

    /**
     * ����·��ɾ��ָ����Ŀ¼���ļ������۴������
     *
     * @param sPath Ҫɾ����Ŀ¼���ļ�
     * @return ɾ���ɹ����� true�����򷵻� false��
     */
    public boolean DeleteFolder(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        // �ж�Ŀ¼���ļ��Ƿ����  
        if (!file.exists()) {  // �����ڷ��� false  
            return flag;
        } else {
            // �ж��Ƿ�Ϊ�ļ�  
            if (file.isFile()) {  // Ϊ�ļ�ʱ����ɾ���ļ�����  
                return deleteFile(sPath);
            } else {  // ΪĿ¼ʱ����ɾ��Ŀ¼����  
                return deleteDirectory(sPath);
            }
        }
    }

    /**
     * ɾ�������ļ�
     *
     * @param sPath ��ɾ���ļ����ļ���
     * @return �����ļ�ɾ���ɹ�����true�����򷵻�false
     */
    public boolean deleteFile(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        // ·��Ϊ�ļ��Ҳ�Ϊ�������ɾ��  
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * ɾ��Ŀ¼���ļ��У��Լ�Ŀ¼�µ��ļ�
     *
     * @param sPath ��ɾ��Ŀ¼���ļ�·��
     * @return Ŀ¼ɾ���ɹ�����true�����򷵻�false
     */
    public boolean deleteDirectory(String sPath) {
        //���sPath�����ļ��ָ�����β���Զ�����ļ��ָ���  
        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }
        File dirFile = new File(sPath);
        //���dir��Ӧ���ļ������ڣ����߲���һ��Ŀ¼�����˳�  
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        //ɾ���ļ����µ������ļ�(������Ŀ¼)  
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            //ɾ�����ļ�  
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            } //ɾ����Ŀ¼  
            else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            return false;
        }
        //ɾ����ǰĿ¼  
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
//        FoxEpub oEpub = new FoxEpub("��������", "C:\\etc\\xxx.mobi");
        FoxEpub oEpub = new FoxEpub("��������", "C:\\etc\\xxx.epub");
        oEpub.AddChapter("��1��", "�����㵱���۷�����ˮ˾����Ƶط�<br>\n���ȼ�ʷ���ķǷ����ʾ", -1);
        oEpub.AddChapter("��2��", "��2���㵱���۷�����ˮ˾����Ƶط�<br>\n���ȼ�ʷ��22222222�ķǷ����ʾ", -1);
        oEpub.SaveTo();
    }
}

