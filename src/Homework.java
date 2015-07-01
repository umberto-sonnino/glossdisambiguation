import it.uniroma1.lcl.babelnet.BabelGloss;
import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelSynsetComparator;
import it.uniroma1.lcl.jlt.ling.Word;
import it.uniroma1.lcl.jlt.util.Language;
import it.uniroma1.lcl.jlt.util.ScoredItem;
import it.uniroma1.lcl.knowledge.KnowledgeBase;
import it.uniroma1.lcl.knowledge.graph.KnowledgeGraph;
import it.uniroma1.lcl.knowledge.graph.KnowledgeGraphFactory;
import it.uniroma1.lcl.knowledge.graph.KnowledgeGraphScorer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.annolab.tt4j.TokenHandler;
import org.annolab.tt4j.TreeTaggerException;
import org.annolab.tt4j.TreeTaggerWrapper;

import com.google.common.collect.Sets;

import edu.mit.jwi.item.POS;


public class Homework {

	private static TreeTaggerWrapper<String> tt = new TreeTaggerWrapper<String>();
	private static PrintWriter writer;
	private static String processedGloss;
	private static String currentSynsetId;
	private static Language currentLanguage;
	private static HashSet<String> stringsToWrite = new HashSet<String>();
	private static List<Token> alignmentsToWrite = new ArrayList<Token>();
	private static HashMap<String, List<Token>> complexAlignments = new HashMap<String, List<Token>>();
	private static HashSet<String> stopwords;
	private static List<String> synsets;
	private static List<Token> existingTokens = new ArrayList<Token>();

	private final static HashSet<String> nounTags = Sets.newHashSet(
			"NNS", "NN", //Inglese 
			"NOM", "NPR", //Italiano
			"NC", "NP", "NMEA", "NMON",	//Spagnolo
			"NE",  //Tedesco [NN ci sta gia in inglese]
			"NAM" 		//Francese [NOM ci sta gia' in italiano]
			);
	private final static HashSet<String> verbTags = Sets.newHashSet(
			"VB", "VBG", "VBN", "RP", "VBD", "MD", "VBZ", "VBP", //Inglese
			"VER:cimp", "VER:cond", "VER:cpre","VER:futu","VER:geru","VER:impe","VER:impf", //Italiano
			"VER:infi","VER:pper","VER:ppre","VER:pres","VER:refl:infi","VER:remo", //Italiano
			"VEadj", "VEfin", "VEger", "VEinf", "VHadj", "VHfin", "VHger", "VHinf", //Spagnolo
			"VLadj", "VLfin", "VLger", "VLinf", "VMadj", "VMfin", "VMger", "VMinf", "VSadj", "VSfin", "VSger", "VSinf", //Spagnolo
			"VVFIN", "VAFIN", "VMFIN", "VAIMP", "VVIMP", "VVINF", "VAINF", "VMINF", "VVIZU", "VVPP", "VMPP", "VAPP", //TEDESCO
			"VER:cond", "VER:futu",	"VER:impe",	"VER:impf",	"VER:infi",	"VER:pper", "VER:ppre",	"VER:pres","VER:simp", "VER:subi", "VER:subp" //FRANCESE
			);
	private final static HashSet<String> adjectiveTags = Sets.newHashSet(
			"JJ", "JJR", "JJS", "JJR", //Inglese
			"ADJ", //Italiano & Spagnolo & Francese
			"ADJA", "ADJD" //Tedesco
			);

	private static final BabelNet instance = BabelNet.getInstance();

	public static void main(String[] args) throws IOException {

		System.setProperty("treetagger.home", "/Users/umbertosonnino/treetagger");

		stopwords = new HashSet<String>();
		synsets = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader("stopwords.txt"));
		String line = "";

		while( (line = br.readLine()) != null){
			stopwords.add(line);
		}
		br.close();

		br = new BufferedReader(new FileReader("Id_Synset.txt"));
		
		line = "";
		while ((line = br.readLine() ) != null){
			synsets.add(line);
		}

		long time = System.currentTimeMillis();

		try {

			tt.setHandler(new TokenHandler<String>() {
				public void token(String token, String pos, String lemma) {
					processedGloss += lemma + "$" + pos + " ";
				}
			});

			iterateSynsets(Language.EN);
			iterateSynsets(Language.IT);
			iterateSynsets(Language.DE);
			iterateSynsets(Language.FR);
			iterateSynsets(Language.ES);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TreeTaggerException e) {
			e.printStackTrace();
		} 
		finally {
			tt.destroy();
		}

		if(stringsToWrite.size() > 0){
			System.out.println("ADDING FINAL GLOSSES"); 
			writeToFile(currentLanguage.getName().toLowerCase());
		}
		if(alignmentsToWrite.size() > 0){
			System.out.println("ADDING FINAL ALIGNMENTS");
			writeAlignments();
		}
		if(existingTokens.size() > 0){
			for(Token t: existingTokens){
				appendExistingTokens(t);

			}
		}

		removeUnnecessary();
		System.out.println("TIME: " + (System.currentTimeMillis() - time));
	}


	private static boolean removeUnnecessary() throws IOException {

		File file = new File("alignments.txt");
		File newFile = new File("alignments_final.txt");

		BufferedReader br = new BufferedReader(new FileReader(file));

		String line;
		int i = 0;

		if(!newFile.exists())
			newFile.createNewFile();


		FileWriter fw = new FileWriter(newFile, true);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter writer = new PrintWriter(bw);


		while( (line = br.readLine()) != null ){
			i++;
			if(line.split("\t").length <= 4 ){
				System.out.println("ANDREBBE ELIMINATO! \n \t" + line);
				continue;
			}
			writer.println(line);
			System.out.println(i + " WRITING!");
		}


		file.delete();
		File oldName = new File("alignments.txt");
		newFile.renameTo(oldName);

		writer.close();
		br.close();
		
		return true;
	}


	/**
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws TreeTaggerException
	 */
	private static void iterateSynsets(Language lang) throws IOException, FileNotFoundException, TreeTaggerException {
		long i = 0;
		Iterator<String> synsetIterator = synsets.iterator();
		BabelSynset currentSynset = null;
		currentLanguage = lang;

		while(synsetIterator.hasNext() && i < 2100){
			currentSynsetId = synsetIterator.next();
			currentSynset = instance.getSynsetFromId(currentSynsetId);
			for(BabelGloss gloss : currentSynset.getGlosses(lang)){
				{
					packGlosses(gloss, currentLanguage.getName());
				}
			}
			i++;
			System.out.println(currentSynsetId + " " + i + " SYSNSETS PROCESSED");

		}

		if(stringsToWrite.size() > 0){
			writeToFile(currentLanguage.getName().toLowerCase());
		}
		if( alignmentsToWrite.size() > 0)
			writeAlignments();
	}

	private static void packGlosses(BabelGloss gloss, String language) throws IOException, TreeTaggerException {

		tt.setModel(language + "-utf8.par");
		String[] glossTokens = gloss.getGloss().split("\\s+");
		tt.process(glossTokens);

		String firstPart = gloss.getLanguage() + "#" + gloss.getSource() + "#" + gloss.getSourceSense();
		String newGlossString = firstPart + "\t" + processedGloss.trim();
		stringsToWrite.add(newGlossString);
		alignments(currentSynsetId, firstPart, processedGloss.trim());
		processedGloss = "";

		if(stringsToWrite.size() == 10000 || alignmentsToWrite.size() == 10000){
			writeAlignments();
			writeToFile(language);
		}

	}


	private static void alignments(String synsetId, String glossId, String gloss) throws IOException {

		String[] tokens = gloss.split("\\s+");
		List<Token> temp = new ArrayList<Token>();
		for(int i = 0 ; i < tokens.length ; ++i)
		{
			String t = tokens[i];
			int beginIndex = t.indexOf("$");
			String pos = t.substring(beginIndex + 1);
			if(nounTags.contains(pos))
				pos = Character.toString(POS.TAG_NOUN);
			if(verbTags.contains(pos))
				pos = Character.toString(POS.TAG_VERB);
			if(adjectiveTags.contains(pos))
				pos = Character.toString(POS.TAG_ADJECTIVE);
			String word = t.substring(0, beginIndex).replaceAll("\\W", "").toLowerCase();
			temp.add( new Token(synsetId, word, pos, currentLanguage.getName(), glossId, i) );
		}
		List<Word> disamb = new ArrayList<Word>();
		for(Token t : temp){
			Word w = new Word(t.getLemma(), t.getPos(), currentLanguage);
			disamb.add(w);
		}

		List<String> done = new ArrayList<String>();
		done = disambiguate(disamb, KnowledgeBase.BABELNET, KnowledgeGraphScorer.DEGREE);
		for(int i = 0; i< temp.size(); i ++){

			Token t = temp.get(i);
			String tokenSynsetId = done.get(i);

			t.setTokenSynsetId(tokenSynsetId);
		}

		alignmentsToWrite.addAll(temp);

	}


	private static void writeToFile(String language) throws IOException {
		File file = new File("glosses/" + language + "_glosses.txt");

		if(!file.exists())
			file.createNewFile();

		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter(fw);
		writer = new PrintWriter(bw);
		for(String s : stringsToWrite){
			writer.println(s);
		}
		writer.close();
		stringsToWrite.clear();

		System.out.println("\n-------DATA SUCCESSFULLY APPENDED TO THE END OF THE FILE-------\n");
	}


	private static void writeAlignments() throws IOException {
		File file = new File("alignments.txt");
		if(!file.exists())
			file.createNewFile();

		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter(fw);
		writer = new PrintWriter(bw);
		for(Token tok : alignmentsToWrite){
			if(tok.getTokenSynsetId().equals("NOT HERE") || stopwords.contains(tok.getLemma()))
				continue;
			if(checkTokenSynsetExists(tok))
				continue;
			String toFile = tok.getSynsetId() + "\t" + tok.getTokenSynsetId() + "\t" + tok.getGlossId() /*+ "\t" + tok.getLemma() */ + "\t" + tok.getPosition();
			writer.println(toFile);
		}

		writer.close();
		alignmentsToWrite.clear();

		System.out.println("\n-------ALIGNMENTS SUCCESSFULLY APPENDED TO THE END OF THE FILE-------\n");


	}

	private static boolean checkTokenSynsetExists(Token t) throws IOException{

		boolean result = false;
		File file = new File("alignments.txt");

		BufferedReader br = new BufferedReader(new FileReader(file));

		String line;
		int i = 0;
		String toBeAdded = "\t" + t.getGlossId() + "\t" + t.getPosition();
		while( (line = br.readLine()) != null ){
			i++;
			if( line.contains(t.getSynsetId() + "\t" + t.getTokenSynsetId())) {
				line += toBeAdded;
				result = true;
				existingTokens.add(t);
			}
			System.out.println("READING");
		}

		br.close();

		return result;
	}

	private static void appendExistingTokens(Token tok) throws IOException {

		File file = new File("alignments.txt");
		File newFile = new File("alignments_temp.txt");
		if(!newFile.exists())
			newFile.createNewFile();

		BufferedReader br = new BufferedReader(new FileReader(file));

		FileWriter fw = new FileWriter(newFile, true);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter writer = new PrintWriter(bw);

		String line;
		int i = 0;
		String toBeAdded = "\t" + tok.getGlossId() + /*"\t" + tok.getLemma() + */ "\t" + tok.getPosition();

		while( (line = br.readLine()) != null ){
			i++;
			if( line.contains(tok.getSynsetId() + "\t" + tok.getTokenSynsetId() )){
				if( line.contains(tok.getGlossId())){
					System.out.println(i + " WRITING!");
					writer.println(line);
					continue;
				}
				System.out.println("TROVATO! ALLA RIGA " + i + "\t" + line);
				line += toBeAdded;
			}
			writer.println(line);
			System.out.println(i + " WRITING!");
		}


		file.delete();
		File oldName = new File("alignments.txt");
		newFile.renameTo(oldName);

		writer.close();
		br.close();


	}


	public static void writeGlossesToFile(String language, BabelGloss gloss) throws FileNotFoundException, TreeTaggerException{

		try{

			File file = new File("glosses/" + language + "_glosses.txt");

			if(!file.exists())
				file.createNewFile();

			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			writer = new PrintWriter(bw);
			String firstPart = gloss.getLanguage() + "#" + gloss.getSource() + "#" + gloss.getSourceSense();

			writer.println(firstPart + "\t" + gloss.getGloss()); //processedGloss.trim());

			writer.close();

			System.out.println("Data successfully appended at the end of file");

		} catch (IOException ioe){
			System.err.println("EXCEPTION!");
			ioe.printStackTrace();
		}

	}

	public static List<String> disambiguate(Collection<Word> words, KnowledgeBase kb, KnowledgeGraphScorer scorer) throws IOException {
		KnowledgeGraphFactory factory = KnowledgeGraphFactory.getInstance(kb);
		KnowledgeGraph kGraph = factory.getKnowledgeGraph(words);
		Map<String, Double> scores = scorer.score(kGraph);
		List<String> goodValues = new ArrayList<String>();

		int firstItem = 0;

		for (String concept : scores.keySet()) {
			double score = scores.get(concept);
			for (Word word : kGraph.wordsForConcept(concept))
				word.addLabel(concept, score);
		}
		for (Word word : words) {
			firstItem = 0;
			if(word.getLabels().size() <= 0){
				String toSearch = word.getWord();
				List<BabelSynset> wordSynsets = instance.getSynsets(word.getLanguage(), toSearch);
				if(wordSynsets.size() > 0){
					Collections.sort(wordSynsets, new BabelSynsetComparator(toSearch));
					String mainSenseId = wordSynsets.get(0).getId();
					goodValues.add(mainSenseId);
				}else{
					goodValues.add("NOT HERE");
				}
			}
			for (ScoredItem<String> label : word.getLabels()) {

				if(firstItem == 0){
					goodValues.add(label.getItem());
				}
				firstItem++;

			}
		}

		return goodValues;

	}

}
