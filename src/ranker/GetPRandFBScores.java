/**
 * 
 */
package ranker;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import storage.ContentClient;

/** @author cis455 */
public class GetPRandFBScores {

	protected static ContentClient contentClient = null;

	public GetPRandFBScores(ContentClient c) {
		
		contentClient = c;
		
		
	}//constructor ends

	public double[] getScores(String url)
	{
		double[] scores = new double[2];
		scores = contentClient.sendGetPageRankScore(url);
		return scores;

	}
}
