/**
 * 
 */
package ranker;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import storage.ContentClient;


/**
 * @author cis455
 *
 */
public class PageRank {

	public static boolean isIntraDomain(String h1,String h2)
	{
		try{
			boolean b = false;
			
			if(h1.startsWith("www."))
				h1 = h1.substring(4);
			if(h1.startsWith("http://"))
				h1 = h1.substring(7);
			if(h1.startsWith("https://"))
				h1 = h1.substring(8);
			if(h1.contains("/"))
				h1 = h1.split("/")[0];

			if(h2.startsWith("www."))
				h2 = h2.substring(4);
			if(h2.startsWith("http://"))
				h2 = h2.substring(7);
			if(h2.startsWith("https://"))
				h2 = h2.substring(8);
			if(h2.contains("/"))
				h2 = h2.split("/")[0];

			b = h1.equalsIgnoreCase(h2);			
			return b;
		}
		catch(Exception e)
		{
			return false;
		}
	}


	public static class TokenizerMapper 
	extends Mapper<Object, Text, Text, Text>{

		private Text word = new Text();

		public void map(Object key, Text value, Context context
				) throws IOException, InterruptedException {
			String[] val = value.toString().split("\t");

			Text mainlink = new Text(val[0].trim());
			double oldrank = Double.parseDouble(val[1]);
			if(val.length>=3)
			{
				if(!val[2].contains("####"))
				{
					String[] outlinks = val[2].split("#&&#");
					double noofoutlinks = outlinks.length;
					double factor = 1.000;
					for(int i=0;i<outlinks.length;i++)
					{
						word.set(outlinks[i].trim());
						if(isIntraDomain(val[0], outlinks[i]))
							factor = factor * 0.80;
						if(outlinks[i].endsWith(".css") || outlinks[i].endsWith(".ico") || outlinks[i].endsWith(".js"))
							factor = factor * 0.10;
						
						context.write(word, new Text(String.valueOf(factor*oldrank/noofoutlinks)));
					}
					context.write(mainlink, new Text(val[2].trim()));
				}
				context.write(mainlink, new Text(val[1].trim()+"####"));
			}

		}
	}

	public static class IntSumReducer 
	extends Reducer<Text,Text,Text,Text> {

		public void reduce(Text key, Iterable<Text> values, 
				Context context
				) throws IOException, InterruptedException {
			double sum = 0.0000;
			double stopsum = 0.0000;
			String outlinks = "";
			int flag = 0;

			for (Text val : values) {
				if(val.toString().contains("#&&#"))
					outlinks = val.toString().trim();
				else if(val.toString().contains("####"))
				{
					stopsum = Double.parseDouble(val.toString().split("####")[0]);
				}
				else
				{
					flag = 1;
					sum += Double.parseDouble(val.toString());
				}
			}


			if(flag == 1)
			{
				context.write(key, new Text(String.valueOf(0.15+(0.85*sum))+"\t"+outlinks));
			}
			else
			{
				context.write(key, new Text(String.valueOf(stopsum)+"\t####"));
			}

		}


	}


	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: pagerank <input folder> <output folder> <iterations>");
			System.exit(2);
		}


		//DanglingLinksHandler dlh = new DanglingLinksHandler();
		//dlh.handleDanglingLinks(otherArgs[0]);

		int iterations = Integer.parseInt(otherArgs[2]);
		for(int i=0;i<iterations;i++)
		{
			Job job = new Job(conf, "page rank");
			job.setJarByClass(PageRank.class);
			job.setMapperClass(TokenizerMapper.class);
			job.setReducerClass(IntSumReducer.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			if(i==0)
				FileInputFormat.addInputPath(job, new Path(otherArgs[0]+"_undangled"));
			else
				FileInputFormat.addInputPath(job, new Path(otherArgs[1]+(i-1)));

			if(i==iterations-1)
				FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
			else
				FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]+i));
			job.waitForCompletion(true);
		}	
		
		
		
	}//main ends



}
