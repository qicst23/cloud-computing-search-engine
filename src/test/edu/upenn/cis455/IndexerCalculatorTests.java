/**
 * 
 */
package test.edu.upenn.cis455;

import indexer.IndexerCalculator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import storage.ContentClient;

import junit.framework.TestCase;



/**
 * @author cis455
 *
 */
public class IndexerCalculatorTests extends TestCase{
	public void testComputeScores() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnknownHostException {
		ArrayList<InetAddress> iNets = new ArrayList<InetAddress>();
		iNets.add(InetAddress.getByName("158.130.210.124"));
		ContentClient c = new ContentClient(iNets);
		IndexerCalculator calc = new IndexerCalculator(c);
		String[] testWords = {"bbc", "new"};
		HashMap<String, Double[]> res =  calc.computeScores(testWords);
		
		System.out.println("[res size]\t" + res.size());
		for(String key: res.keySet()){
			System.out.println("key: " + key);
			System.out.print("value: ");
			Double[] val = res.get(key);
			for(int i=0; i<val.length; i++){
				System.out.print(val[i] + " ");
			}
			System.out.println();
		}
	}
}
