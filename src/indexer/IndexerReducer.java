package indexer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class IndexerReducer extends Reducer<Text,Text,Text,Text> {
	private Text result = new Text();
	public void reduce(Text key, Iterable<Text> values, 
			Context context
			) throws IOException, InterruptedException {
		Set<String> collect = new HashSet<String>();
		for (Text val : values) {
			//System.out.println("a value: " + val);
			collect.add(val.toString());
		}
		String[] temp = new String[collect.size()];
		result.set(collectStrings(collect.toArray(temp)));
		//System.out.println("reducer out: " + key + " " + result);
		context.write(key, result);
	}
	
	public String collectStrings(String[] array){
		if(array==null || array.length==0) return "";
		StringBuilder s = new StringBuilder(array[0]);
		for(int i=1; i<array.length; i++){
			s.append(" ");
			s.append(array[i]);
		}
		return s.toString();
	}
	
}
