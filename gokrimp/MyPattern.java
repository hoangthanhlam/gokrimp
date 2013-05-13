/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gokrimp;

import java.util.ArrayList;

/**
 *
 * @author thoang
 */
public class MyPattern implements Comparable<MyPattern> {
 ArrayList<Integer> ids;
 double ben; // the compression benfit of using this pattern
 int freq; // the number of time this pattern is used
 int g_cost; // the cost of encoding the gaps  
 
 /**
  * print the set of patterns
  */
 void print(){
     System.out.print(ids);
     System.out.println(" " + ben);
 }
 
 /**
  * constructor
  */
 MyPattern(){
     ids=new ArrayList();
 }
 
 /**
  * copy constructor 
  * @param p 
  */
 MyPattern(MyPattern p){
     ben=p.ben;
     freq=p.freq;
     g_cost=p.g_cost;
     ids=new ArrayList(p.ids);
 }
 
  @Override
  /**
   * compare two patterns by benefits
   */
   public int compareTo(MyPattern o) {
        return (int)(o.ben-this.ben);
   }
}
