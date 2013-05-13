/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gokrimp;


/**
 *
 * @author thoang
 */
public class Experiments {
     
    
    
    static void experiment_with_gokrimp(String dataname){
       
        DataReader d=new DataReader();
        GoKrimp g=d.readData(dataname);
        g.gokrimp();
       
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
       long startTime = System.currentTimeMillis();
       experiment_with_gokrimp(args[0]);
       long endTime   = System.currentTimeMillis();
       long totalTime = endTime - startTime;
       System.out.println("Running time: "+totalTime/1000+" seconds");
    }
}
