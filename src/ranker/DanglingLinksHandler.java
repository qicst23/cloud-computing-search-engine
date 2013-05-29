/**
 * 
 */
package ranker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author cis455
 *
 */
public class DanglingLinksHandler {

	static HashSet<String> h1 = new HashSet<String>();
	static HashSet<String> h2 = new HashSet<String>();
	static HashMap<String,ArrayList<String>> hm = new HashMap<String,ArrayList<String>>();
	static HashMap<String,String> distincturls = new HashMap<String,String>();


	//public void handleDanglingLinks(String dir)
	public static void main(String[] args)
	{
		try{
			if (args.length != 1) {
				System.err.println("Usage: danglinglinkshandler <input folder>");
				System.exit(2);
			}
			String dir = args[0];
			File f = new File(dir);
			File lf[] = f.listFiles();
			

			//boolean success = true;
			boolean success = (new File(dir+"_new")).mkdir();
			if(success)
			{
				//************combining the files*************
				for(File file : lf)
				{	System.out.println("Reading:"+file.getName());
					FileInputStream fs = new FileInputStream(file.getAbsolutePath());
					DataInputStream din = new DataInputStream(fs);
					BufferedReader br = new BufferedReader(new InputStreamReader(din),4000);
					String line;
					while ((line = br.readLine()) != null)
					{
						line = line.trim();
						if(line.split("\t").length==3 && line.split("\t")[2]!=null)
							distincturls.put(line.split("\t")[0], line.substring(line.indexOf("\t")+1));
					}

					din.close();
					br.close();
					fs.close();
				}
		
				
			}//if success ends

			
			//*************Weeding out the duplicate urls*********
			File fd3 = new File(dir+"_new/inputfile");
			if (!fd3.exists()) {
				fd3.createNewFile();
			}
			
			FileWriter fw3 = new FileWriter(fd3.getAbsoluteFile());
			BufferedWriter bw3 = new BufferedWriter(fw3);
			Iterator<Entry<String, String>> it2 = distincturls.entrySet().iterator();
			while (it2.hasNext()) {
				Map.Entry pairs = (Map.Entry)it2.next();
				if(!pairs.getKey().toString().matches("\\s+"))
					bw3.write(pairs.getKey()+"\t"+pairs.getValue()+"\n");
			}
			
			bw3.close();
			fw3.close();
			
			
			File file = new File(dir+"_new/inputfile");
			File file1 = new File(dir+"_new/inputfile");
			File file2 = new File(dir+"_new/inputfile");
			//boolean success1 = true;
			boolean success1 = (new File(dir+"_undangled")).mkdir();
			if(success1)
			{
				
				File fd = new File(dir+"_undangled/inputfile");
				if (!fd.exists()) {
					fd.createNewFile();
				}

				FileWriter fw = new FileWriter(fd.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);


				//*********Figuring out dangling links*************
				FileInputStream fs = new FileInputStream(file.getAbsolutePath());
				DataInputStream din = new DataInputStream(fs);
				BufferedReader br = new BufferedReader(new InputStreamReader(din),4000);
				String line;
				String[] s;
				while ((line = br.readLine()) != null)
				{	line.trim();
					if(line.contains("#&&#"))
					{
						h1.add(line.split("\t")[0]);
						s = line.split("\t")[2].split("#&&#");
						for(String s1 : s)
							h2.add(s1);
					}
				}
				h2.removeAll(h1); //now h2 contains the dangling links
				
				
				br.close();
				din.close();
				fs.close();

				//*******Figuring out back links********

				FileInputStream fs2 = new FileInputStream(file1.getAbsolutePath());
				DataInputStream din2 = new DataInputStream(fs2);
				BufferedReader br2 = new BufferedReader(new InputStreamReader(din2),4000);
				String line2;
				String backlinker;
				String[] outlinks;
				
				while ((line2 = br2.readLine()) != null)
				{
					if(line2.contains("#&&#"))
					{
						backlinker = line2.split("\t")[0];
						outlinks = line2.split("\t")[2].split("#&&#");
						for(String ol : outlinks)
						{
							if(h2.contains(ol)) //if it's a dangled link
							{
								
								ArrayList<String> backlinks = new ArrayList<String>();
								if(hm.get(ol) == null)
								{
									backlinks.add(backlinker);
									hm.put(ol,backlinks);
								}
								else
								{
									backlinks = hm.get(ol);
									if(!backlinks.contains(backlinker)) //check if backlinker is already put once
										backlinks.add(backlinker);
									hm.put(ol, backlinks);
									
								}
							}
						}
					}
				}
			

				br2.close();
				din2.close();
				fs2.close();

				//********Adding back links*********

				FileInputStream fs1 = new FileInputStream(file2.getAbsolutePath());
				DataInputStream din1 = new DataInputStream(fs1);
				BufferedReader br1 = new BufferedReader(new InputStreamReader(din1),4000);
				String line1;
		
				//copying the existing content
				while ((line1 = br1.readLine()) != null)
				{
					bw.write(line1+"\n");
				}
		
				//appending the backlinks
				Iterator<Entry<String, ArrayList<String>>> it = hm.entrySet().iterator();
				while (it.hasNext()) {
					String snew = "";
					Map.Entry pairs = (Map.Entry)it.next();
					ArrayList<String> temp = new ArrayList<String>(); 
					temp = (ArrayList<String>) pairs.getValue();
					for(String t : temp)
						snew += t+"#&&#";
					bw.write(pairs.getKey()+"\t1.000\t"+snew+"\n");
					it.remove(); 
				}
	

				din1.close();
				br1.close();
				fs1.close();
				bw.close();
				fw.close();
				

			}//if success1 ends



		}catch (Exception e){
			e.printStackTrace();
		}


	}

}