/**
 * 
 */
package test.edu.upenn.cis455;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

/**
 * @author cis455
 *
 */
public class RankerTests extends TestCase {

	String combinedInputFile = "/home/cis455/Desktop/hadoop-1.0.4/wordcount/output/part-r-00000";

	public void testInput()
	{
		try{
			File fd = new File(combinedInputFile);
			FileInputStream fs1 = new FileInputStream(fd);
			DataInputStream din1 = new DataInputStream(fs1);
			BufferedReader br1 = new BufferedReader(new InputStreamReader(din1));
			String line1;
			double d = 1.000;
			String s = " ";
			int i = 3;
			while ((line1 = br1.readLine()) != null)
			{
				String[] l = line1.split("\t");
				if(l.length!=3)
				{
					System.out.println("length:"+l.length+" line:"+line1);
					//System.out.println("One:"+l[0]);
					//System.out.println("Two:"+l[1]);
					
				}
				if(l.length==5)
				{	
					System.out.println("l[0]:"+l[0]);
					System.out.println("l[1]:"+l[1]);
					System.out.println("l[2]:"+l[2]);
					System.out.println("l[3]:"+l[3]);
					System.out.println("l[4]:"+l[4]);
					
				}
				assertEquals(i, l.length);
				assertNotNull(l[0]);
				assertNotSame(s, l[0]);
				//assertEquals(d , Double.parseDouble(l[1]));
				assertNotNull(l[2]);
			}
			br1.close();
			din1.close();
			fs1.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}
