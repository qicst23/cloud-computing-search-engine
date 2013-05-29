/**
 * 
 */
package ranker;

import java.net.InetAddress;
import java.util.ArrayList;

import storage.ContentClient;

/**
 * @author cis455
 *
 */
public class testGetScores {
	
	protected static ContentClient contentClient = null;
	
	public static void main(String args[])
	{
		try{
		ArrayList<InetAddress> dhtGateways = new ArrayList<InetAddress>();
		for(int i=0; i<args.length;i++)
			dhtGateways.add(InetAddress.getByName(args[i]));
		contentClient = new ContentClient(dhtGateways);
		GetPRandFBScores gpfb = new GetPRandFBScores(contentClient);
		double[] scores = gpfb.getScores("www.wikipedia.com/");
		System.out.println("Scores are:"+scores[0]+" "+scores[1]);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

}
