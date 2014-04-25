package com.ws.yhy;

import java.io.*;

/**
 * Created by WS on 14-4-16.
 */
public class IO {
    private static boolean correctFile(String fileName) throws IOException {
        return correctFile(fileName,"GB2312");
    }//纠正单独文件
    private static boolean correctFile(String fileName, String charSet) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),charSet));
        String correctFileName = fileName+"corrected";
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(correctFileName),charSet));
        String stringTmp = null;
        int lineNumber = 0;
        String[] labelValue = null;
        String outputString = null;
        int labelNumber = 0;
        int labelValueNumberTmp = 0;
        int labelNumberTmp = 0;
        while((stringTmp = br.readLine())!=null){
            if(lineNumber++ == 0){
                labelNumber = stringTmp.split("\t").length;
                bw.write(stringTmp+"\r\n");
                continue;
            }
            labelValueNumberTmp = stringTmp.split("\t").length;
            if(labelValueNumberTmp  == labelNumber){
                if(labelNumberTmp!=0){
                    System.err.println("Read File Error, maybe because your" +
                            " files are missing some label values.");
                    return false;
                }
                bw.write(stringTmp+"\r\n");
            }else{
                if(outputString == null){
                    outputString = stringTmp;
                    labelNumberTmp = labelValueNumberTmp;
                }else{
                    outputString+=stringTmp;
                    labelNumberTmp = labelNumberTmp+labelValueNumberTmp -1;
                    if(labelNumberTmp == labelNumber){
                        bw.write(outputString+"\r\n");
                        outputString = null;
                        labelNumberTmp = 0;
                    }
                }
            }
        }
        bw.close();
        br.close();
        return true;
    }//纠正单独文件
    public static boolean correctFiles(String filesName) throws IOException {
        return correctFiles(filesName,"GB2312");
    }//纠正文件夹内所有循环
    public static boolean correctFiles(String filesName,String charSet) throws IOException {
        File f = new File(filesName);
        if(f.isDirectory()){
            String[] files = f.list();
            for(int i=0;i<files.length;i++){
                if(correctFiles(filesName+"\\"+files[i],charSet)==false) return false;
            }
        }
        else{

            if(filesName.length() == filesName.lastIndexOf("corrected")+9) return true;
            else if(filesName.contains("csv"))return correctFile(filesName,charSet);
        }
        return true;
    }//纠正文件夹内所有循环

    public static void main(String[] args) throws IOException {
        if(correctFiles("D:\\DBInf")==false) System.err.println("Read File Error, maybe because your" +
                " files are missing some label values.");
    }
}
