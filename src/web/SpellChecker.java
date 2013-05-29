package web;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * @author Norvig
 * reference: http://raelcunha.com/spell-correct.php
 *
 */
class SpellChecker {
	
	private final HashMap<String, Integer> nWords = new HashMap<String, Integer>();

	public SpellChecker(String file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		Pattern p = Pattern.compile("\\w+");
		for(String temp = ""; temp != null; temp = in.readLine()){
			Matcher m = p.matcher(temp.toLowerCase());
			while(m.find()) nWords.put((temp = m.group()), nWords.containsKey(temp) ? nWords.get(temp) + 1 : 1);
		}
		in.close();
	}

	private final ArrayList<String> edits(String word) {
		ArrayList<String> result = new ArrayList<String>();
		for(int i=0; i < word.length(); ++i) result.add(word.substring(0, i) + word.substring(i+1));
		for(int i=0; i < word.length()-1; ++i) result.add(word.substring(0, i) + word.substring(i+1, i+2) + word.substring(i, i+1) + word.substring(i+2));
		for(int i=0; i < word.length(); ++i) for(char c='a'; c <= 'z'; ++c) result.add(word.substring(0, i) + String.valueOf(c) + word.substring(i+1));
		for(int i=0; i <= word.length(); ++i) for(char c='a'; c <= 'z'; ++c) result.add(word.substring(0, i) + String.valueOf(c) + word.substring(i));
		return result;
	}

	public final String correct(String word) {
		if(nWords.containsKey(word)) return word;
		ArrayList<String> list = edits(word);
		HashMap<Integer, String> candidates = new HashMap<Integer, String>();
		for(String s : list) if(nWords.containsKey(s)) candidates.put(nWords.get(s),s);
		if(candidates.size() > 0) return candidates.get(Collections.max(candidates.keySet()));
		for(String s : list) for(String w : edits(s)) if(nWords.containsKey(w)) candidates.put(nWords.get(w),w);
		return candidates.size() > 0 ? candidates.get(Collections.max(candidates.keySet())) : word;
	}
	
	/**  
	 * added functions for searching query
	 * @return
	 */
	public final static String correctQuery(String query){
		String dictRoot = "/Users/Alantyy/Documents/workspace/UI/src/web/big.txt";
		String correctToken;
		StringBuffer correctQuery = new StringBuffer();
		for(String word : query.split("\\s+")){
			try {
				correctToken = (new SpellChecker(dictRoot).correct(word)).trim();
				correctQuery.append(correctToken + " ");
			} catch (IOException e) {
				e.printStackTrace();
				return query;
			}
		}
		return correctQuery.toString().trim();
	
	
	}
	public static void main(String args[]) throws IOException {
		String query = "tyhannk yuuou";
		String corrected = correctQuery(query);
		System.out.println("Are you meaning:" + corrected + "?");
	}

}
