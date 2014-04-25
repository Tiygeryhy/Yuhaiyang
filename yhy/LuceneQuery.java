package com.ws.yhy;

import com.chenlb.mmseg4j.analysis.ComplexAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.Math.min;

public class LuceneQuery {
    private  Directory dir = null;
    public class scoreItem{
        public String name = null;
        public Float value = 0.0f;
        scoreItem(String na,Float val){
            name = na;
            value = val;
        }
    }
    IndexReader reader = null;
    IndexSearcher searcher = null;
    Map<String,Integer> appearTimes = null;
    Map<String,Float> scoreCalculate = null;
    public void close() throws IOException {
        reader.close();
    }

    public LuceneQuery(String indexDic) throws IOException {
        dir = FSDirectory.open(new File(indexDic));
        reader=DirectoryReader.open(dir);
        searcher=new IndexSearcher(reader);
        appearTimes = new HashMap<String, Integer>();
        scoreCalculate = new HashMap<String, Float>();
    }

    public  boolean preQueryTerm(String filedString,String queryString,int maxQuery,int ScoreValue) throws IOException {

        Term term=new Term(filedString, queryString);
        PrefixQuery query = new PrefixQuery(term);
        //TermQuery query=new TermQuery(term);
        TopDocs topdocs=searcher.search(query, maxQuery);
        ScoreDoc[] scoreDocs=topdocs.scoreDocs;
       // System.out.println("查询结果总数---" + topdocs.totalHits+"最大的评分--"+topdocs.getMaxScore());
        for(int i=0; i < scoreDocs.length; i++) {
            int doc = scoreDocs[i].doc;
            Document document = searcher.doc(doc);
            String albumname = document.get("albumname");
            if(appearTimes.containsKey(albumname)){
                appearTimes.put(albumname,appearTimes.get(albumname)+1);
            }else{
                appearTimes.put(albumname,1);
            }
            if(scoreCalculate.containsKey(albumname)){
                scoreCalculate.put(albumname,scoreCalculate.get(albumname)+scoreDocs[i].score);
            }else{
                scoreCalculate.put(albumname,scoreDocs[i].score+Float.parseFloat(document.get("tvyear"))%100+ScoreValue);
            }
        }
        return true;
    }
    public List<scoreItem> queryTerm(String filedString, String queryString, int maxQuery) throws IOException, ParseException {
        QueryParser parser = new QueryParser(Version.LUCENE_40,filedString+"FenciPinyin",new ComplexAnalyzer());
        Query query = parser.parse(LuceneIndex.convertToPinyin(LuceneIndex.getAnalyzer(queryString)));
        TopDocs topdocs=searcher.search(query, maxQuery);
        ScoreDoc[] scoreDocs=topdocs.scoreDocs;
        ArrayList<scoreItem> ls = new ArrayList<scoreItem>();
        for(int i=0; i < scoreDocs.length; i++) {
            int doc = scoreDocs[i].doc;
            Document document = searcher.doc(doc);
            ls.add(new scoreItem(document.get("albumname"),scoreDocs[i].score));
        }
        List<scoreItem> list =ls.subList(0,min(maxQuery,ls.size()));
        clear();
        return list;
    }
    public List<scoreItem> getSortedResult(int maxQuery){
        ArrayList<scoreItem> scoreItems = new ArrayList<scoreItem>();
        Iterator iterator = scoreCalculate.keySet().iterator();
        while(iterator.hasNext()){
            String keyV = (String )iterator.next();
            scoreItems.add(new scoreItem(keyV,scoreCalculate.get(keyV)));
        }
        Collections.sort(scoreItems, new Comparator<scoreItem>() {
            @Override
            public int compare(scoreItem o1, scoreItem o2) {
                return  o2.value.compareTo(o1.value);
            }
        });
        return scoreItems.subList(0, min(maxQuery, scoreItems.size()));
    }
    public  List<scoreItem> searchPreQuery(String field,String queryString,int maxQuery) throws IOException {
        preQueryTerm(field+"WholeName", queryString, maxQuery,100);
        if(scoreCalculate.size()<maxQuery)
        {
            preQueryTerm(field+"Pinyin",LuceneIndex.getAnalyzer(LuceneIndex.convertToPinyin(queryString)).split("\t")[0],maxQuery-scoreCalculate.size(),50);
        }
        if(scoreCalculate.size()<maxQuery){
            String[] queryStrings = LuceneIndex.getAnalyzer(queryString).split("\t");
            preQueryTerm(field+"WholeName", queryStrings[0], maxQuery-scoreCalculate.size(),0);
        }
        List<scoreItem> list = getSortedResult(maxQuery);
        clear();
        return list;
    }
    public void outputScoreQuery(){
        Iterator iterator = scoreCalculate.keySet().iterator();
        while(iterator.hasNext()){
            String keyV = (String )iterator.next();
            System.out.println(keyV+" "+scoreCalculate.get(keyV));
        }
    }
    public void outputTimesQuery(){
        Iterator iterator = appearTimes.keySet().iterator();
        while(iterator.hasNext()){
            String keyV = (String )iterator.next();
            System.out.println(keyV+" "+appearTimes.get(keyV));
        }
    }
    public static void print(List<scoreItem> scoreItems){
        for(int i=0;i<scoreItems.size();i++){
            System.out.println(scoreItems.get(i).name+" "+scoreItems.get(i).value);
        }
    }

    public void clear(){
        appearTimes.clear();
        scoreCalculate.clear();
    }
    public static void main(String[] args) throws Exception {
        String s1 = "我们";
        String s2 = "我门";
        LuceneQuery luceneQuery = new LuceneQuery("D:\\DBInf\\index");  //建立一个query
        luceneQuery.queryTerm("albumname", "爱情 谁怕谁", 5);
        /*
        ArrayList<scoreItem> sortedItems = luceneQuery.albumnamePreQuery(s1, 5);      //得到一个排序后的检索数组
        System.out.println("--------------" + s1 + "----------------");
        luceneQuery.print(sortedItems);
        luceneQuery.clear();
        luceneQuery.close();
        */
    }
}
