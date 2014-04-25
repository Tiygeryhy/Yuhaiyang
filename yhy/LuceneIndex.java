package com.ws.yhy;
import com.chenlb.mmseg4j.analysis.ComplexAnalyzer;
import net.sourceforge.pinyin4j.PinyinHelper;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;

public class LuceneIndex {
    private  String charSetDefault = "GB2312";    //默认读取文件字符集
    private String detailField = null;
    private boolean[] detailFieldVec = null;
    public LuceneIndex(){
        detailField = "";
    }
    public void addDetailField(String field){
        detailField += "\t"+field+"\t";
    }

    private  boolean indexTVFile(String file) throws Exception {
        return indexTVFile(file, "Index", charSetDefault);
    }//索引文件
    private  boolean indexTVFile(String file,String indexPath) throws Exception {
        return indexTVFile(file, indexPath, charSetDefault);
    }//索引文件到目标目录
    private  IndexWriter getIndexWriter(String indexPath) throws Exception {
        ComplexAnalyzer analyzer=new ComplexAnalyzer();//默认分词器
        FSDirectory fs = FSDirectory.open(new File(indexPath));
        IndexWriterConfig iwc=new IndexWriterConfig(Version.LUCENE_40, analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        return new IndexWriter(fs, iwc);
    }//获得indexWriter
    private  void indexDetail(String filedName,String value,Document doc) throws IOException {
        String pinyinString =convertToPinyin(value);
        String fenciPinyinString = convertToPinyin(getAnalyzer(value));
        doc.add(new TextField(filedName+"FenciPinyin",fenciPinyinString,Store.NO));
        doc.add(new TextField(filedName+"Pinyin",pinyinString,Store.NO));
        doc.add(new StringField(filedName+"WholeName",value,Store.NO));
        doc.add(new TextField(filedName, value, Store.YES));
    }
    private  boolean indexTVFile(String file,String indexPath,String charSet) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),charSet));
        int lineNumber = 0;
        String tmpString = null;
        String[] label = null;
        String[] labelValue = null;
        IndexWriter writer = getIndexWriter(indexPath);//默认用mmseg4j词法分析器,得到写index的一个writer
        while((tmpString = br.readLine())!=null){
            if(lineNumber++==0){
                label = tmpString.split("\t");
                detailFieldVec  = new boolean[label.length];
                for(int i=0;i<detailFieldVec.length;i++){
                    detailFieldVec[i]=false;
                }
                for(int i=0;i<detailFieldVec.length;i++){
                    if(detailField.contains(label[i])&&(detailField.charAt(detailField.indexOf(label[i])-1)=='\t'
                            &&(detailField.charAt(detailField.indexOf(label[i])+label[i].length())=='\t'))) {
                        //System.out.println(label[i]);
                        detailFieldVec[i]=true;
                    }
                }
                continue;
            }
            labelValue = tmpString.split("\t");
            Document doc=new Document();
            for(int i=0;i<labelValue.length;i++){
                if(detailFieldVec[i]==true){

                    indexDetail(label[i], labelValue[i], doc);
                }
                else doc.add(new TextField(label[i], labelValue[i], Store.YES));
            }
            writer.addDocument(doc);
        }
        writer.close();
        return true;
    }

    public  void setCharSet(String c){
        charSetDefault = c;
    }//设置字符集
    public  static String getAnalyzer(String s) throws IOException {
        ComplexAnalyzer analyzer=new ComplexAnalyzer();
        Reader r = new StringReader(s);
        TokenStream sf =  analyzer.tokenStream("test", r);
        String s1 = "",s2 = "";
        boolean hasnext = sf.incrementToken();
        while(hasnext){
            CharTermAttribute ta = sf.getAttribute(CharTermAttribute.class);
            s2 = ta.toString() + " ";
            s1 += s2;
            hasnext = sf.incrementToken();
        }
        return s1;
    }//显示该index所用分词器的效果
    public  boolean indexTVFiles(String file)throws Exception{
        return indexTVFiles( file, "index",charSetDefault);
    }
    public  boolean indexTVFiles(String file,String indexPath)throws Exception{
        return indexTVFiles( file, indexPath,charSetDefault);
    }
    public  static String convertToPinyin(String s){
        String pinyinString = "";
        for(int j=0;j<s.length();j++){
            if(Character.toString(s.charAt(j)).matches("[\\u4E00-\\u9FA5]+")) pinyinString+= PinyinHelper.toHanyuPinyinStringArray(s.charAt(j))[0].substring(0,PinyinHelper.toHanyuPinyinStringArray(s.charAt(j))[0].length()-1);
            else if((s.charAt(j)<='z'&&(s.charAt(j)>='a'))||(s.charAt(j)<='Z'&&(s.charAt(j)>='A'))){
                pinyinString+=s.charAt(j);
            }else pinyinString+=" "+s.charAt(j)+" ";
        }
        return pinyinString;
    }
    public  boolean indexTVFiles(String filename,String indexPath,String charSet)throws Exception{
        File f = new File(filename);

        if(f.isDirectory()){
            String[] files = f.list();
            for(int i=0;i<files.length;i++){
                if(indexTVFiles(filename + "\\" + files[i],indexPath, charSet)==false) return false;
            }
        }else{
            if(filename.indexOf("corrected")!=-1){
                System.out.println(filename);
                indexTVFile(filename, indexPath, charSet);
            }
        }
        return true;
    }



    /**
     * 初始添加文档
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        //System.out.println(getAnalyzer("贫民英雄"));
       // System.out.println(getAnalyzer("贫民英雄"));
        ///indexTVFiles("D:\\DBInf","D:\\DBInf\\index");
        //System.out.println(convertToPinyin(getAnalyzer("一米阳光")));
    }



}
