package indexer;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.Mapper;
import org.tartarus.snowball.SnowballStemmer;

public class IndexerMapper extends Mapper<Object, Text, Text, Text>{

	private Text word = new Text();
	private Text occurrence = new Text();
	public void map(Object key, Text value, Context context) throws IOException, InterruptedException 
	{
		//System.out.println(value);
		String[] split = value.toString().split("[\\s\\.,?)!}>\\]'\"”]*\\s+[\\s{'\"\\[(“<]*");
		FileSplit fileSplit = (FileSplit)context.getInputSplit();
		String filename = fileSplit.getPath().getName();
		try {
		    @SuppressWarnings("rawtypes")
			Class stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
		    SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
			for(int i=0; i<split.length; i++) {
				String origWord = split[i].trim();
				if(origWord.matches("\\s*")){
					continue;
				}
			    stemmer.setCurrent(split[i].trim());
				stemmer.stem();
				String stemmedWord = stemmer.getCurrent().toLowerCase();
				word.set(stemmedWord + " " + filename);
				occurrence.set(i + " " + origWord);
				context.write(word, occurrence);
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
