/*  Copyright (C) <2013>  University of Massachusetts Amherst

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 *
 * @author Ismet Zeki Yalniz
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class TermIndexBuilder {

    String originalText = null;
    String[] tokens = null;
    HashMap<String, IndexEntry> index = null;
    TextPreprocessor tp = null;

    public TermIndexBuilder(String filename, TextPreprocessor tp) {
        this.tp = tp;
        originalText = TextPreprocessor.readFile(filename);
        originalText = tp.processText(originalText);
        tokens = originalText.split("\\s+");
    }
    

    public int getOriginalTextLength() {
        return originalText.length();
    }

    public double intersect_vocabularies(HashMap<String, IndexEntry> h) {
        Collection<IndexEntry> col = index.values();
        Iterator<IndexEntry> iter = col.iterator();
        long total = 0;
        long mintotal = 0;
        while (iter.hasNext()) {
            IndexEntry ent = iter.next();
            if (ent.getTerm().equals("")) {
                continue;
            }
            IndexEntry result = h.get(ent.getTerm());
            if (result != null) {
                mintotal += Math.min(ent.getFrequency(), result.getFrequency());
            }
            total += ent.getFrequency();
        }
        return ((double) mintotal / total);
    }

    // including startIndex, excluding endIndex
    public HashMap<String, IndexEntry> indexTerms(int startIndex, int endIndex) {

        index = new HashMap<String, IndexEntry>(endIndex - startIndex);

        for (int i = startIndex; i < endIndex; i++) {
            String s = tokens[i];

            if (!s.equals("")) {
                if (index.containsKey(s)) {
                    IndexEntry e = index.get(s);
                    e.incrementFre();
                } else {
                    index.put(s, new IndexEntry(s, 1, i, 1));
                }
            }
        }
        return index;
    }

    public static HashMap<String, IndexEntry> findUniqueTerms(HashMap<String, IndexEntry> terms) {
        HashMap<String, IndexEntry> uniqueTerms = new HashMap<String, IndexEntry>();

        Collection<IndexEntry> col = terms.values();
        Iterator<IndexEntry> iter = col.iterator();

        while (iter.hasNext()) {
            IndexEntry ent = iter.next();

            // enforce the term to be unique
            // in order to avoid stop words put a size contraint on the length of words to be selected
            if (ent.getFrequency() == 1 && ent.getTerm().length() > 3) {
                uniqueTerms.put(ent.getTerm(), ent);
            }
        }

        //  Comparator<IndexEntry> comparator = new TermPosComparator();
        //  IndexEntry sorteduniqueterms[] =  uniqueTerms.values().toArray(new IndexEntry[0]);
        //  Arrays.sort(sorteduniqueterms, comparator);
        return uniqueTerms;
    }

    public static ArrayList<IndexEntry> countStopWords(HashMap<String, IndexEntry> terms, String[] stopwords) {
        ArrayList<IndexEntry> stopTerms = new ArrayList<IndexEntry>(stopwords.length);

        for (int i = 0; i < stopwords.length; i++) {
            String curWord = stopwords[i];
            IndexEntry ent = terms.get(curWord);
            if (ent != null) {
                stopTerms.add(ent);
            } else {
                stopTerms.add(new IndexEntry("", 0, 0, 1));
            }
        }
        return stopTerms;
    }

    public static int[] countTermsBasedOnRank(HashMap<String, IndexEntry> ind, int MAX_RANK) {
        int result[] = new int[MAX_RANK + 1];

        Collection<IndexEntry> col = ind.values();
        Iterator<IndexEntry> it = col.iterator();
        while (it.hasNext()) {
            IndexEntry ent = it.next();
            int fre = (int) ent.getFrequency();
            result[fre]++;
        }
        return result;
    }

    public static int[][] countTermsBasedOnRank(HashMap<String, IndexEntry> ind, HashMap<String, IndexEntry> ind2,
            int MAX_RANK) {
        int result[][] = new int[MAX_RANK + 1][3];

        Collection<IndexEntry> col = ind.values();
        Iterator<IndexEntry> it = col.iterator();
        while (it.hasNext()) {
            IndexEntry ent = it.next();
            int fre = (int) ent.getFrequency();
            if (fre > MAX_RANK) {
                continue;
            }

            result[fre][0]++;
            IndexEntry found = ind2.get(ent.getTerm());
            if (found != null) {
                int fre2 = (int) found.getFrequency();
                if (fre == fre2) {
                    result[fre][2]++;
                }
            }
        }
        col = ind2.values();
        it = col.iterator();
        while (it.hasNext()) {
            IndexEntry ent = it.next();
            int fre = (int) ent.getFrequency();
            if (fre > MAX_RANK) {
                continue;
            }
            result[fre][1]++;
        }
        return result;
    }

    public static void outputVocabulary(HashMap<String, IndexEntry> ind, String filename) {
        if (ind == null) {
            System.out.println("TermIndexBuilder.outputVocabulary(): input hashmap can not be null. Skipping");
            return;
        }

        FileWriter writer = null;
        try {
            Collection<IndexEntry> vocab = ind.values();
            Comparator<IndexEntry> comparator = new IndexTermComparator();
            IndexEntry sortedVocab[] = vocab.toArray(new IndexEntry[0]);
            Arrays.sort(sortedVocab, comparator);
            File file = new File(filename);
            writer = new FileWriter(file, false);
            for (int mm = 0; mm < sortedVocab.length; mm++) {
                IndexEntry term = sortedVocab[mm];
                writer.append(term.getTerm() + "\t" + term.getFrequency() + "\n");
            }
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public TextPreprocessor getTextPreprocessor() {
        return tp;
    }

    public String[] getTokens() {
        return tokens;
    }

    public int getNumOfTokens() {
        return tokens.length;
    }

    public HashMap<String, IndexEntry> getIndex() {
        return index;
    }

    public static void writeRareWordsInFile(TextPreprocessor tp, String inputFile, String outputFile, int max_frequency) {
        FileWriter writer = null;
        try {
            String text = TextPreprocessor.readFile(inputFile);
            text = tp.processText(text).toLowerCase();
            String[] tokens = text.split("\\s+");
            HashMap<String, IndexEntry> index = new HashMap<String, IndexEntry>();
            // find rare words
            for (int j = 0; j < tokens.length; j++) {
                String s = tokens[j];
                if (index.containsKey(s)) {
                    IndexEntry e = index.get(s);
                    e.incrementFre();
                } else {
                    index.put(s, new IndexEntry(s, 1, j, 1));
                }
            }
            writer = new FileWriter(new File(outputFile));
            for (int i = 0; i < tokens.length; i++) {
                IndexEntry e = index.get(tokens[i]);
                if (e.getFrequency() <= max_frequency) {
                    writer.append(tokens[i] + "\t" + i + "\n");
                }
            }
            writer.close();
        } catch (IOException ex) {
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
            }
        }

    }

    public static ArrayList<IndexEntry> getRareWords(TextPreprocessor tp, String inputFile, int max_frequency) {
        long ss = System.nanoTime();
        ArrayList<IndexEntry> results = new ArrayList<IndexEntry>();
        String text = TextPreprocessor.readFile(inputFile);
        text = tp.processText(text).toLowerCase();
        String[] tokens = text.split("\\s+");
        HashMap<String, IndexEntry> index = new HashMap<String, IndexEntry>();
        // find rare words
        for (int j = 0; j < tokens.length; j++) {
            String s = tokens[j];
            if (index.containsKey(s)) {
                IndexEntry e = index.get(s);
                e.incrementFre();
            } else {
                index.put(s, new IndexEntry(s, 1, j, 1));
            }
        }
        for (int i = 0; i < tokens.length; i++) {
            IndexEntry e = index.get(tokens[i]);
            if (e.getFrequency() <= max_frequency) {
                results.add(e);
            }
        }
        System.out.println(System.nanoTime() - ss);
        return results;
    }
}
