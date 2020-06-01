package com.entopix.maui.vocab;

import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.ObjectInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author nathanholmberg
 */
public class VocabularyStore_HT extends VocabularyStore implements Externalizable {

	private String conceptURIPrefix = null;
	/** reverse index : id --> descriptor */
	private HashMap<Integer, String> idTermIndex;
	/** normalized descriptor --> list of all possible meanings */
	private HashMap<String, ArrayList<Integer>> listsOfSenses;
	/** non-descriptor id  --> descriptors id */
	private HashMap<Integer, Integer> nonDescriptorIndex = null;
	/** id -->  list of related ids */
	private HashMap<Integer, ArrayList<Integer>> listsOfRelatedTerms = null;
	private int currentID = 0;
	private HashMap<String, Integer> URItoIDMap = null;
	private HashMap<Integer, String> IDtoURIMap = null;


	public long CantorPairingFunction(int n1, int n2) {
		// from http://en.wikipedia.org/wiki/Cantor_pairing_function
		return (long) (0.5 * (n1 + n2) * (n1 + n2 + 1) + n2);
	}

	public int createIDFromURI(String in) {
		String prefix_current = in.substring(0, in.lastIndexOf('/') + 1);
		if (conceptURIPrefix == null) {
			conceptURIPrefix = prefix_current;
			in = in.substring(prefix_current.length());
		} else if (prefix_current.equals(conceptURIPrefix)) {
			in = in.substring(prefix_current.length());
		} else {
			in = "<" + in + ">";
			// log.info("Not saving anything by cutting off prefix?");
		}

		if (URItoIDMap == null) {
			URItoIDMap = new HashMap<String, Integer>();
			IDtoURIMap = new HashMap<Integer, String>();
		}

		// now that we have a normalized string, add if necessary
		if (URItoIDMap.get(in) == null) {
			IDtoURIMap.put(currentID, in);
			URItoIDMap.put(in, currentID);
			currentID++;
		}

		return URItoIDMap.get(in);
	}

	public String createURIFromID(Integer id) {
		String localName = IDtoURIMap.get(id);
		if (localName.startsWith("<")) {
			return localName.substring(1, localName.length()-1);
		}
		return conceptURIPrefix + localName;
	}

	public VocabularyStore_HT() {

		idTermIndex = new HashMap<Integer, String>();
		listsOfSenses = new HashMap<String, ArrayList<Integer>>();

		nonDescriptorIndex = new HashMap<Integer, Integer>();
		listsOfRelatedTerms = new HashMap<Integer, ArrayList<Integer>>();
	}

	public void addSense(String descriptor, String id_string) {
		int id = createIDFromURI(id_string);

		ArrayList<Integer> ids = listsOfSenses.get(descriptor);
		if (ids == null) {
			ids = new ArrayList<Integer>();
		}
		ids.add(id);
		listsOfSenses.put(descriptor, ids);
	}

	public void addDescriptor(String id_string, String descriptor) {
		int id = createIDFromURI(id_string);

		idTermIndex.put(id, descriptor);
	}

	public void addNonDescriptor(String id_string, String nonDescriptor) {
		int id = createIDFromURI(id_string);
		int desc_id = createIDFromURI(nonDescriptor);
		nonDescriptorIndex.put(id, desc_id);
	}

	public void addRelatedTerm(String term, String relatedTerm) {
		int term_id = createIDFromURI(term);

		ArrayList<Integer> related_terms = listsOfRelatedTerms.get(term_id);
		if (related_terms == null) {
			related_terms = new ArrayList<Integer>();
		}
		int idf = createIDFromURI(relatedTerm);
		if (!related_terms.contains(idf))
			related_terms.add(idf);
		listsOfRelatedTerms.put(term_id, related_terms);
	}

	public int getNumTerms() {
		return idTermIndex.size();
	}

	public int getNumNonDescriptors() {
		return nonDescriptorIndex.size();
	}

	public int getNumRelatedTerms() {
		return listsOfRelatedTerms.size();
	}

	public ArrayList<String> getRelatedTerms(String id) {
		int term_id = createIDFromURI(id);

		ArrayList<String> results = null;

		ArrayList<Integer> int_terms = listsOfRelatedTerms.get(term_id);
		if (int_terms != null) {
			results = new ArrayList<String>(int_terms.size());
			for (int i = 0; i < int_terms.size(); i++) {
				results.add(createURIFromID(int_terms.get(i)));
			}
		}

		return results;
	}

	public int getNumSenses(String sense) {
		ArrayList<Integer> meanings = listsOfSenses.get(sense);
		if (meanings != null) {
			return meanings.size();
		}
		return 0;
	}

	public String getTerm(String id) {
		int term_id = createIDFromURI(id);
		return idTermIndex.get(term_id);
	}

	public ArrayList<String> getSensesForPhrase(String phrase) {

		ArrayList<Integer> senses = new ArrayList<Integer>();
		if (listsOfSenses.containsKey(phrase)) {
			ArrayList<Integer> list = listsOfSenses.get(phrase);
			for (Integer senseId : list) {
				// 1. retrieve a descriptor if this sense is a non-descriptor
				if (nonDescriptorIndex.containsKey(senseId)) {
					senseId = nonDescriptorIndex.get(senseId);
				}
				if (!idTermIndex.containsKey(senseId)) {
					continue;
				}
				if (!senses.contains(senseId)) {
					senses.add(senseId);
				}
			}
		}

		ArrayList<String> results = new ArrayList<String>(senses.size());
		for (int i = 0; i < senses.size(); i++) {
			results.add(createURIFromID(senses.get(i)));
		}

		return results;
	}

	public void writeExternal(ObjectOutput out) throws java.io.IOException {
		// Write non-hashmap objects
		out.writeUTF(conceptURIPrefix);

		/** reverse index : id --> descriptor */
		out.writeInt(idTermIndex.size());

		for (Map.Entry<Integer, String> e : idTermIndex.entrySet()) {
			out.writeInt(e.getKey());
			out.writeUTF(e.getValue());
		}


		/** normalized descriptor --> list of all possible meanings */
		out.writeInt(listsOfSenses.size());

		for (Map.Entry<String, ArrayList<Integer>> e : listsOfSenses.entrySet()) {
			out.writeUTF(e.getKey());
			ArrayList<Integer> senses = e.getValue();
			out.writeInt(senses.size());
			for (int i = 0; i < senses.size(); i++) {
				out.writeInt(senses.get(i));
			}
		}

		/** non-descriptor id  --> descriptors id */
		out.writeInt(nonDescriptorIndex.size());

		for (Map.Entry<Integer, Integer> e : nonDescriptorIndex.entrySet()) {
			out.writeInt(e.getKey());
			out.writeInt(e.getValue());
		}

		/** id -->  list of related ids */
		out.writeInt(listsOfRelatedTerms.size());

		for (Map.Entry<Integer, ArrayList<Integer>> e : listsOfRelatedTerms.entrySet()) {
			out.writeInt(e.getKey());
			ArrayList<Integer> relatedTerms = e.getValue();
			out.writeInt(relatedTerms.size());
			for (int i = 0; i < relatedTerms.size(); i++) {
				out.writeInt(relatedTerms.get(i));
			}
		}

		out.writeInt(IDtoURIMap.size());

		for (Map.Entry<Integer, String> e : IDtoURIMap.entrySet()) {
			out.writeInt(e.getKey());
			out.writeUTF(e.getValue());
		}


	}

	public void readExternal(ObjectInput in) throws java.io.IOException, ClassNotFoundException {

		conceptURIPrefix = in.readUTF();

		int size = 0;
		/** reverse index : id --> descriptor */
		size = in.readInt();
		idTermIndex = new HashMap<Integer, String>();
		for (int i = 0; i < size; i++) {
			idTermIndex.put(in.readInt(), in.readUTF());
		}


		/** normalized descriptor --> list of all possible meanings */
		size = in.readInt();
		listsOfSenses = new HashMap<String, ArrayList<Integer>>();

		for (int i = 0; i < size; i++) {
			String sense = in.readUTF();
			int size_2 = in.readInt();
			ArrayList<Integer> senses = new ArrayList<Integer>();
			for (int j = 0; j < size_2; j++) {
				senses.add(in.readInt());
			}
			listsOfSenses.put(sense, senses);
		}

		/** non-descriptor id  --> descriptors id */
		size = in.readInt();

		nonDescriptorIndex = new HashMap<Integer, Integer>();
		for (int i = 0; i < size; i++) {
			nonDescriptorIndex.put(in.readInt(), in.readInt());
		}

		/** id -->  list of related ids */
		size = in.readInt();

		listsOfRelatedTerms = new HashMap<Integer, ArrayList<Integer>>();
		for (int i = 0; i < size; i++) {
			int term = in.readInt();
			int size_2 = in.readInt();
			ArrayList<Integer> relations = new ArrayList<Integer>();
			for (int j = 0; j < size_2; j++) {
				relations.add(in.readInt());
			}
			listsOfRelatedTerms.put(term, relations);
		}

		size = in.readInt();

		IDtoURIMap = new HashMap<Integer, String>();
		URItoIDMap = new HashMap<String, Integer>();
		for (int i = 0; i < size; i++) {
			int id = in.readInt();
			String name = in.readUTF();
			IDtoURIMap.put(id, name);
			URItoIDMap.put(name, id);
		}

		finishedInitialized();
	}
}
