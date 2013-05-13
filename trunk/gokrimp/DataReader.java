/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gokrimp;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * reads data with different formats
 * @author thoang
 */
public class DataReader {
    
    GoKrimp readData(String dataname){
        GoKrimp gk=new GoKrimp();
        gk.labels=readLabel(dataname+".lab");
        gk.data=new ArrayList();
        try{
            DataInputStream in;
            FileInputStream fstream = new FileInputStream(dataname+".dat");
            in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            int size=0;
            while((strLine = br.readLine()) != null){
                String[] temp;
                String delimiter = " ";
                temp = strLine.split(delimiter);
                ArrayList<Event> s=new ArrayList();
                gk.data.add(s);
                int ts=0,prev=0;
                size++;
                for(int i=0;i<temp.length;i++){
                    Event e=new Event();
                    e.id=Integer.parseInt(temp[i]);
                    e.ts=ts;
                    e.gap=ts-prev;
                    prev=ts;
                    gk.data.get(gk.data.size()-1).add(e);
                    ts++;
                    /*if(ts%100==0)
                     System.out.println(e.id);
                    else
                     System.out.print(e.id+" ");*/
                }
               
            }
            System.err.println("data size:"+ size);
            in.close();
        }catch (IOException e){
                System.err.println("Error: " + e.getMessage());
        }
        
        return gk;
    }
    
    HashMap<Integer,String> readLabel(String dataname){
        HashMap<Integer,String> labels= new HashMap();
        File file = new File(dataname+".lab");
        if(file.exists()){ //the label file with such name does not exist
            return labels;
        }
        try{
            DataInputStream in;
            FileInputStream fstream = new FileInputStream(dataname);
                in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine;
                int k=0;
                while((strLine = br.readLine()) != null){
                    labels.put(k, strLine);
                    k++;
                }
                in.close();
        }catch (IOException e){
                System.err.println("Error: " + e.getMessage());
        }
        return labels;
     }
     
     /**
      * read Siemens tskm data
      * @param dataname
      * @return 
      */
      GoKrimp readSiemensIntervalData(String dataname){
        GoKrimp gk=new GoKrimp();
        gk.data=new ArrayList();
        try{
         gk.classlabels =new ArrayList();
         ArrayList<Event> ve=new ArrayList();
         String intervalfile=dataname+"_intervals.int";
         String windowfile=dataname+"_windows.int";
         String labelfile=dataname+"_intervals.int.tskm";
         FileInputStream fstream = new FileInputStream(intervalfile);
         DataInputStream in = new DataInputStream(fstream);
         BufferedReader br = new BufferedReader(new InputStreamReader(in));
         String strLine;
         int max=0;
         //scan the interval file for the first time to know the number of different type of interval
         while ((strLine = br.readLine()) != null)   {
               String[] temp;
               String delimiter = "\t";
               temp = strLine.split(delimiter);
               Integer i=Integer.parseInt(temp[0]);
               if(max<i)
                 max=i;
         }
         in.close();
         fstream.close();
         fstream = new FileInputStream(intervalfile);
         in = new DataInputStream(fstream);
         br = new BufferedReader(new InputStreamReader(in));

         //scan the interval file for the second time to parse the events stream
         while ((strLine = br.readLine()) != null)   {
               String[] temp;
               String delimiter = "\t";
               temp = strLine.split(delimiter);
               Event e=new Event();
               e.id=Integer.parseInt(temp[0]);
               e.ts=Integer.parseInt(temp[1]);
               ve.add(e);
               e=new Event();
               e.id=Integer.parseInt(temp[0])+max;
               e.ts=Integer.parseInt(temp[2]);
               ve.add(e);
         }
         //sort events by increasing timestamp
         Collections.sort(ve);
         fstream.close();
         in.close();
         fstream = new FileInputStream(windowfile);
         in = new DataInputStream(fstream);
         br = new BufferedReader(new InputStreamReader(in));
         ArrayList<Integer> vw=new ArrayList();

         //scan the windows input file to get the sequence labels and the sequence start and end timestamps
        while ((strLine = br.readLine()) != null)   {
               String[] temp;
               String delimiter = "\t";
               temp = strLine.split(delimiter);
               vw.add(Integer.parseInt(temp[2])+1);
               gk.classlabels.add(Integer.parseInt(temp[0])-1);
         }
         fstream.close();
         in.close();
         int w=0;
         for(int i=0;i<vw.size();i++)
             gk.data.add(new ArrayList<Event>());

         for(int i=0;i<ve.size()&&w<vw.size();i++){
             if(ve.get(i).ts<vw.get(w))
                 gk.data.get(w).add(ve.get(i));
             else
                 w++;
         }
         for(int i=0;i<gk.data.size();i++){
             int ts=0;
             for(int j=0;j<gk.data.get(i).size();j++){
                gk.data.get(i).get(j).ts=ts;
                ts++;                 
             }
             for(int j=0;j<gk.data.get(i).size();j++){
                 if(j==0){
                     gk.data.get(i).get(j).gap=gk.data.get(i).get(j).ts;
                 }else{
                     gk.data.get(i).get(j).gap=gk.data.get(i).get(j).ts-gk.data.get(i).get(j-1).ts;
                 }
             }
         }
         
         fstream = new FileInputStream(labelfile);
         in = new DataInputStream(fstream);
         br = new BufferedReader(new InputStreamReader(in));
         gk.labels=new HashMap();
         while ((strLine = br.readLine()) != null)   {
               String[] temp;
               String delimiter = "\t";
               temp = strLine.split(delimiter);
               int id=Integer.parseInt(temp[0]);
               gk.labels.put(id, temp[1]+"+");
               gk.labels.put(id+max, temp[1]+"-");               
         }
         fstream.close();
         in.close();
        }catch (IOException e){
                System.err.println("Error: " + e.getMessage());
        }
        return gk;
    }    
}

