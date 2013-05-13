/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gokrimp;


import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * implementation of GoKrimp and SedKrimp algorithms
 * GoKrimp: direct look for compressing sequential patterns from a database of sequences
 * SeqKrimp: read a set of candidate patterns and find a good subset of compressing sequential patterns
 * For more information please refer to the paper Mining Compressing Sequential Patterns in the Journal Statistical Analysis and Data Mining
 * @author thoang
 */
class GoKrimp {
    ArrayList<Integer> characters; //map from characters to its indices in the dictionary
    ArrayList<ArrayList<Event>> data;  // a database of sequences
    ArrayList<MyPattern> patterns; // the set of patterns, the dictionary in this implementation
    ArrayList<MyPattern> candidates; // the set of candidates
    HashMap<Integer,String> labels; // event labels
    HashMap<Integer,ArrayList<Integer>> related_events; // map from events to related events
    ArrayList<Integer> classlabels; // class labels of each sequence 
    
    int Nword; // the number of encoded words   
    double comp_size; // size (in bits) of the compressed data 
    double uncomp_size; // the size (in bits) of the uncompressed data (representation by the Huffman codes)
    static final int NSTART=1000; // the maximum number of candidate events as starting points for extending to find compressing patterns
   
    /**
     *  find compression patterns by greedily extending initial candidate events 
     */ 
    void gokrimp(){
        initialization();
        ArrayList<MyPattern> ie=get_Initial_Patterns(); // get a set of initial event
        MyPattern maxp=new MyPattern(); double max;
        while(true){
            max=Double.NEGATIVE_INFINITY;
            for(int i=0;i<ie.size();i++){
                MyPattern mp=ie.get(i),prev=mp;
                while((mp=extend(mp))!=null){
                   prev=mp;
                }
                if(prev.ben>max){
                    maxp=prev;
                    max=prev.ben;
                }
            }
            if(max<=0)
                break;
            else{
                addPattern(maxp);
                printMyPattern(maxp);
                remove(maxp);                 
                //printData();
                //printPattern();
            }            
        }
        System.out.println("Compressed size: "+comp_size+", uncompressed size: "+uncomp_size+", compression ratio: "+uncomp_size/(0.0+comp_size));
    }
    
    /**
     * candidate based algorithm, search for the best encoding of the data given the set of candidates
     */
    void seqkrimp(){
        
        MyPattern maxp=new MyPattern(); double max;
        while(true){
            max=Double.NEGATIVE_INFINITY;
            int mi=getBestPattern();
            if(candidates.get(mi).ben>max){
                    maxp=new MyPattern(candidates.get(mi));
                    max=candidates.get(mi).ben;
            }
            if(max<=0)
                break;
            else{
                addPattern(maxp);
                printMyPattern(maxp);
                remove(maxp);   
                //printData();
                //printPattern();
            } 
            // remove the best candidate from the canidate lists 
            candidates.remove(mi);  
            for(int i=0;i<candidates.size();i++){
                candidates.get(i).ben=0;
                candidates.get(i).freq=0;
                candidates.get(i).g_cost=0;             
            }
        }
        System.out.println("Compressed size: "+comp_size+", uncompressed size: "+uncomp_size+", compression ratio: "+uncomp_size/(0.0+comp_size));
    }
    
    /**
     * Initialization
     */
    void initialization(){
        patterns=new ArrayList();
        candidates=new ArrayList();
        related_events=new HashMap();
        Nword=0;
        comp_size=0;
        characters=new ArrayList(); // temporarilly, the characters map contains event id and the number of time the corresponding event occurs in the database
        for(int i=0;i<data.size();i++){
            for(int j=0;j<data.get(i).size();j++){
                if(data.get(i).get(j).id>=characters.size()){
                    for(int ii=characters.size();ii<data.get(i).get(j).id+1;ii++)
                        characters.add(new Integer(0));
                }
                characters.set(data.get(i).get(j).id,characters.get(data.get(i).get(j).id)+1);                
            }
            Nword+=data.get(i).size();
        }
        Nword+=2*characters.size();        
        for(int i=0;i<characters.size();i++){
           MyPattern mp=new MyPattern();
           mp.ids.add(i);
           mp.ben=0;
           mp.freq=characters.get(i)+2; //plus 2 because counting also two occurence of the singleton in the dictionary
           mp.g_cost=0;
           patterns.add(mp); //add the given singleton to the dictionary
           comp_size+=mp.freq*Math.log(Nword)/Math.log(2)- mp.freq*Math.log(mp.freq)/Math.log(2);  
           characters.set(i, patterns.size()-1); // the characters map now contains event id and its index in the dictionary
        }
        uncomp_size=comp_size;
        //remove occurences of rare events in the data, rare events are the ones having frequency less than SignTest.N (25 by default)
        for(int i=0;i<data.size();i++){
            for (Iterator<Event> it = data.get(i).iterator(); it.hasNext(); ) {
                 if(patterns.get(characters.get(it.next().id)).freq<SignTest.N){
                         it.remove();
                 }   
            }
        }
        /*System.out.println(comp_size);
        for(int i=0;i<patterns.size();i++){
            System.out.println(patterns.get(i).ids +" "+patterns.get(i).ben);
        }  */             
    }
    
    /**
     * add a new pattern to the dictionary
     * @param pattern 
     */
    void addPattern(MyPattern pattern){
        Nword=Nword-(pattern.freq-1)*pattern.ids.size()+pattern.ids.size()+pattern.freq; //update the number of encoded words
        comp_size-=pattern.ben; //update the compression size
        HashMap<Integer,Integer> hm=new HashMap(); // hm contains event id and the number of time the event occurs in the pattern.ids
        for(int j=0;j<pattern.ids.size();j++){
                if(!hm.containsKey(pattern.ids.get(j))){
                    hm.put(pattern.ids.get(j), 1);
                } else{
                    hm.put(pattern.ids.get(j), hm.get(pattern.ids.get(j))+1);
                }
        }
        for(int i=0;i<patterns.size();i++){ //update the frequency of the existing patterns 
            if(patterns.get(i).ids.size()==1&&hm.containsKey(patterns.get(i).ids.get(0))){//singleton among the events of the pattern
                patterns.get(i).freq-=(pattern.freq-1)*hm.get(patterns.get(i).ids.get(0));
                patterns.get(i).freq+=hm.get(patterns.get(i).ids.get(0)); 
            }            
        }
        patterns.add(pattern); // add the new pattern to the dictionary
    }    
    
   /**
    * extend the current pattern
    * @param pattern the pattern to be extended
    * @return null if no extension gives additional compression benefit or the extended pattern if otherwise
    */
    MyPattern extend(MyPattern pattern){
       //ArrayList<Integer> ve=get_Extending_Events(); // get the set of extending events
       ArrayList<Integer> ve=get_Extending_Events_SignTest(pattern.ids.get(pattern.ids.size()-1)); // get the set of extending events
      // System.out.println(pattern.ids);
       //System.out.println(ve);
       candidates.clear();
       //append the set of extending event to the pattern to create new candidates
       for(int i=0;i<ve.size();i++){
           MyPattern can=new MyPattern();//create a new candidate
           can.g_cost=0;
           can.freq=0;
           can.ben=0;
           can.ids=new ArrayList(pattern.ids);
           can.ids.add(ve.get(i));
           candidates.add(can);
       }
       if(candidates.isEmpty())
           return null;
       int best=getBestPattern(); //get the index of the best candidate
       if(candidates.get(best).ben>pattern.ben)
            return candidates.get(best);
       else 
           return null;
    }
    
    /**
     * get the set of initial patterns
     * @return return a set of initial patterns
     */
    ArrayList<MyPattern> get_Initial_Patterns(){
        ArrayList<MyPattern> ie=new ArrayList();
        for(int i=0;i<patterns.size();i++){
            if(patterns.get(i).freq>=SignTest.N){ //only consider unrare events
                 ie.add(new MyPattern(patterns.get(i)));
            }
        }
        for(int i=0;i<ie.size();i++){
            ie.get(i).ben=ie.get(i).freq;
        }
        Collections.sort(ie);
        while(ie.size()>NSTART){
            ie.remove(ie.size()-1);
        }
         for(int i=0;i<ie.size();i++){
            ie.get(i).ben=0;
        }
        /*for(int i=0;i<patterns.size();i++)
            System.out.println("a "+patterns.get(i).ben+" "+patterns.get(i).ids+ie.get(i).ids);*/
        return ie;
    }
    
   
    /**
     * get the set of events being considered to extend a pattern
     * @return the set of events being considered to extend a pattern
     */
    ArrayList<Integer> get_Extending_Events(){
        ArrayList<Integer> ve=new ArrayList();
        for(int i=0;i<patterns.size();i++){
            if(patterns.get(i).ids.size()==1)
                ve.add(patterns.get(i).ids.get(0));
        }
        return ve;
    }
    
     /**
     * get the set of events being considered to extend a pattern, Signed Test is used to select such events
     * @return the set of events being considered to extend a pattern
     */
    ArrayList<Integer> get_Extending_Events_SignTest(Integer e){
       if(related_events.containsKey(e))
           return related_events.get(e);
       ArrayList<Integer> ve=getRelatedEvents(e);
       related_events.put(e, ve);
       return ve;
    }
   
    /**
     * get the best pattern among the set of candidates
     * @return index of the best pattern in the candidates ArrayList
     */
    int getBestPattern(){
       int index=0; double min=Double.POSITIVE_INFINITY;
       for(int i=0;i<candidates.size();i++){//for every candidate
           //get all the best matches of the candidate in every sequence
           for(int j=0;j<data.size();j++){
               HashMap<Integer,ArrayList<Integer>> hm=new HashMap();
               ArrayList<ArrayList<Integer>> pos=new ArrayList();
               for(int k=0;k<candidates.get(i).ids.size();k++){
                 if(!hm.containsKey(candidates.get(i).ids.get(k))){
                     ArrayList<Integer> a=new ArrayList();
                     a.add(k);
                     hm.put(candidates.get(i).ids.get(k),a);
                 } else{
                      ArrayList<Integer> a=hm.get(candidates.get(i).ids.get(k));
                      a.add(k);
                      hm.put(candidates.get(i).ids.get(k),a);
                 }
                 pos.add(new ArrayList());
               }
               for(int k=0;k<data.get(j).size();k++){
                   if(hm.containsKey(data.get(j).get(k).id)){
                       for(int l=0;l<hm.get(data.get(j).get(k).id).size();l++)
                        pos.get(hm.get(data.get(j).get(k).id).get(l)).add(data.get(j).get(k).ts);
                   }
               }
               ArrayList<ArrayList<Integer>> matches=getBestMatches(pos);
               candidates.get(i).freq+=matches.size();
               candidates.get(i).g_cost+=gap_cost(matches);
           }
           if(candidates.get(i).freq==0) //skip the candidate that has no occurrence in the data
               continue;
           candidates.get(i).freq+=1;//plus one because we also count its occurence in the dictionary
           double com= get_Compress_Size_When_Adding(candidates.get(i));
           if(com<min){
               min=com;
               index=i;
           }           
       }
       candidates.get(index).ben=comp_size-min;
       return index;
    }    
    
    /**
     * remove all the best matches of the pattern in the data
     * @param pattern 
     */
    void remove(MyPattern pattern){
        for(int j=0;j<data.size();j++){
               HashMap<Integer,ArrayList<Integer>> hm=new HashMap();
               ArrayList<ArrayList<Integer>> pos=new ArrayList();
               for(int k=0;k<pattern.ids.size();k++){
                 if(!hm.containsKey(pattern.ids.get(k))){
                     ArrayList<Integer> a=new ArrayList();
                     a.add(k);
                     hm.put(pattern.ids.get(k),a);
                 } else{
                      ArrayList<Integer> a=hm.get(pattern.ids.get(k));
                      a.add(k);
                      hm.put(pattern.ids.get(k),a);
                 }
                 pos.add(new ArrayList());
               }
               for(int k=0;k<data.get(j).size();k++){
                   if(hm.containsKey(data.get(j).get(k).id)){
                       for(int l=0;l<hm.get(data.get(j).get(k).id).size();l++)
                         pos.get(hm.get(data.get(j).get(k).id).get(l)).add(data.get(j).get(k).ts);
                   }
               }
       
               ArrayList<ArrayList<Integer>> matches=getBestMatches(pos);    
               remove(matches,j);
         }
    }
    /**
     * get the compress size of the data when the given @param pattern is added to the current dictionary
     * @param pattern
     * @return 
     */
    double get_Compress_Size_When_Adding(MyPattern pattern){
       // System.out.println(pattern.ids);
        int new_Nword=Nword-(pattern.freq-1)*pattern.ids.size()+pattern.ids.size()+pattern.freq;
        double com=comp_size; //
        com+=new_Nword*Math.log(new_Nword)/Math.log(2)-Nword*Math.log(Nword)/Math.log(2)-pattern.freq*Math.log(pattern.freq)/Math.log(2);
        HashMap<Integer,Integer> hm=new HashMap(); // hm contains event id and the number of time the event occurs in the pattern.ids
        for(int i=0;i<pattern.ids.size();i++){
            if(!hm.containsKey(pattern.ids.get(i))){
                hm.put(pattern.ids.get(i), 1);
            } else{
                hm.put(pattern.ids.get(i), hm.get(pattern.ids.get(i))+1);
            }
        }
        for(Integer key:hm.keySet()){
            int new_freq=patterns.get(characters.get(key)).freq-hm.get(key)*pattern.freq+2*hm.get(key);
            com-=new_freq*Math.log(new_freq)/Math.log(2)-patterns.get(characters.get(key)).freq*Math.log(patterns.get(characters.get(key)).freq)/Math.log(2);
        }
        com+=pattern.g_cost;
        return com;
    }
    
    /**
     * return the best matches of a pattern with positions stored in the @param pos
     * @param pos
     * @return 
     */
    ArrayList<ArrayList<Integer>> getBestMatches(ArrayList<ArrayList<Integer>> pos){
        ArrayList<ArrayList<Integer>> matches=new ArrayList();
        while(true){
            ArrayList<ArrayList<Event> > matrix=new ArrayList();
            for(int i=0;i<pos.size();i++){
                matrix.add(new ArrayList());                
            }
            for(int i=0;i<pos.size();i++){
            	if(i==0){
			for(int j=0;j<pos.get(0).size();j++){
				Event ww=new Event();
				ww.ts=0;
				ww.id=pos.get(0).get(j);
				ww.gap=0;
				matrix.get(0).add(ww);
			}
		} else {
			for(int j=0;j<pos.get(i).size();j++){
                                int index=0,min=Integer.MAX_VALUE,mini=0;
				while(index<matrix.get(i-1).size()&&matrix.get(i-1).get(index).id<pos.get(i).get(j)){
					if(matrix.get(i-1).get(index).ts==Integer.MAX_VALUE){
						index++;
						continue;
					}
					int g=matrix.get(i-1).get(index).ts+bits(pos.get(i).get(j)-matrix.get(i-1).get(index).id);
					if(g<=min){
						min=g;
						mini=index;
					}
					index++;
					
				}
				Event ww=new Event();
				ww.ts=min;
                                ww.id=pos.get(i).get(j);
				ww.gap=mini;
				matrix.get(i).add(ww);				
			}
		}
		
            }
	
            int min=Integer.MAX_VALUE,mini=0;
            for(int i=0;i<matrix.get(matrix.size()-1).size();i++){
		if(min>matrix.get(matrix.size()-1).get(i).ts){
			min=matrix.get(matrix.size()-1).get(i).ts;
			mini=i;
		}
            }
            
            //for(int i=0;i<matrix.size();i++){
		//for(int j=0;j<matrix.get(i).size();j++){
		//	System.out.print(matrix.get(i).get(j).ts +" "+matrix.get(i).get(j).id+" "+matrix.get(i).get(j).gap+" ");
		//}			
                //System.out.println();
            //}
            
            if(min==Integer.MAX_VALUE)
                   break;
            ArrayList<Integer> match=new ArrayList();
            //trace back to get the best match
            HashMap<Integer,Integer> hm=new HashMap();
            for(int i=matrix.size()-1;i>=0;i--){    
		match.add(0,matrix.get(i).get(mini).id);
                hm.put(matrix.get(i).get(mini).id, 1); 
                mini=matrix.get(i).get(mini).gap;                                
            }
             matches.add(match);
             for(int i=0;i<pos.size();i++){
                for (Iterator<Integer> it = pos.get(i).iterator(); it.hasNext(); ) {
                            if(hm.containsKey(it.next())){
                                it.remove();
                            }
                }  
                if(pos.get(i).isEmpty())
                     return matches;
             }
        }
        return matches;
    }
    
    /**
     * 
     * @param matches
     * @return the cost of encoding the gaps of the matches
     */
    int gap_cost( ArrayList<ArrayList<Integer>> matches){
      int g=0;
      for(int i=0;i<matches.size();i++){
          for(int j=1;j<matches.get(i).size();j++)
              g+=bits(matches.get(i).get(j)-matches.get(i).get(j-1));
      }
      return g;
    }
    
    /**
     * remove all the matches in the sequence with identifier index
     * @param matches
     * @param index the identifier of the sequence 
     */
    void remove(ArrayList<ArrayList<Integer>> matches, int index){
      HashMap<Integer,Integer> hm=new HashMap();
      for(int i=0;i<matches.size();i++){
          for(int j=0;j<matches.get(i).size();j++){
             hm.put(matches.get(i).get(j), 1); 
          }
      }
     
      for (Iterator<Event> it = data.get(index).iterator(); it.hasNext(); ) {
            if(hm.containsKey(it.next().ts)){
                it.remove();
            }
      }   
    }
    
                 
    /**
     *  get related event of the event @param e
     * @param e 
     * @return  the set of related events
     */
    ArrayList<Integer> getRelatedEvents(Integer e){
        HashMap<Integer,SignTest> me=new HashMap();//statistics
        HashMap<Integer,Integer> mc=new HashMap();//counter
        ArrayList<Integer> nextdata=new ArrayList();
        for(int i=0;i<data.size();i++){
            int next=data.get(i).size();
            for(int j=0;j<data.get(i).size();j++){
                if(data.get(i).get(j).id==e.intValue()){
                    next=j;
                    break;
                }
            }
            next++;
            nextdata.add(next);
        }
        for(int i=0;i<data.size();i++){
            mc.clear();
            if(nextdata.get(i)>=data.get(i).size())
                continue;
            double middle = data.get(i).get(nextdata.get(i)).ts;
             middle= middle+(data.get(i).get(data.get(i).size()-1).ts-middle)/2;
            for(int j=nextdata.get(i).intValue();j<data.get(i).size();j++){
                if(data.get(i).get(j).ts<=middle){ // in the first half
                    if(!mc.containsKey(data.get(i).get(j).id)){ // the event has been seen for the first time
                       mc.put(data.get(i).get(j).id, new Integer (1));
                    } else { // the event has been already seen before
                       mc.put(data.get(i).get(j).id, new Integer(mc.get(data.get(i).get(j).id)+1)); 
                    }
                } else { // in the second half
                     if(!mc.containsKey(data.get(i).get(j).id)){ // the event has been seen for the first time
                       mc.put(data.get(i).get(j).id, new Integer (-1));
                    } else { // the event has been already seen before
                       mc.put(data.get(i).get(j).id, new Integer(mc.get(data.get(i).get(j).id)-1)); 
                    }
                }
                
            }
            for(Integer key:mc.keySet()){
                if(!me.containsKey(key)){ // see for the first time
                    SignTest st=new SignTest(1,0);
                    if(mc.get(key).intValue()>0)
                        st.Nplus=st.Nplus+1;
                    me.put(key, st);
                } else{ // have been already seen
                    SignTest st;
                    if(mc.get(key).intValue()!=0)
                        st=new SignTest(me.get(key).Npairs+1,me.get(key).Nplus);
                    else
                        st=new SignTest(me.get(key).Npairs,me.get(key).Nplus);
                    if(mc.get(key).intValue()>0)
                        st.Nplus=st.Nplus+1;
                    me.put(key, st);
                }
            }
        }
        ArrayList<Integer> results =new ArrayList();
        for(Integer key:me.keySet()){
            
           if(me.get(key).sign_test()){//pass the sign-test
                results.add(key);
                //System.out.println(e+" "+key+" "+me.get(key).Npairs+" "+me.get(key).Nplus);
           }
        }      
        return results;
    }
  
    /**
     * 
     * @param a
     * @return the number of bits in the binary representation of an integer a using the Elias code
     */
    int bits(Integer a){
        if(a.intValue()<0)
            return 0;
        else{
            double x=Math.log(a)/Math.log(2);
            //return lowround(x)+2*lowround(Math.log(lowround(x)+1)/Math.log(2))+1; //ellias delta
            return lowround(x);
            //return 2*lowround(x)+1; //elias gamma code
        }
    }
    
   /**
    * 
    * @param x
    * @return the lower round value of x 
    */
    int lowround(double x){
       int y=(int) Math.round(x);
       if(y>x)
           y=y-1;
       return y;
    }
    
    /**
     * check if pattern p is occurred in the sequence index 
     * @param index
     * @return 
     */
    boolean isOccurred(MyPattern p, int index){
        int d=0;
      
        for(int i=0;i<data.get(index).size()&&d<p.ids.size();i++){
            if(p.ids.get(d)==data.get(index).get(i).id){
                d++;                
            }
        }
        if(d==p.ids.size())
            return true;
        else
            return false;
    }
    
    /**
     * print all patterns having positive compression benefit 
     */
    void printPattern(){
        Collections.sort(patterns); //sort patterns decreasingly by the compresion benefit
        int np=0;
        for(int i=0;i<patterns.size();i++){
           if(patterns.get(i).ben<=0)
               break;
           np++;
           if(labels.isEmpty()){
               System.out.print("[ ");
               for(int j=0;j<patterns.get(i).ids.size();j++)
                   System.out.print(patterns.get(i).ids.get(j)+" ");
               System.out.println("] "+patterns.get(i).g_cost+" "+patterns.get(i).freq+" "+patterns.get(i).ben);
           } else {
               System.out.print("[ "); 
               for(int j=0;j<patterns.get(i).ids.size();j++)
                   System.out.print(labels.get(patterns.get(i).ids.get(j))+" ");
               System.out.println("] "+patterns.get(i).g_cost+" "+patterns.get(i).freq+" "+patterns.get(i).ben);
           }          
        }
        System.out.println("# encoded words: "+Nword+ ", # patterns: "+np);
    }
    
    /**
     * print the sequence database
     */
    void printData(){
       System.out.println("o--------------------------------o");
       for(int i=0;i<data.size();i++){
               for(int j=0;j<data.get(i).size();j++)
                   System.out.print("("+data.get(i).get(j).id+","+data.get(i).get(j).ts +") ");
                System.out.println();
       }
       System.out.println("o--------------------------------o");
   }
   
    /**
     * print a pattern
     * @param pattern 
     */
   void printMyPattern(MyPattern pattern) {
        if(labels==null||labels.isEmpty()){
               System.out.print("[ ");
               for(int j=0;j<pattern.ids.size();j++)
                   System.out.print(pattern.ids.get(j)+" ");
               System.out.println("] "+pattern.ben);
        } else {
               System.out.print("[ "); 
               for(int j=0;j<pattern.ids.size();j++)
                   System.out.print(labels.get(pattern.ids.get(j))+" ");
               System.out.println("] "+pattern.ben);
        }     
   }
   
   /**
    * print binary representation of the data in Weka type
    * @param dataname 
    */
   void PrintPatternWeka(String dataname){
         try{
            int nlab=0;
            for(int i=0;i<classlabels.size();i++){
                if(nlab<classlabels.get(i))
                    nlab=classlabels.get(i);
            }
            // Create file 
            FileWriter fstream = new FileWriter(dataname+".arff");
            PrintWriter out = new PrintWriter(fstream);
                out.println("@RELATION "+dataname);
                for(int i=0;i<patterns.size();i++){
                    if(patterns.get(i).ids.size()==1)
                        out.println("@ATTRIBUTE s"+ i+  " NUMERIC");
                    else
                        out.println("@ATTRIBUTE m"+ i+  " NUMERIC");
                }
                String s="@ATTRIBUTE class        {";
                for(int i=0;i<nlab+1;i++){
                    if(i==0)
                        s+="0";
                    else
                        s+=","+i;
                }
                s+="}";
                out.println(s);
                out.println("@DATA");
                for(int i=0;i<data.size();i++){
                    out.print("{");
                    for(int j=0;j<patterns.size();j++){
                        if(isOccurred(patterns.get(j),i))
                           out.print(j+" 1,");                        
                    }
                    out.println(patterns.size()+" "+classlabels.get(i)+"}");
                }
                out.close();
            }catch (Exception e){//Catch exception if any
                System.err.println("Error: " + e.getMessage());
            }
     }
   
    
    
    /**
     * copy data
     */
    ArrayList<ArrayList<Event>> copy(){
        ArrayList<ArrayList<Event>> cd=new ArrayList();
        for(int i=0;i<data.size();i++){
            ArrayList<Event> s=new ArrayList();
            for(int j=0;j<data.get(i).size();j++){
                Event e=new Event();
                e.gap=data.get(i).get(j).gap;
                e.id=data.get(i).get(j).id;
                e.ts=data.get(i).get(j).ts;
                s.add(e);
            }
            cd.add(s);
        }
        return cd;
    }
}
