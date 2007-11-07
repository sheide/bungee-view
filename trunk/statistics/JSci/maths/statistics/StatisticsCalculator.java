package JSci.maths.statistics;

import java.math.*;
import java.util.Hashtable;

/***
 * This class defines some statistical operation methods (e.g., random walk ratio, binomial distribution, etc.).
 * @author mingtai
 *
 */
public class StatisticsCalculator {
	public final static int ResultScale = 10;  // # of digits after point
	public final static int InitialProbabilityScale = 5;  // initial scale for p (p = n/N for binomial distribution)
    public final static int RoundMode = BigDecimal.ROUND_DOWN;
    public final static double NAT_BASE = 2.71828183d;
    private static OrderTable OTable = null;  // OrderTable

    /***
     * natural log of gamma function with parameters z
     * @param z
     * @return natural log gamma function with parameter z
     */
    public static double lngamm(int z)
//  Reference: "Lanczos, C. 'A precision approximation
//  of the gamma function', J. SIAM Numer. Anal., B, 1, 86-96, 1964."
//  Translation of  Alan Miller's FORTRAN-implementation
//  See http://lib.stat.cmu.edu/apstat/245
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

    /***
     * Calculate natural log of factorial n
     * @param n order(!)
     * @return natural log of factorial n
     */

   public static double lnfact(int n)
   {
     if(n<=1) return(0);
     return(lngamm(n+1));
   }

   /***
    * natural log value of combinational C(n,k)
    */

   public static double lnbico(int n,int k)
   {
     return(lnfact(n)-lnfact(k)-lnfact(n-k));
   }



    /***
     * Set up maximum order's value which you may use of. (For example, if you use only up to of 1000!, pass 1000 as the parameter)
     * @param MaximumOrder maximum order's value
     * @deprecated
     */
    public static void setUpOrderTable(int MaximumOrder) {
    	OTable = new OrderTable(MaximumOrder);
    }

    /***
     * Get Combination C(x,y)
     * @deprecated
     */
	public static BigInteger getFactorial(int x, int y) throws Exception{
		if(x < y)
			throw new Exception("x " + x +" is less than y " + y );
		//	 reduce fraction
		if(y == 0)
			return new BigInteger("1");
		if(y == 1)
			return new BigInteger(new Integer(x).toString());

		BigInteger up = OTable.getOrderValue(new Integer(x));
		BigInteger down = OTable.getOrderValue(new Integer(y)).multiply( OTable.getOrderValue(new Integer(x-y)) );
		return up.divide(down);
	}

	/***
	 * Binomial distribution formula
	 * @param numbers: number of success (x)
	 * @param trials: number of trials (X)
	 * @param probability: n/N
	 * @return binomial distribution value
	 * @throws Exception OTable null exception
	 * @deprecated
	 */
//	public  static BigDecimal BinoMdist(int numbers, int trials, BigDecimal probability) throws Exception{
//		if(OTable == null)
//			throw new Exception("Must set up OrderTable before calculating binomial distribution");
//		BigDecimal sum = new BigDecimal(0);
//		BigDecimal fact;
//		BigDecimal pi;
//		BigDecimal qx_i;
//		BigDecimal q = new BigDecimal(1).subtract(probability);
//
//		if(numbers < (trials)/2 )
//			for(int i = 0; i <= numbers-1; i++) {
//				fact = new BigDecimal(getFactorial(trials,i) );
//				pi = probability;//.pow(i);
//				qx_i = q;//.pow(trials-i );
//				sum = sum.add( fact.multiply(pi).multiply(qx_i) );
//			}
//		else
//		{
//			//for(int i = numbers+1; i <=X; i++)  // wrong
//			for(int i = numbers; i <=trials; i++)
//			{
//				fact = new BigDecimal(getFactorial(trials,i) );
//				pi = probability;//.pow(i);
//				qx_i = q;//.pow(trials-i );
//				sum = sum.add( fact.multiply(pi).multiply(qx_i) );
//			}
//			sum = new BigDecimal("1").subtract(sum);
//		}
//
//		return new BigDecimal(1d).subtract( sum).setScale(ResultScale,RoundMode);
//		//return new BigDecimal(1d).subtract( sum);
//	}

	/***
	 * Faster binomial distribution calculation using natural log gamma function.
	 * @param numbers number of success (e.g., annotated count of a GO term in test set)
	 * @param trials number of trials (e.g., number of genes having at least one GO annotation in test set)
	 * @param probability n/N (n: number of annotated count of a GO term in reference set;
	 * 		                    N: number of genes having at least one GO annotaiton in reference set)
	 * @return binomial distribution value
	 */
	public static Double FastBinoMdist(int numbers,int trials, double probability) {
		double sum = 0;
		double fact;
		double pi;
		double qx_i;
		double q = 1d - probability;

		if(numbers < (trials)/2 )
		{
			for(int i = 0; i <= numbers-1; i++) {
				fact = lnbico(trials,i);
				pi = Math.log(probability)*i;
				qx_i = Math.log(q)*(trials -i);
				sum = sum + Math.exp( fact + pi + qx_i);
			}
			if(sum > 1d)  // java's double precision problem may cause sum to be slightly greater than 1
				sum = 1d;
		}
		else
		{
			//for(int i = numbers+1; i <=X; i++)  // wrong
			for(int i = numbers; i <=trials; i++)
			{
				fact = lnbico(trials,i);
				pi = Math.log(probability)*i;
				qx_i = Math.log(q)*(trials -i);
				//sum = sum.add( fact.multiply(pi).multiply(qx_i) );
				sum = sum +Math.exp( fact + pi + qx_i);
			}
			if(sum > 1d)	// just java's double precision problem
				sum = 1d;
			sum = 1d- sum;
		}

		return new Double(1d- sum);
	}

	/***
	 * Calculate the sum of all Chi-square values of a 2 x 2 contingency table
	 * Note if any of the expected values is less than 5, return -1, and use Fisher Exact Test instead of Chi-Square.
	 * @param TestSetSize number of genes in test set (X)
	 * @param RefSetSize number of genes in reference set (N)
	 * @param TestCnt count of a GO term annotated by test set (x)
	 * @param RefCnt count of a GO term annotated by reference set (n)
	 * @return sum of chi-square values of the table
	 */
	public static Double Twoby2ChiSqrSum(double TestSetSize, double RefSetSize,double TestCnt,  double RefCnt) {
		double total = TestSetSize + RefSetSize;
		double sum_row1 = TestCnt + RefCnt;
		double sum_row2 = total - sum_row1;
		double exp_test1 = (TestSetSize * sum_row1)/total;  // left up column's exp
		double exp_test2 = (TestSetSize * sum_row2)/total;  // left down column's exp
		double exp_ref1 = (RefSetSize * sum_row1)/total;  // right up column's exp
		double exp_ref2 = (RefSetSize * sum_row2)/total;	 // right down column's exp
		if(exp_test1 < 5 || exp_test2 < 5 || exp_ref1 < 5 || exp_ref2 < 5)	// if any of the expected values less than 5, use Fisher's Exact test
		{
			return new Double(-1);
		}
		else {
			double chi_test1 =  Math.pow(TestCnt - exp_test1,2)/exp_test1;
			double chi_test2 =  Math.pow( (TestSetSize-TestCnt) - exp_test2,2)/exp_test2;
			double chi_ref1 =  Math.pow(RefCnt - exp_ref1,2)/exp_ref1;
			double chi_ref2 =  Math.pow( (RefSetSize-RefCnt) - exp_ref2,2)/exp_ref2;
			return new Double(chi_test1 + chi_test2 + chi_ref1 + chi_ref2);
		}
	}

	/***
	 * return random walk ratio
	 * @param upCnt count of up-regulating genes
	 * @param downCnt count of down-regulating genes
	 * @return random walk ratio
	 */
	public static Double RandomWalkRatio(int upCnt, int downCnt) {
		return new Double( (upCnt - downCnt)/ Math.pow( (upCnt + downCnt), 0.5) );
	}

	/***
	 * return random walk probability
	 * @param upCnt count of up-regulating genes
	 * @param downCnt count of down-regulating genes
	 * @return random walk probability
	 */
	public static Double RandomWalkProbability(int upCnt, int downCnt) {
		return new Double( (upCnt - downCnt)/ Math.pow( (upCnt + downCnt), 0.5) );
	}

}

/***
 *
 * OrderTable class is merely used to store each value of order (!).
 * @deprecated
 */
class OrderTable
{
	private Hashtable table;

	public OrderTable(int x) {
		table = new Hashtable();
		BigInteger sum = new BigInteger("1");
		int a;
		for(a = 0; a <= x; a++)
		{
			if(a == 0 || a == 1)
				table.put(new Integer(a), new BigInteger("1"));
			else
			{
				Integer order = new Integer(a);
				sum = sum.multiply(new BigInteger(order.toString() ) );
				table.put(order, sum);
			}
		}
		System.gc();
	}
	public BigInteger getOrderValue(Integer t) throws Exception {

		BigInteger a = (BigInteger)table.get(t);
		if(a == null)
			throw new Exception("Cannot find order value of " + t.intValue());
		return a;
	}
}