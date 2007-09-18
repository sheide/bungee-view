package JSci.maths.statistics;

public class FisherExactTest {
	  private double sn11;
	  private double sn;
	  private double sn1_;
	  private double sn_1;
	  private double sprob;
	  private double sleft;
	  private double sright;
	  private double sless;
	  private double slarg;
	  private double left;
	  private double right;
//	  private double twotail;

	  public FisherExactTest(int a, int b, int c, int d) {
			  if(a < 0 || b < 0 || c < 0 || d < 0)
				  throw new OutOfRangeException("Parameters of FisherExactTest constructor less than 0 found; parameters must equal or greater than 0");
			  exact22(a,b,c,d);
		  }

	  public double lngamm(double z)
//	 Reference: "Lanczos, C. 'A precision approximation
//	 of the gamma function', J. SIAM Numer. Anal., B, 1, 86-96, 1964."
//	 Translation of  Alan Miller's FORTRAN-implementation
//	 See http://lib.stat.cmu.edu/apstat/245
	  {
	    double x = 0;
	    x += 0.1659470187408462e-06/(z+7);
	    x += 0.9934937113930748e-05/(z+6);
	    x -= 0.1385710331296526    /(z+5);
	    x += 12.50734324009056     /(z+4);
	    x -= 176.6150291498386     /(z+3);
	    x += 771.3234287757674     /(z+2);
	    x -= 1259.139216722289     /(z+1);
	    x += 676.5203681218835     /(z);
	    x += 0.9999999999995183;
	    return(Math.log(x)-5.58106146679532777-z+(z-0.5)*Math.log(z+6.5));
	  }

	  public double lnfact(double n)
	  {
	    if(n<=1) return(0);
	    return(lngamm(n+1));
	  }

	  public double lnbico(double n,double k)
	  {
	    return(lnfact(n)-lnfact(k)-lnfact(n-k));
	  }


	  /*

	  private double FisherExactCompute(int min,int diag,int other1,int other2){
	    double prob=0,sprob=0;
	    int i;

	    for(i=0;i<=min;i++){
	      if(i==0 || ((min-i)%10)==0){
	        sprob = hyper_323(min-i,min+other1,min+other2,min+diag+other1+other2);
	      }else{
	        sprob = sprob * (min-i+1) / (other1+i) * (diag-i+1) / (other2+i);
	      }
	      prob += sprob;
	    }

	    return prob;
	  }

	  public double FisherExact(int a,int b,int c,int d)
	  {
	    if(a<=b && a<=c && a<=d){
	      return FisherExactCompute(a,d,b,c);
	    }else if(b<=a && b<=c && b<=d){
	      return FisherExactCompute(b,c,a,d);
	    }else if(c<=a && c<=b && c<=d){
	      return FisherExactCompute(c,b,a,d);
	    }else{ // d<=a && d<=b && d<=c
	      return FisherExactCompute(d,a,b,c);
	    }
	  }
	  */

	  public double hyper_323(double n11,double n1_,double n_1,double n)
	  {
	    return(Math.exp(lnbico(n1_,n11)+lnbico(n-n1_,n_1-n11)-lnbico(n,n_1)));
	  }

	  public double hyper0(double n11i,double n1_i,double n_1i,double ni)
	  {
	    if(n1_i==0 && n_1i==0 && ni==0)
	    {
	      if(!(n11i % 10 == 0))
	      {
	        if(n11i==sn11+1)
	        {
	        	sprob *= ((sn1_-sn11)/(n11i))*((sn_1-sn11)/(n11i+sn-sn1_-sn_1));
	        	sn11 = n11i;
	        	return sprob;
	        }
	        if(n11i==sn11-1)
	        {
	        	sprob *= ((sn11)/(sn1_-n11i))*((sn11+sn-sn1_-sn_1)/(sn_1-n11i));
	        	sn11 = n11i;
	        	return sprob;
	        }
	      }
	      sn11 = n11i;
	    }
	    else
	    {
	      sn11 = n11i;
	      sn1_=n1_i;
	      sn_1=n_1i;
	      sn=ni;
	    }
	    sprob = hyper_323(sn11,sn1_,sn_1,sn);
	    return sprob;
	  }

	  public double hyper(double n11)
	  {
	    return(hyper0(n11,0,0,0));
	  }


	  public double exact(double n11, double n1_,double n_1,double n)
	  {
		double i,j,p,prob;
	    double max=n1_;
	    if(n_1<max) max=n_1;
	    double min = n1_+n_1-n;
	    if(min<0) min=0;
	    if(min==max)
	    {
	      sless = 1;
	      sright= 1;
	      sleft = 1;
	      slarg = 1;
	      return 1;
	    }
	    prob = hyper0(n11,n1_,n_1,n);
	    sleft=0;
	    p = hyper(min);
	    for(i=min+1; p<0.99999999*prob; i++)
	    {
	      sleft += p;
	      p=hyper(i);
	    }
	    i--;
	    if(p<1.00000001*prob) sleft += p;
	    else i--;
	    sright=0;
	    p=hyper(max);
	    for(j=max-1; p<0.99999999*prob; j--)
	    {
	      sright += p;
	      p=hyper(j);
	    }
	    j++;
	    if(p<1.00000001*prob) sright += p;
	    else j++;
	    if(Math.abs(i-n11)<Math.abs(j-n11))
	    {
	      sless = sleft;
	      slarg = 1 - sleft + prob;
	    }
	    else
	    {
	      sless = 1 - sright + prob;
	      slarg = sright;
	    }
	    return prob;
	  }

	  private void exact22(double n11,double n12,double n21,double n22)
	  {
	    double n1_ = n11+n12;
	    double n_1 = n11+n21;
	    double n   = n11 +n12 +n21 +n22;
	    exact(n11,n1_,n_1,n);
	    left    = sless;
	    right   = slarg;
	    double twotail = sleft + sright;
	    if(twotail>1) twotail=1;
	  }

	  public Double getLeft(){
	    return new Double(left);
	  }

	  public Double getRight(){
	    return new Double(right);
	  }

	  public Double getTwoTail(){
	    if(sleft+sright>1) return new Double(1);
	    else return new Double(sleft+sright);
	  }

	  public double getTwoTailPrimitive() {
		  return Math.min(1.0, sleft+sright);
	  }
	}
