/**
 * 
 */
package indexer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import storage.ContentClient;

/**
 * @author cis455
 *
 */
public class IndexerEMR {
	protected static ContentClient contentClient = null;
	protected static Charset utf8 = Charset.forName("UTF-8");
	protected static int numDocUrls; //number of docurls to get before terminating
	protected static String dfResultFolder = "dfCounterResult";
	protected static String indexerResultFolder = "indexerResult";
	protected static String tfResultFolder = "tfCounterResult";
	protected static String dvlResultFolder = "dvlResult";
	protected static boolean debugMode = true;
	protected static boolean printErrors = true;
	static Path unstemmedDir;
	static Path stemmedDir;
	static Path outputDir;
	
	
	protected static void printDebug(String str){
		if(debugMode){
			System.out.println(str);
		}
	}
	
	protected static void printError(String str){
		if(printErrors){
			System.err.println(str);
		}
	}
	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
		String[] otherArgs = null;
		try {
			otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		if (otherArgs.length<1) {
			System.err.println("Usage: Indexer <folder>");
			System.exit(2);
		}
		Path inputDir = new Path(otherArgs[0]);
		outputDir = new Path(inputDir, "outputs");
		unstemmedDir = new Path(inputDir, "unstemmed");
		stemmedDir = new Path(inputDir, "stemmed");
		Job indexerJob = indexerMain(args);
		Job dfCounterJob = dfCounterMain(args);
		Job tfCounterJob = tfCounterMain(args);
		Job dvlJob = dvlMain(args);
		printDebug("starting jobs");
		indexerJob.waitForCompletion(true);
		printDebug("indexer finished");
		dfCounterJob.waitForCompletion(true);
		printDebug("dfCounter finished");
		tfCounterJob.waitForCompletion(true);
		printDebug("tfCounter finished");
		dvlJob.waitForCompletion(true);
		printDebug("dvl finished");
	}
	public static Job dfCounterMain(String[] args) throws IOException{
		Configuration conf = new Configuration();
		Job job = new Job(conf, "dfCounter");
		job.setJarByClass(Indexer.class);
		job.setMapperClass(DfCounterMapper.class);
		job.setReducerClass(DfCounterReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, stemmedDir);
		FileOutputFormat.setOutputPath(job, new Path(outputDir, dfResultFolder));
		return job;
	}

	public static Job indexerMain(String[] args) throws IOException{
		Configuration conf = new Configuration();
		Job job = new Job(conf, "indexer");
		job.setInputFormatClass(MyFileInputFormat.class);
		job.setJarByClass(Indexer.class);
		job.setMapperClass(IndexerMapper.class);
		job.setReducerClass(IndexerReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, unstemmedDir);
		FileOutputFormat.setOutputPath(job, new Path(outputDir, indexerResultFolder));
		return job;
	}

	public static Job tfCounterMain(String[] args) throws IOException {
		Configuration conf = new Configuration();
		Job job = new Job(conf, "indexer");
		job.setJarByClass(TfCounterMain.class);
		job.setMapperClass(TfCounterMapper.class);
		job.setCombinerClass(TfCounterReducer.class);
		job.setReducerClass(TfCounterReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, stemmedDir);
		FileOutputFormat.setOutputPath(job, new Path(outputDir, tfResultFolder));
		return job;
	}

	public static Job dvlMain(String[] args) throws IOException{
		Configuration conf = new Configuration();
		Job job = new Job(conf, "indexer");
		job.setJarByClass(DVLMain.class);
		job.setInputFormatClass(KeyValueTextInputFormat.class);
		job.setMapperClass(DVLMapper.class);
		job.setReducerClass(DVLReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(outputDir, tfResultFolder));
		FileOutputFormat.setOutputPath(job, new Path(outputDir, dvlResultFolder));
		return job;
	}
}
