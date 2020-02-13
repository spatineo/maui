package com.entopix.maui.vocab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import com.entopix.maui.filters.MauiPhraseFilter;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds an index with the content of the controlled vocabulary.
 * Accepts vocabularies as rdf files (SKOS format) and in plain text format:
 * vocabulary_name.en (with "ID TERM" per line) - descriptors and non-descriptors
 * vocabulary_name.use (with "ID_NON-DESCR \t ID_DESCRIPTOR" per line)
 * vocabulary_name.rel (with "ID \t RELATED_ID1 RELATED_ID2 ... " per line)
 *
 * @author Alyona Medelyan (medelyan@gmail.com)
 */
public class Vocabulary {

	private static final Logger log = LoggerFactory.getLogger(Vocabulary.class);

	private VocabularyStore vocabStore;
	private String vocabularyName;

	/** Document language */
	private String language = "en";
	/** Document encoding */
	private String encoding = "UTF-8";
	/** Default stemmer to be used */
	private transient Stemmer stemmer;
	/** List of stopwords to be used */
	private transient Stopwords stopwords;
	/** Normalization to lower case - default true */
	private boolean toLowerCase = true;
	/** Normalization via alphabetic reordering - default true*/
	private boolean reorder = true;
	private boolean serialize = false;


	/** Initializes vocabulary from a file path
	 *
	 * Given the file path to the vocabulary and the format, 
	 * it first checks whether this file exists:<br>
	 * - vocabularyName.rdf or vocabularyName.rdf.gz if skos format is selected<br>
	 * - or a set of 3 flat txt files starting with vocabularyName and with extensions<br>
	 * * .en (id term) - the path to this file should be supplied as the main parameters
	 * * .use (non-descriptor \t descriptor)
	 * * .rel (id \t related_id1 related_id2 ...)
	 * If the required files exist, the vocabulary index is built.
	 *
	 * @param vocabularyName The name of the vocabulary file (before extension).
	 * @param vocabularyFormat The format of the vocabulary (skos or text).
	 * */
	public void initializeVocabulary(String vocabularyName, String vocabularyFormat) {

		this.vocabularyName = vocabularyName;

		if (vocabularyFormat.equals("skos")) {

			if (!vocabularyName.endsWith(".rdf.gz") && !vocabularyName.endsWith("rdf")) {
				log.error("Error while loading vocabulary from " + vocabularyName);
				throw new RuntimeException("File " + vocabularyName + " appears to be not in the skos format!");
			}

			File skosFile = new File(vocabularyName);
			
			
			try {
				if (!skosFile.exists()) {
					InputStream resourceStream = Vocabulary.class.getClassLoader().getResourceAsStream(vocabularyName);
					
					if (resourceStream == null) {
						throw new IOException("No such file or resource");
					} else {
						if (vocabularyName.endsWith(".gz")) {
							resourceStream = new GZIPInputStream(resourceStream);
						}
					}
					initializeFromStream(resourceStream);
				} else {
					initializeFromSKOSFile(skosFile);
				}
				
			} catch(Exception ie) {
				log.error("Error while loading vocabulary from " + vocabularyName);
				throw new RuntimeException("Error while loading vocabulary from "+vocabularyName, ie);
			}

		} else if (vocabularyFormat.equals("text")) {

			/** Location of the vocabulary's *.en file
			 * containing all terms of the vocabularies and their ids.*/
			File enFile = new File(vocabularyName);
			if (!enFile.exists()) {
				log.error("Error while loading vocabulary from " + vocabularyName);
				throw new RuntimeException(enFile.getAbsolutePath() + " does not exist!");
			}

			/** Location of the vocabulary's *.use file
			 * containing ids of non-descriptor with the corresponding ids of descriptors.*/
			File useFile = new File(vocabularyName.replace(".en", ".use"));
			if (!useFile.exists()) {
				log.error("Error while loading vocabulary from " + vocabularyName);
				throw new RuntimeException(useFile.getAbsolutePath() + " does not exist!");
			}

			/** Location of the vocabulary's *.rel file
			 * containing semantically related terms for each descriptor in the vocabulary.*/
			File relFile = new File(vocabularyName.replace(".en", ".rel"));
			if (!relFile.exists()) {
				log.error("Error while loading vocabulary from " + vocabularyName);
				throw new RuntimeException(relFile.getAbsolutePath() + " does not exist!");
			}
			initializeFromTXTFiles(enFile, useFile, relFile);

		} else {
			throw new RuntimeException(vocabularyFormat
					+ " is an unsupported vocabulary format! Use skos or text");
		}

	}


	/** Initializes vocabulary from an RDF Model object
	 *  
	 * @param vocabularyName The name of the vocabulary file (before extension).
	 * @param model RDF Model of the SKOS contents of the vocabulary.
	 * */
	public void initializeVocabulary(String vocabularyName, Model model) throws VocabularyException {
		this.vocabularyName = vocabularyName;
		if (model != null) {
			initializeFromModel(model);
		} else {
			throw new VocabularyException("Model can't be null!");
		}
	}



	public void setLanguage(String language) {
		this.language = language;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setLowerCase(boolean toLowerCase) {
		this.toLowerCase = toLowerCase;
	}

	public void setReorder(boolean reorder) {
		this.reorder = reorder;
	}

	public void setStemmer(Stemmer stemmer) {
		this.stemmer = stemmer;
	}

	public void setVocabularyStore(VocabularyStore store) {
		vocabStore = store;
	}

	public void setSerialize(boolean serialize) {
		this.serialize = serialize;
	}



	/**
	 * Loading RDF Model into a VocabularyStore structure for fast access.
	 */
	public void initializeFromModel(Model model) {

		vocabStore = VocabularyStoreFactory.createVocabStore(vocabularyName, stemmer, serialize);

		// we already have a de-serialized vocabStore
		if (vocabStore.isInitialized()) {
			return;
		}

		log.info("--- Building the Vocabulary index from the RDF model...");

		ResIterator iter;
		Statement stmt;
		// for some reason Jena doesn't predefine the owl:deprecated property
		Property owlDeprecated = ResourceFactory.createProperty(OWL.NS, "deprecated");

		// to create IDs for non-descriptors!
		int count = 0;
		// Iterating over all concepts in the SKOS file
		iter = model.listResourcesWithProperty(RDF.type, SKOS.Concept);

		while (iter.hasNext()) {
			Resource concept = iter.nextResource();

			// check whether it's deprecated
			stmt = concept.getProperty(owlDeprecated);
			if (stmt != null) {
				if (stmt.getBoolean() == true) {
					continue; // skip deprecated concept
				}
			}

			// id of the concept (Resource), e.g. "c_4828"
			String id_string = concept.getURI();

			// preferred label
			stmt = concept.getProperty(SKOS.prefLabel, this.language);
			if (stmt != null) {
				Literal descriptor = stmt.getLiteral();
				String descriptorNormalized = normalizePhrase(descriptor.getLexicalForm());
				if (descriptorNormalized.length() >= 1) {
					vocabStore.addSense(descriptorNormalized, id_string);
					vocabStore.addDescriptor(id_string, descriptor.getLexicalForm());
				}
			}

			// alternate and hidden labels
			Property[] nondescriptorProps = { SKOS.altLabel, SKOS.hiddenLabel };
			for (Property prop : nondescriptorProps) {
				StmtIterator statements = concept.listProperties(prop, this.language);
				while (statements.hasNext()) {
					stmt = statements.nextStatement();
					Literal non_descriptor = stmt.getLiteral();
					String non_descriptorNormalized = normalizePhrase(non_descriptor.getLexicalForm());
					if (non_descriptorNormalized.length() >= 1) {
						vocabStore.addSense(non_descriptorNormalized, id_string);
					}
					addNonDescriptor(count, id_string, non_descriptor.getLexicalForm(), non_descriptorNormalized);
					count++;
				}
			}

			Property[] relationProps = { SKOS.broader, SKOS.narrower, SKOS.related };
			for (Property prop : relationProps) {
				StmtIterator statements = concept.listProperties(prop);
				while (statements.hasNext()) {
					stmt = statements.nextStatement();
					// adds directly related term
					vocabStore.addRelatedTerm(id_string, stmt.getResource().getURI());
				}
			}
		}

		log.info("--- Statistics about the vocabulary: ");
		log.info("\t" + vocabStore.getNumTerms() + " terms in total");
		log.info("\t" + vocabStore.getNumNonDescriptors() + " non-descriptive terms");
		log.info("\t" + vocabStore.getNumRelatedTerms() + " terms have related terms");
	
		vocabStore.finishedInitialized();

		if (serialize) {
			VocabularyStoreFactory.serializeNewVocabStore(vocabularyName, vocabStore, stemmer);
		}
	}



	/**
	 * Loads the Model from the SKOS file first, then initializes it.
	 */
	public void initializeFromSKOSFile(File skosFile) throws IOException {

		if (serialize) {
			vocabStore = VocabularyStoreFactory.createVocabStore(vocabularyName, stemmer, serialize);

			// we already have a de-serialized vocabStore
			if (vocabStore.isInitialized()) {
				return;
			} else {
				Model model = readModelFromFile(skosFile);
				initializeFromModel(model);
			}
		} else {
			Model model = readModelFromFile(skosFile);
			initializeFromModel(model);
		}
	}
	
	public void initializeFromStream(InputStream stream) throws IOException {

		if (serialize) {
			vocabStore = VocabularyStoreFactory.createVocabStore(vocabularyName, stemmer, serialize);

			// we already have a de-serialized vocabStore
			if (vocabStore.isInitialized()) {
				return;
			} else {
				Model model = readModelFromInputStream(stream);
				initializeFromModel(model);
			}
		} else {
			Model model = readModelFromInputStream(stream);
			initializeFromModel(model);
		}
	}

	private Model readModelFromFile(File skosFile) throws IOException {
		log.info("--- Loading RDF model from the SKOS file...");
		InputStream stream;
		
		if (skosFile.getName().endsWith("rdf.gz")) {
			stream = new GZIPInputStream(new FileInputStream(skosFile));
		} else {
			stream = new FileInputStream(skosFile);
		}
		
		return readModelFromInputStream(stream);
		
	}
	
	private Model readModelFromInputStream(InputStream stream) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		model.read(new InputStreamReader(stream, encoding), "");
		return model;
	}



	/**
	 * Loading data from text files into a Vocabulary Store object for fast access.
	 * 
	 */
	public void initializeFromTXTFiles(File enFile, File useFile, File relFile) {

		vocabStore = VocabularyStoreFactory.createVocabStore(vocabularyName, stemmer, serialize);

		// we already have a de-serialized vocabStore
		if (vocabStore.isInitialized()) {
			return;
		}

		log.info("--- Loading Vocabulary from text files...");
		buildTEXT(enFile);
		buildUSE(useFile);
		buildREL(relFile);

		vocabStore.finishedInitialized();

		if (serialize) {
			VocabularyStoreFactory.serializeNewVocabStore(vocabularyName, vocabStore, stemmer);
		}
	}

	/**
	 * Set the stopwords class.
	 * @param stopwords
	 */
	public void setStopwords(Stopwords stopwords) {
		this.stopwords = stopwords;
	}

	public VocabularyStore getVocabularyStore() {
		return vocabStore;
	}


	private void addNonDescriptor(int count, String idDescriptor,
			String nonDescriptor, String normalizedNonDescriptor) {

		if (vocabularyName.equals("lcsh") && nonDescriptor.indexOf('(') != -1) {
			return;
		}

		String idNonDescriptor = "d_" + count;

		if (normalizedNonDescriptor.length() >= 1) {
			vocabStore.addSense(normalizedNonDescriptor, idNonDescriptor);
		}

		vocabStore.addDescriptor(idNonDescriptor, nonDescriptor);
		vocabStore.addNonDescriptor(idNonDescriptor, idDescriptor);
	}

	public String getFormatedName( String in )
	{
		return vocabStore.getFormatedName( in );
	}

	/**
	 * Builds the vocabulary index from the text files.
	 */
	public void buildTEXT(File enFile) {

		log.info("-- Building the Vocabulary index");

		String readline;
		String term;
		String avterm;
		String id_string;
		try {
			InputStreamReader is = new InputStreamReader(new FileInputStream(enFile));
			BufferedReader br = new BufferedReader(is);
			while ((readline = br.readLine()) != null) {
				int i = readline.indexOf(' ');
				term = readline.substring(i + 1);
	
				avterm = normalizePhrase(term);
	
				if (avterm.length() >= 1) {
					id_string = readline.substring(0, i);
					vocabStore.addDescriptor(id_string, term);
				}
			}
			br.close();
			is.close();
		} catch (IOException e) {
			log.error("Error while loading vocabulary from " + enFile.getAbsolutePath() + "!\n", e);
			throw new RuntimeException();
		}
	}

	/**
	 * Builds the vocabulary index with descriptors/non-descriptors relations.
	 */
	public void buildUSE(File useFile) {
		String readline;
		String[] entry;
		try {
			InputStreamReader is = new InputStreamReader(new FileInputStream(useFile));
			BufferedReader br = new BufferedReader(is);
			while ((readline = br.readLine()) != null) {
				entry = readline.split("\t");
				//	if more than one descriptors for
				//	one non-descriptors are used, ignore it!
				//	probably just related terms (cf. latest edition of Agrovoc)
				if ((entry[1].indexOf(" ")) == -1) {
					vocabStore.addNonDescriptor(entry[0], entry[1]);
				}
			}
			br.close();
			is.close();
		} catch (IOException e) {
			log.error("Error while loading vocabulary from " + useFile.getAbsolutePath() + "!\n", e);
			throw new RuntimeException();
		}
	}

	/**
	 * Builds the vocabulary index with semantically related terms.
	 */
	public void buildREL(File relFile) {
		log.info("-- Building the Vocabulary index with related pairs");
		String readline;
		String[] entry;
		try {
			InputStreamReader is = new InputStreamReader(new FileInputStream(relFile));
			BufferedReader br = new BufferedReader(is);
			while ((readline = br.readLine()) != null) {
				entry = readline.split("\t");
				String[] temp = entry[1].split(" ");
				for (int i = 0; i < temp.length; i++) {
					vocabStore.addRelatedTerm(entry[0], temp[i]);
				}
			}
			br.close();
			is.close();
		} catch (IOException e) {
			log.error("Error while loading vocabulary from " + relFile.getAbsolutePath() + "!\n", e);
			throw new RuntimeException();
		}
	}

	/**
	 * Returns the term for the given id (as a string)
	 * @param id - id of some phrase in the vocabulary
	 * @return phrase, i.e. the full form listed in the vocabulary
	 */
	public String getTerm(String id) {
		return vocabStore.getTerm(id);
	}

	/**
	 * Checks whether a normalized phrase
	 * is a valid vocabulary term.
	 * @param phrase
	 * @return true if phrase is in the vocabulary
	 */
	public boolean containsNormalizedEntry(String phrase) {
		return vocabStore.getNumSenses(normalizePhrase(phrase)) > 0;
	}

	/**
	 * Returns true if a phrase has more than one senses
	 * @param phrase
	 * @return false if a phrase has only one sense
	 */
	public boolean isAmbiguous(String phrase) {
		return vocabStore.getNumSenses(normalizePhrase(phrase)) > 1;
	}

	/**
	 * Retrieves all possible descriptors for a given phrase
	 * @param phrase
	 * @return a vector list of all senses of a given term
	 */
	public ArrayList<String> getSenses(String phrase) {
		String normalized = normalizePhrase(phrase);
		ArrayList<String> senses = vocabStore.getSensesForPhrase(normalized);
		return senses;
	}

	/**
	 * Given id of a term returns the list with ids of terms related to this term.
	 * @param id
	 * @return a vector with ids related to the input id
	 */
	public ArrayList<String> getRelated(String id) {
		return vocabStore.getRelatedTerms(id);
	}

	/***
	 * Returns falls if the phrase only contains upper case characters
	 * @param phrase
	 * @return
	 */
	private boolean isOkToLower(String phrase) {
		int lower = 0;
		int upper = 0;
		for (char p : phrase.toCharArray()) {
			if (Character.isLowerCase(p)) {
				lower++;
			}
			if (Character.isUpperCase(p)) {
				upper++;
			}
		}

		// don't lower case words containing 5 or less characters
		// that are all capitalized (most likely it's an abbreviation)
		if (upper > lower && upper < 5) {
			return false;
		}
		return true;
	}

	/**
	 * Generates the pseudo phrase from a string.
	 * A pseudo phrase is a version of a phrase
	 * that only contains non-stopwords,
	 * which are stemmed and sorted into alphabetical order.
	 */
	public String normalizePhrase(String phrase) {

		String orig = phrase;
		if (orig.endsWith("-") || orig.endsWith(".")) {
			return orig;
		}
		MauiPhraseFilter filter = new MauiPhraseFilter();
		phrase = filter.tokenize(phrase).replace('\n', ' ');
		StringBuilder result = new StringBuilder();
		char prev = ' ';
		int i = 0;
		while (i < phrase.length()) {
			char c = phrase.charAt(i);

			// we ignore everything after the "/" symbol and everything in brackets
			// e.g. Monocytes/*immunology/microbiology -> monocytes
			if (vocabularyName.equals("mesh") && c == '/') {
				break;
			}

			if (c == '&' || c == '.' || c == '.') {
				c = ' ';
			}

			if (c == '*' || c == ':') {
				prev = c;
				i++;
				continue;
			}

			if (c != ' ' || prev != ' ') {
				result.append(c);
			}

			prev = c;
			i++;
		}


		phrase = result.toString().trim();
		//        int indexOfOpen = phrase.indexOf('(');
		//        if (indexOfOpen != -1) {
		//            int indexOfClose = phrase.indexOf(')');
		//            if (indexOfOpen < indexOfClose && indexOfOpen != 0) {
		//                if (indexOfClose == phrase.length()) {
		//                    phrase = phrase.substring(0, indexOfOpen - 1);
		//                } else {
		//                    phrase = phrase.substring(0, indexOfOpen - 1) + phrase.substring(indexOfClose + 1);
		//                }
		//            }
		//        }


		if (isOkToLower(phrase) && toLowerCase) {
			phrase = phrase.toLowerCase();
		}

		if (reorder || stopwords != null || stemmer != null) {
			phrase = pseudoPhrase(phrase);
		}
		if (phrase.equals("")) {
			// to prevent cases where the term is a stop word (e.g. Back).
			return result.toString();
		} else {
			// log.info(orig + " >> " + phrase);
			return phrase;
		}
	}

	/**
	 * Generates the preudo phrase from a string.
	 * A pseudo phrase is a version of a phrase
	 * that only contains non-stopwords,
	 * which are stemmed and sorted into alphabetical order.
	 */
	public String pseudoPhrase(String str) {
		String result = "";
		String[] words = str.split(" ");
		if (reorder) {
			Arrays.sort(words);
		}
		for (String word : words) {

			if (stopwords != null) {
				if (stopwords.isStopword(word)) {
					continue;
				}
			}
			int apostr = word.indexOf('\'');
			if (apostr != -1 && apostr == word.length() - 2) {
				word = word.substring(0, apostr);
			}
			if (stemmer != null) {
				word = stemmer.stem(word);
			}
			result += word + " ";
		}
		return result.trim();
	}


	public void setVocabularyName(String vocabularyName) {
		this.vocabularyName = vocabularyName;	
	}

	public class VocabularyException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public VocabularyException(String message) {
			super(message);
		}
	}

	public double getGenerality(String id) {
		// TODO: insert generality method using SPARQL query
		return 0.0;
	}
}
