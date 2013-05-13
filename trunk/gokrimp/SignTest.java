/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gokrimp;

/**
 * SignTest class implements the standard Sign Test to compare two populations and test if they are sampled from the distributions with the same mean value 
 * @author thoang
 */
public class SignTest {
    static final double alpha=0.01; //significant level
    static final int N=25; // the number of pairs must be greater than N to perform sign test
    int Npairs; // the number of pairs (X,Y)
    double Nplus; // the number of pairs (X,Y) such that X>Y
    /**
     * the cdf function of the Standard Normal distribution 
     * @param xx
     * @return 
     */
    double standard_normal_cdf(double xx){
	double x=xx;
	if (xx<0)
		x=-x;		
	double b0=0.2316419, b1=0.319381530, b2=-0.356563782, b3=1.781477937, b4=-1.821255978, b5=1.330274429;
	double t=1/(1+b0*x);
	double pi=4.0*Math.atan(1.0);
	double pdf= 1/Math.sqrt(2*pi)*Math.exp(-0.5*x*x); //standard normal distribution's pdf
	if (xx>0)	
		return 1-pdf*(b1*t+b2*t*t+b3*t*t*t+b4*t*t*t*t+b5*t*t*t*t*t);
	else
		return pdf*(b1*t+b2*t*t+b3*t*t*t+b4*t*t*t*t+b5*t*t*t*t*t);
    }

    /*
     *  return true if it passes the test
     */
    boolean sign_test(){
        if(Npairs<N){ // the number of pairs must be at least N to perform the test
           return false;
        }else {
            double x=Math.abs(Nplus -0.5*Npairs)/Math.sqrt(Npairs+0.0);
            if(1-standard_normal_cdf(x)<alpha){
                return true;
            }else
                return false;
        }
    }   
    
    SignTest(int np, double npp){
        Npairs=np;
        Nplus=npp;
    }
}
