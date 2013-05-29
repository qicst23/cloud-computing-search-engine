/**
 * 
 */
package indexer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * @author cis455
 *
 */
public class RIReducer extends Reducer<Text, Text, Text, Text> {
	private Text outKey = new Text();
	private Text outValue = new Text();

	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{
		String strKey = key.toString();
		if(strKey.startsWith("\"\"\"\"")){ //tf and dvl
			HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
			for(Text val : values){
				String word = val.toString();
				if(wordCount.containsKey(word)){
					wordCount.put(word, wordCount.get(word)+1);
				}
				else{
					wordCount.put(word, 1);
				}
			}
			double dvl = 0;
			for(String word: wordCount.keySet()){
				int count =  wordCount.get(word);
				dvl += count*count;
				outKey.set(word + " " + strKey.substring(4));
				outValue.set("" + count);
				context.write(outKey, outValue); //tf
			}
			outKey.set(strKey);
			outValue.set("" + Math.sqrt(dvl));
			context.write(outKey, outValue); //dvl		
		}
		else{ //df
			Set<String> set = new HashSet<String>();
			for (Text docHash : values) {
				set.add(docHash.toString());
			}
			outKey.set(strKey); //strKey = word
			outValue.set("" + set.size());
			context.write(outKey, outValue);
		}
	}
}
