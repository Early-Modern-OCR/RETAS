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
/*
 RECURSIVE ALIGNMENT TOOL for OCR Evaluation
 Version 1.1
 
 @author Ismet Zeki Yalniz
 University of Massachusetts-Amherst
 zeki@cs.umass.edu       
 Modified: MichaelZ@cs.umass.edu
   
 If you make use of this alignment tool, please cite the following paper: 
 - I. Zeki Yalniz, R. Manmatha: A Fast Alignment Scheme for Automatic OCR Evaluation of Books. ICDAR 2011: 754-758
  
 About the code:
 This is just a prototype. The code can be speeded up further in several ways as discussed below. 
 1 - Currently String's equals() method is being called for comparing words and characters for the alignment. HashMap<String> is used for term indexing and finding the unique terms. Assigning an integer ID for each distinct term in the word sequence and using the resulting integers for finding unique words and aligning them might work faster.   
 2 - The recursion is implemented in an iterative way with a stack represented by an ArrayList. Converting it to a LinkedList might improve the speed. 
 3 - Character level alignment is performed by creating a String array of individual characters. Instead, the alignment can be performed directly at the level of characters for further speed.
 4 - There are several constants MAX_SEGMENT_LENGTH, MAX_DYNAMIC_TABLE_SIZE and MAX_NUMBER_OF_CANDIDATE_ANCHORS. Using a different configuration may help improve the alignment speed. The default (recommended) values are given below in the code.
 5 - The "edit distance" alignment code (params: insCost = delCost = 1, repCost = 2) uses the conventional dynamic programming algorithm. It uses O(n^2) space. One can implement an O(n) space version using a binary recursion, however, this would work two times slower. Therefore we use the original algorithm with a limit on the maximum size of the dynamic programming table (MAX_DYNAMIC_TABLE_SIZE). Using larger tables makes the computation take longer and can cause out-of-memory errors.
 6 - MAX_SEGMENT_LENGTH specifies the base condition for recursion. If the text segments become shorter than this threshold, than the recursion is terminated and the alignment is carried out using a dynamic programming approach. Notice that MAX_DYNAMIC_TABLE_SIZE and MAX_SEGMENT_LENGTH are correlated.  
 7 - MAX_NUMBER_OF_CANDIDATE_ANCHORS limits the total number of common unique words used for LCS alignment at the coarse level. The first MAX_NUMBER_OF_CANDIDATE_ANCHORS of them are used for splitting the text into shorter pieces. The last segment is therefore expected to be larger if the text is long and accomodates a larger number of unique words. This help improve the speed by avoiding the LCS computation for long sequences. Larger values may help produce more accurate alignments. Preliminary experiments on 1261 scanned book pairs suggest that the total number of matching chars/words is not significantly effected by varying this constant between 100 and 20000 (the change is less than 0.01%). But the speed improvement is drastical for long noisy texts.
 8 - The TextPreprocessor merges hypenated words before the alignment. If this is not necessary, one could skip the text preprocessing step to improve the speed. The bottleneck is the I/O time. 
 9 - Please see the comments in the code for other specifications. 
  
 NOTE: The alignment is case sensitive. One can obtain a case-insensitive alignment by preprocessing the input documents accordingly. 
  
 GOOD LUCK! 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class RecursiveAlignmentTool {

    private static long MAX_SEGMENT_LENGTH = 400; // the maximum size for a text segment for the recursion. If the segment is shorter than this specified length (in terms of words) then the text is aligned directly using dynamic programming at the leaf level of the recursion. 
    private static long MAX_DYNAMIC_TABLE_SIZE = 2000000; // run dynamic programming for aligning text segments if the dynamic programming table size is smaller than MAX_DYNAMIC_TABLE_SIZE. Otherwise align the sequences with nulls. 
    private static int MAX_NUMBER_OF_CANDIDATE_ANCHORS = 1000; // used for avoiding outofmemory errors for very large texts with large number of unique words.
    private static final String WORD_BOUNDARY = "---";
    // DEFAULT SETTINGS
    public static boolean WORD_LEVEL_ALIGNMENT = true;
    public static String OUTPUT_FORMAT = "COLS";
    public static String numericCharSequence = "1234567890"; // if a unique word contains a numeric letter, it is not used as an anchor for dividing the text into pieces. Because page numbers are typically unique in the text. 
    TermIndexBuilder refIndex;
    TermIndexBuilder ocrIndex;
    ArrayList<IndexEntry> anchorsRef;
    ArrayList<IndexEntry> anchorsCand;
    ArrayList<AlignedSequence> alignment;
    String language = "";
    String groundTruthFile = "";
    String ocrFile = "";

    public RecursiveAlignmentTool(String groundTruthFile, String ocrFile, TextPreprocessor tp) {

        this.groundTruthFile = groundTruthFile;
        this.ocrFile = ocrFile;

        // create indices for these text files
        refIndex = new TermIndexBuilder(groundTruthFile, tp);
        ocrIndex = new TermIndexBuilder(ocrFile, tp);
    }

    // TODO: use linked list in place of arraylist. It may get faster.
    private void findAnchorWords() {

        // intersect unique words in the ground truth and ocr output
        anchorsRef = new ArrayList<IndexEntry>(MAX_NUMBER_OF_CANDIDATE_ANCHORS);
        anchorsCand = new ArrayList<IndexEntry>(MAX_NUMBER_OF_CANDIDATE_ANCHORS);
        ArrayList<IndexEntry> newAnchorsRef = new ArrayList<IndexEntry>(MAX_NUMBER_OF_CANDIDATE_ANCHORS);
        ArrayList<IndexEntry> newAnchorsCand = new ArrayList<IndexEntry>(MAX_NUMBER_OF_CANDIDATE_ANCHORS);

        int i = 0;
        int startRef, endRef, startCand, endCand;
        boolean start = true;

        do {
            if (start) { // initial condition: input sequences are considered to be two big segments.
                startRef = 0;
                endRef = refIndex.getNumOfTokens();
                startCand = 0;
                endCand = ocrIndex.getNumOfTokens();
                i--; // not to skip the first subsegment in the list
                start = false;
            } else {

                // if the first iteration could not find any anchor words, then simply quit.
                if (anchorsRef.isEmpty()) {
                    break;
                }

                // RECURSIVE STAGE
                // INDEX SUB-SEGMENTS FOR MORE ANCHOR WORDS               
                if (i == 0) { // first segment
                    startRef = 0;
                    endRef = anchorsRef.get(i).getPosInt();
                    startCand = 0;
                    endCand = anchorsCand.get(i).getPosInt();
                }// last segment
                else if (i == anchorsRef.size()) {
                    startRef = anchorsRef.get(i - 1).getPosInt() + 1;
                    endRef = refIndex.getNumOfTokens();
                    startCand = anchorsCand.get(i - 1).getPosInt() + 1;
                    endCand = ocrIndex.getNumOfTokens();
                } else { // inbetween segments
                    startRef = anchorsRef.get(i - 1).getPosInt() + 1;
                    endRef = anchorsRef.get(i).getPosInt();
                    startCand = anchorsCand.get(i - 1).getPosInt() + 1;
                    endCand = anchorsCand.get(i).getPosInt();
                }
            }

            // if one of the segments is still large, go on recursive operations.
            if (((long) (endRef - startRef)) > MAX_SEGMENT_LENGTH || ((long) (endCand - startCand)) > MAX_SEGMENT_LENGTH) {
                newAnchorsRef.clear();
                newAnchorsCand.clear();
                findCommonUniqueWords(startRef, endRef, startCand, endCand, newAnchorsRef, newAnchorsCand);

                // if there are no common unique words, then do not try to chop it down
                if (newAnchorsRef.isEmpty()) {
                    if (i < anchorsRef.size()) {
                        i++; //skip over the segment
                        continue;
                    } else {
                        break; // if it is the last segment, quit anchor word search
                    }
                }
            } else { // if the segment is small enough to use edit distance based alignment, then skip over
                if (i < anchorsRef.size()) {
                    i++;
                    continue;
                } else {
                    break; //  if it is the last segment, quit anchor word search
                }
            }

            int LCSindices[] = LCS.findLCS(newAnchorsRef, newAnchorsCand);
            int lcsLength = LCSindices.length / 2;

            // use achors if and only if they are in the longest common subsequence
            ArrayList<IndexEntry> newAnchorsRefRefined = new ArrayList<IndexEntry>();
            ArrayList<IndexEntry> newAnchorsCandRefined = new ArrayList<IndexEntry>();

            for (int kk = 0; kk < lcsLength; kk++) {
                IndexEntry refEntry = newAnchorsRef.get(LCSindices[kk]);
                IndexEntry candEntry = newAnchorsCand.get(LCSindices[(kk + lcsLength)]);

                newAnchorsRefRefined.add(refEntry);
                newAnchorsCandRefined.add(candEntry);
            }
            newAnchorsRef = newAnchorsRefRefined;
            newAnchorsCand = newAnchorsCandRefined;

            if (lcsLength > 0) {
                if (i == -1) { // for the first iteration
                    anchorsRef.addAll(newAnchorsRef);
                    anchorsCand.addAll(newAnchorsCand);
                } else {
                    anchorsRef.addAll(i, newAnchorsRef);
                    anchorsCand.addAll(i, newAnchorsCand);
                    i--;
                }
            }
            i++;
            // break; // test - stop recursion at the first stage
        } while (i <= anchorsRef.size());
    }

    public ArrayList<AlignedSequence> align() {

        if (alignment == null) {
            alignment = new ArrayList<AlignedSequence>(refIndex.getNumOfTokens() + ocrIndex.getNumOfTokens());
        } else {
            alignment.clear();
        }

        findAnchorWords();

        wordAlignSubsequences();
        //checkWordLevelAlignment(); // for debugging purposes   

        if (!WORD_LEVEL_ALIGNMENT) {
            charAlignSubsequences();
            //checkCharLevelAlignment(); // for debugging purposes   
        }

        return alignment;
    }

    // for debugging purposes
    private void checkWordLevelAlignment() {
        String[] refTokens = refIndex.getTokens();
        String[] candTokens = ocrIndex.getTokens();
        int curIndexRef = 0;
        int curIndexCand = 0;

        for (int i = 0; i < alignment.size(); i++) {
            AlignedSequence as = alignment.get(i);
            if (as.m_candidate != null) {
                if (as.m_candidate.equals(candTokens[curIndexCand])) {
                    curIndexCand++;
                } else {
                    System.out.println("checking word alignment - alignment error");
                }
            }
            if (as.m_reference != null) {
                if (as.m_reference.equals(refTokens[curIndexRef])) {
                    curIndexRef++;
                } else {
                    System.out.println("checking word alignment - alignment error");
                }
            }
        }

        if (refTokens.length != curIndexRef || candTokens.length != curIndexCand) {
            System.out.println("checking word alignment - alignment error");
        }

        System.out.println("Word level check successful " + refTokens.length + "\t" + candTokens.length + "\t" + alignment.size());
    }

    // for debugging purposes
    private void checkCharLevelAlignment() {

        System.out.println("Checking character level alignment...");
        // strip out all the spaces from the original text. During the character level alignment, each
        // word has a space added after it, which makes things
        // a tiny bit more difficult here if we don't remove the spaces. 
        // For example if you had a referece work with the text "Max the cat", 
        // and an OCR text of just "x", it would align as:
        // OCR:	@@x @@@@@@@
        // GT :	Max the cat
        // if we kept the spaces in the original text we'd have to keep track that
        // the "x" in the OCR text is the last token and we can ignore the space
        // afer it which does not exist in the original text since the alignment added it.
        // Since we've already verified the word level alignment, and that's what the
        // character level alignment uses as input, it's safe to ignore spaces. 

        final String refText = refIndex.originalText.replaceAll("\\s+", "");
        final String candText = ocrIndex.originalText.replaceAll("\\s+", "");
        int curIndexRef = 0;
        int curIndexCand = 0;

        for (int i = 0; i < alignment.size(); i++) {
            AlignedSequence as = alignment.get(i);

            if (as.m_candidate != null && !as.m_candidate.equals(" ")) {

                if (as.m_candidate.charAt(0) != candText.charAt(curIndexCand)) {
                    System.out.println("checking candidate character alignment - alignment error");
                }
                curIndexCand++;
            }
            if (as.m_reference != null && !as.m_reference.equals(" ")) {

                if (as.m_reference.charAt(0) != refText.charAt(curIndexRef)) {
                    System.out.println("checking reference character alignment - alignment error");
                }
                curIndexRef++;
            }
        } // end loop through alignment

        if (curIndexRef != refText.length() || curIndexCand != candText.length()) {
            System.out.println("Problem with character level algnment. Reference had "
                    + curIndexRef + " characters, expected: " + refText.length()
                    + ". Candidate had " + curIndexCand + " characters, expected: " + candText.length());
        } else {
            System.out.println("Alignment is OK");
        }
        System.out.println("Done character level check.");

    }

    public Stats[] estimateWordAndCharacterOCRaccuracies() {

        if (alignment == null) {
            alignment = new ArrayList<AlignedSequence>(refIndex.getNumOfTokens() + ocrIndex.getNumOfTokens());
        } else {
            alignment.clear();
        }
        Stats[] sts = new Stats[2];

        findAnchorWords();
        wordAlignSubsequences();
        sts[0] = this.calculateOCRaccuracy();

        charAlignSubsequences();
        sts[1] = this.calculateOCRaccuracy();

        return sts;
    }

    // STAGE 2: based on word level alignment, align all the characters
    // Michael Z    7/2013   Modified to avoid ArrayIndexOutOfBoundsException when there
    // are long streteches without an anchor word. To avoid that, we only build the "accumulator"
    // array when we need it for the alignment and we're assured all the words will fit in the array.
    private void charAlignSubsequences() {
        int EXPECTED_WORD_LENGTH_IN_CHARACTERS = 5; // for memory pre-allocation

        ArrayList<AlignedSequence> alignment2 = new ArrayList<AlignedSequence>(alignment.size() * EXPECTED_WORD_LENGTH_IN_CHARACTERS);
        EditDistAligner aligner = new EditDistAligner();

        String candAccu[] = new String[(int) MAX_DYNAMIC_TABLE_SIZE];
        String refAccu[] = new String[(int) MAX_DYNAMIC_TABLE_SIZE];
        int candAccuSize = 0;
        int refAccuSize = 0;
        int refStartAnchorIdx = -1;
        int candStartAnchorIdx = -1;
        String ch;

        String cand, ref;
        for (int i = 0; i <= alignment.size(); i++) {

            if (alignment.size() == i) { // dont forget the last text segment
                cand = "";
                ref = "";
            } else {
                AlignedSequence cur = alignment.get(i);
                cand = cur.m_candidate;
                ref = cur.m_reference;
            }

            // save the index of the first "anchor" so we
            // know where to start the alignment below
            if (cand != null && candStartAnchorIdx == -1) {
                candStartAnchorIdx = i;
            }
            if (ref != null && refStartAnchorIdx == -1) {
                refStartAnchorIdx = i;
            }

            // if either is null, skip the alignment step, we only align
            // when the reference word is the same as the candidate word.
            if (ref == null || cand == null) {
                // only one of the words can be null at a time.
                if (cand != null) {
                    candAccuSize += cand.length() + 1;// +1 for the space after the word
                } else { // ref != null
                    refAccuSize += ref.length() + 1;
                }
                continue;
            }

            // if we got here, we have two words
            if (!cand.equals(ref)) {
                // words don't match, increment the character accumulators
                // and skip trying to align until the words match
                refAccuSize += ref.length() + 1; // +1 for the space after the word
                candAccuSize += cand.length() + 1;
                continue;
            }

            // we only do an alignment if it's small enough to do efficiently  
            boolean blnUseDynamicAlignment = ((long) refAccuSize * (long) candAccuSize < MAX_DYNAMIC_TABLE_SIZE) && refAccuSize != 0 && candAccuSize != 0;

            // build the array to pass to the aligner or just null align
            int tmpAccuSize = 0;
            for (int kk = refStartAnchorIdx; kk < i; kk++) {
                if (alignment.get(kk).m_reference == null) {
                    continue; // only process non-null words
                }
                String tmpWord = alignment.get(kk).m_reference + " "; // add space at end of word
                for (int charIdx = 0; charIdx < tmpWord.length(); charIdx++) {
                    if (blnUseDynamicAlignment) {
                        refAccu[tmpAccuSize++] = Character.toString(tmpWord.charAt(charIdx));
                    } else {
                        alignment2.add(new AlignedSequence(tmpWord.charAt(charIdx), null));
                    }
                }
            }

            tmpAccuSize = 0;
            for (int kk = candStartAnchorIdx; kk < i; kk++) {
                if (alignment.get(kk).m_candidate == null) {
                    continue;
                }
                String tmpWord = alignment.get(kk).m_candidate + " "; // add space at end of word
                for (int charIdx = 0; charIdx < tmpWord.length(); charIdx++) {
                    if (blnUseDynamicAlignment) {
                        candAccu[tmpAccuSize++] = Character.toString(tmpWord.charAt(charIdx));
                    } else {
                        alignment2.add(new AlignedSequence(null, tmpWord.charAt(charIdx)));
                    }
                }
            }

            // add it to the resulting sequence if    
            if (blnUseDynamicAlignment) {
                ArrayList<AlignedSequence> sq = aligner.align(refAccu, candAccu, 0, refAccuSize, 0, candAccuSize);
                alignment2.addAll(sq);
            }

            // we know that current words align
            for (int j = 0; j < ref.length(); j++) {
                String cha = ref.substring(j, j + 1);
                alignment2.add(new AlignedSequence(cha, cha));
            }
            alignment2.add(new AlignedSequence(" ", " "));

            // reset accumulators & anchor indexes
            refAccuSize = 0;
            candAccuSize = 0;
            refStartAnchorIdx = -1;
            candStartAnchorIdx = -1;

        } // end loop through the word level alignment

        // we always add a space after a word, and the last run through uses
        // an empty string for both the cand and ref word to make sure we "fall through"
        // to the logic and process any words still waiting to be aligned. 
        // Now that we're done, remove the last two alignments. Without this, there
        // are extra spaces in the alignment that don't exist in the original works and
        // a sanity check such as checkCharLevelAlignment() will fail. 
        alignment2.remove(alignment2.size() - 1);
        alignment2.remove(alignment2.size() - 1);
        alignment = alignment2;
    }

    // STAGE 1 : apply word level alignment for each segment
    private void wordAlignSubsequences() {

        String[] refTokens = refIndex.getTokens();
        String[] candTokens = ocrIndex.getTokens();
        EditDistAligner aligner = new EditDistAligner();

        int startRef = 0, endRef = 0, startCand = 0, endCand = 0;

        // now align each segment using dynamic programming
        for (int j = 0; j <= anchorsRef.size(); j++) {

            // copy segment
            if (j == anchorsRef.size()) {
                endRef = refIndex.getNumOfTokens();
                endCand = ocrIndex.getNumOfTokens();
            } else {
                endRef = anchorsRef.get(j).getPosInt();
                endCand = anchorsCand.get(j).getPosInt();
            }

            // if the segment is larger than the largest segment size, then do not align the sequence
            long dynamicTableSize = ((long) (endRef - startRef) * (long) (endCand - startCand));

            if (dynamicTableSize != 0 && dynamicTableSize <= MAX_DYNAMIC_TABLE_SIZE) {
                // run dynamic programming                
                ArrayList<AlignedSequence> sq = aligner.align(refTokens, candTokens, startRef, endRef, startCand, endCand);
                alignment.addAll(sq);

            } else {
                for (int ls = startRef; ls < endRef; ls++) {
                    alignment.add(new AlignedSequence(refTokens[ls], null));
                }
                for (int ls = startCand; ls < endCand; ls++) {
                    alignment.add(new AlignedSequence(null, candTokens[ls]));
                }
            }

            startRef = endRef;
            startCand = endCand;
        }
    }

    private String[] copySegment(String[] tokens, int start, int end, boolean WORD_LEVEL) {

        String[] out;
        if (end > tokens.length) {
            System.out.println("RecursiveAlignmentTool: check failed (end > tokens.length)");
            end = tokens.length;
        }
        if (end < start) {
            System.out.println("RecursiveAlignmentTool: check failed (end < start)");
            end = start;
        }

        if (WORD_LEVEL) {
            out = new String[end - start];
            for (int i = start, j = 0; i < end; i++, j++) {
                out[j] = tokens[i];
            }
        } else {

            int count = 0;
            for (int i = start; i < end; i++) {
                count += tokens[i].length();
            }
            count += (end - start); // allocate place for word boundaries

            out = new String[count];
            count = 0;
            for (int i = start; i < end; i++) {
                String cur = tokens[i];

                for (int j = 0; j < cur.length(); j++) {
                    out[count] = cur.substring(j, j + 1);
                    count++;
                }
                out[count] = WORD_BOUNDARY;
                count++;
            }
        }
        return out;
    }

    public void findCommonUniqueWords(int refStart, int refEnd, int ocrStart, int ocrEnd,
            ArrayList<IndexEntry> anchors1, ArrayList<IndexEntry> anchors2) {

        if (anchors1 == null || anchors2 == null) {
            System.out.println("findAnchorWordsSorted: arguments anchors1 or 2 can not be null");
        }

        // count words in designated segments
        HashMap<String, IndexEntry> refVocab = refIndex.indexTerms(refStart, refEnd);
        HashMap<String, IndexEntry> candVocab = ocrIndex.indexTerms(ocrStart, ocrEnd);

        // determine unique terms
        HashMap<String, IndexEntry> refUniqueTerms = TermIndexBuilder.findUniqueTerms(refVocab);
        HashMap<String, IndexEntry> candUniqueTerms = TermIndexBuilder.findUniqueTerms(candVocab);

        // find common unique words
        Collection<IndexEntry> col = refUniqueTerms.values();
        IndexEntry[] uniqueTerms = col.toArray(new IndexEntry[0]);
        for (int i = 0; i < uniqueTerms.length && anchors1.size() <= MAX_NUMBER_OF_CANDIDATE_ANCHORS; i++) {
            IndexEntry ent1 = uniqueTerms[i];
            IndexEntry ent2 = candUniqueTerms.get(ent1.getTerm());
            if (ent2 != null && isValidAnchor(ent1.getTerm())) {
                anchors1.add(ent1);
                anchors2.add(ent2);
            }
        }
        /*      if ( REPORT ){
         System.out.println(groundTruthFile + "\t" + ocrFile + "\t" + refIndex.getNumOfTokens() + "\t" + ocrIndex.getNumOfTokens() +  "\t" + refUniqueTerms.size() + "\t" + candVocab.size() + "\t" + anchors1.size() + "\n");
         REPORT = false;
         }*/
        // sort anchor words based on their positions in the text
        Comparator<IndexEntry> comparator = new TermPosComparator();
        Collections.sort(anchors1, comparator);
        Collections.sort(anchors2, comparator);
    }

    public boolean isValidAnchor(String word) {

        for (int i = 0; i < word.length(); i++) {
            if (numericCharSequence.indexOf(word.charAt(i)) != -1) {
                return false;
            }
        }
        return true;
    }

    // ************************************************************************* //
    //                    OUTPUT FUNCTIONS
    // ************************************************************************* //
    // This method is equivalent to calling "outputAlignmentResults (null, null)" , but faster;
    public Stats calculateOCRaccuracy() {

        if (alignment.isEmpty()) {
            System.out.println("RecursiveAlignmentTool::calculateOCRaccuracy(): There are no words aligned. Aligner must be run prior to calling calculateOCRaccuracy");
            return null;
        }

        // align each segments characters individually
        long numError = 0;
        long total = 0;
        long ocrLength = 0;

        for (int kk = 0; kk < alignment.size(); kk++) {
            AlignedSequence alignedTerm = alignment.get(kk);
            String refout = alignedTerm.getReference();
            String candidateout = alignedTerm.getCandidate();
            if (candidateout != null) {
                ocrLength++;
            }
            if (refout != null) {
                total++;
                if (candidateout == null || !refout.equals(candidateout)) {
                    numError++;
                }
            }
        }

        Stats st = new Stats((total - numError), ocrLength, total);
        return st;
        // double accuracy = 1.0 - (numError / (double) total);
        // System.out.println("OCR accuracy: " + wordAccuracy + " (matching words= " + (total - numError) + " reference length= " + total + ")");
    }

    // Alignment results are written to outputfile
    // if both arguments are null, then this method simply calculates OCR accuracy and outputs nothing.
    public Stats outputAlignmentResults(String outputfile, String errorsFilename) {

        long numError = 0;
        long total = 0;
        int inc = 1;
        int lineWidth = 0;

        StringBuffer candBuffer = new StringBuffer(10000);
        StringBuffer refBuffer = new StringBuffer(10000);

        boolean colFormat = OUTPUT_FORMAT.toLowerCase().startsWith("c");
        // System.out.println(OUTPUT_FORMAT);

        String NULL_STRING = "null";
        if (WORD_LEVEL_ALIGNMENT) {
            lineWidth = 20;
            NULL_STRING = "null";
        } else {
            lineWidth = 100;
            NULL_STRING = "@";
        }
        long counter = 0;
        long ocrLen = 0;

        try {
            Writer writer = null;
            Writer errorWriter = null;
            if (outputfile != null) {
                //    writer = new BufferedWriter(new FileWriter(new File(outputfile)));
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputfile)), "UTF8"));
            }
            if (errorsFilename != null) {
                //  errorWriter = new BufferedWriter(new FileWriter(new File(errorsFilename)));
                errorWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(errorsFilename)), "UTF8"));
            }

            for (int kk = 0; kk < alignment.size(); kk++) {
                AlignedSequence alignedTerm = alignment.get(kk);

                String refout = alignedTerm.getReference();
                String candidateout = alignedTerm.getCandidate();

                if (candidateout != null) {
                    ocrLen++;
                }

                String ref = refout == null ? NULL_STRING : refout;
                String candidate = candidateout == null ? NULL_STRING : candidateout;

                if (writer != null) {
                    if (colFormat) {
                        writer.append(candidate + "\t" + ref + "\n");
                    } else {
                        if (counter == lineWidth) {
                            writer.append("OCR:\t" + candBuffer + "\n");
                            writer.append("GT :\t" + refBuffer + "\n\n");
                            candBuffer.delete(0, candBuffer.length());
                            refBuffer.delete(0, refBuffer.length());
                            counter = 0;
                            // kk--;
                        }
                        if (!WORD_LEVEL_ALIGNMENT) {
                            candBuffer.append(candidate);
                            refBuffer.append(ref);
                        } else {
                            String tabbedCand = candidate, tabbedRef = ref;
                            if (candidate.length() > ref.length()) {
                                for (int t = 0; t < (candidate.length() - ref.length()); t++) {
                                    tabbedRef += " ";
                                }
                            } else if (candidate.length() < ref.length()) {
                                for (int t = 0; t < (ref.length() - candidate.length()); t++) {
                                    tabbedCand += " ";
                                }
                            }
                            candBuffer.append(tabbedCand).append("\t");
                            refBuffer.append(tabbedRef).append("\t");
                        }
                        counter++;

                    }
                }

                if (refout != null) {
                    total++;
                    if (candidateout == null || !refout.equals(candidateout)) {
                        numError++;
                        if (errorWriter != null) {
                            errorWriter.append(candidateout + "\t" + refout + "\t" + (kk + inc) + "\n");
                        }
                    }
                }

            }
            if (!colFormat) {
                writer.append("OCR:\t" + candBuffer + "\n");
                writer.append("GT :\t" + refBuffer + "\n\n");
            }

            if (writer != null) {
                writer.close();
            }
            if (errorWriter != null) {
                errorWriter.close();
            }
        } catch (Exception e) {
            System.out.println("Error. Can not write the file: " + outputfile);
        }

        Stats st = new Stats((total - numError), ocrLen, total);
        return st;
        //     System.out.println("OCR accuracy: " + wordAccuracy + " (matching words= " + (total - numError) + " reference length= " + total + ")");
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {

        // System.out.println(".,;:=-+/'`&|$#@!%^*()[]_\"}{\\<>?~");
        int argc = args.length;
        String USAGE = "\nRecursive Text Alignment Tool (RETAS V1.0) Copyright (C) 2013 by the University of Massachusetts at Amherst\n"
                + "\nThis program comes with ABSOLUTELY NO WARRANTY. "
                + "This is free software, and you are welcome to redistribute it under certain conditions;"
                + " see the attached GNU licence for details.\n"
                + "\nUSAGE: java RecursiveAligmentTool <refFilename> <candFilename> <outputFilename> -opt <configFile>\n\n"
                + "<refFilename> is the reference text filename\n"
                + "<candFilename> is the candidate (OCR output) text filename\n"
                + "<outputFilename> is the filename for the alignment output (optional)\n"
                + "<configFile> file must contain the following arguments on each line:\n"
                + "\tignoredChars=<listOfChars>\n"
                + "\talignmentFormat=<COLUMN|LINES> (default is lines)\n"
                + "\tlevel=<W|C> (level of alignment can be either character or word level. Default is W.)\n\n"
                + "The screen output format is:\n"
                + "\t<refFilename> <candFilename> <OCR_accuracy>"
                + "\n\n TRY: java RecursiveAlignmentTool texts/adventuresofhuck_ground_truth.txt texts/adventuresofhuck00clemrich_OCR_output.txt texts/alignmentOutput.txt -opt config.txt";

        // System.out.println(USAGE);
        if (!(argc == 5 || argc == 4)) {
            System.out.println(USAGE);
            return;
        }

        String gtFile = null, candFile = null, alignFile = null, argFile = null;
        String level = "w", outFormat = "lines";
        // String ocrOutputFile = null;
        String ignoredChars = null;

        gtFile = args[0];
        candFile = args[1];
        if (args[2].equals("-opt")) {
            argFile = args[3];
        } else {
            alignFile = args[2];
            argFile = args[4];
        }

        // read arguments from the file
        double accuracy = 0.0;
        BufferedReader reader = new BufferedReader(new FileReader(new File(argFile)));
        String line = null;
        try {
            line = reader.readLine();
            line = line.trim();
            line = line.toLowerCase();

            int lineNumber = 1;
            while (line != null) {
                String[] tokens = line.split("=");
                int numOfArgs = tokens.length;
                if (numOfArgs > 0) {
                    if (numOfArgs == 2 && tokens[0].equals("level")) {
                        level = tokens[1];
                    } else if (numOfArgs == 2 && tokens[0].equalsIgnoreCase("alignmentFormat")) {
                        if (tokens[1].toLowerCase().startsWith("c")) {
                            outFormat = "cols";
                        }
                    } else if (tokens[0].equalsIgnoreCase("ignoredChars")) {
                        ignoredChars = line.substring(line.indexOf('=') + 1);
                    } else {
                        System.out.println("UNKNOWN OPTION: \"" + tokens[0] + "\" ->IGNORING...");
                    }
                } else {
                    System.out.println("Error in the configuration file. Skipping the parameter at line:" + line);
                }

                line = reader.readLine();
                lineNumber++;
            }
        } catch (IOException ex) {
            System.out.println("Error: Can not read configuration file -> " + argFile);
            System.exit(0);
        } finally {
            reader.close();
        }

        if (candFile == null || gtFile == null) {
            System.out.println("Error: Input and output files must be specified");
        } else {
            Stats st = processSingleJob(gtFile, candFile, level, outFormat, ignoredChars, alignFile);
            //   Stats sts[] = processSingleJob_OCR_accuracies_only(gtFile, candFile, ignoredChars);            
            //   BufferedWriter outwriter = null;
            //   outwriter = new BufferedWriter(new FileWriter(new File(ocrOutputFile),true)); // append
            //   outwriter.append(gtFile + "\t" + candFile + "\t" + accuracy + "\n");
            System.out.println(gtFile + "\t" + candFile + "\t" + st.getOCRAccuracy() + "\n");
            //    outwriter.close();
        }
    }

    // This is Recursive Text Alignment Scheme (RETAS) API.
    /**
     * This function produces the alignment at the word or character level and
     * produces a text output file. The output file has two formats. One can
     * also choose the characters to be ignored for the alignment.
     *
     * @param gtFile input text 1: ground truth text
     * @param candFile input text 2: OCR output text (or the candidate text)
     * @param level alignment level: "c" or "w" (for character and word level
     * alignment respectively)
     * @param outFormat the format of the alignment output: 'column' or 'line'
     * @param ignoredChars The list of characters to be ignored
     * @param alignFile output text filename
     *
     * @return This method returns the alignment statistics.
     */
    public static Stats processSingleJob(
            String gtFile, // input text 1: ground truth text
            String candFile, // input text 2: OCR output text (or the candidate text)
            String level, // The level of alignment: 'c' for the character and 'w' for the the word level alignment.
            String outFormat, // The format of the alignment output: 'column' or 'line' 
            String ignoredChars, // The list of characters to be ignored
            String alignFile // The filename for the alignment output
            ) {

        if (gtFile == null || candFile == null) {
            return null;
        }

        // select appropriate text preprocessor
        TextPreprocessor tp;
        Stats stats;
        tp = new TextPreprocessorUniversal(null); // if you want to use another locale, simply give it as a parameter to TextPreprocessorUniversal

        if (ignoredChars != null) {
            TextPreprocessor.IGNORED_CHARS = ignoredChars;
        } else {
            TextPreprocessor.IGNORED_CHARS = "";
        }

        // define the level of alignment
        if (level != null && level.toLowerCase().startsWith("c")) {
            RecursiveAlignmentTool.WORD_LEVEL_ALIGNMENT = false; // default value is true;
        } else {
            RecursiveAlignmentTool.WORD_LEVEL_ALIGNMENT = true;
        }

        if (outFormat != null && outFormat.toLowerCase().startsWith("l")) {
            RecursiveAlignmentTool.OUTPUT_FORMAT = "LINES";
        } else {
            RecursiveAlignmentTool.OUTPUT_FORMAT = "COLS";
        }

        // initialize and run the recursive alignment tool
        RecursiveAlignmentTool tool = new RecursiveAlignmentTool(gtFile, candFile, tp);
        tool.align();

        // output alignment results
        if (alignFile != null) {
            stats = tool.outputAlignmentResults(alignFile, null);
        } else {
            // if OCR accuracy is the only concern, then this method is faster
            stats = tool.calculateOCRaccuracy();
        }

        // System.out.println(OCRaccuracy);
        return stats;
    }

    // This is Recursive Text Alignment Scheme (RETAS) API.
    /**
     * if the number of matching chars/words is the only concern, then this
     * method is faster.
     *
     * @param gtFile input text 1: ground truth text
     * @param candFile input text 2: OCR output text (or the candidate text)
     * @param ignoredChars The list of characters to be ignored
     * @return It returns a Stats array of size two. The first element includes
     * the statistics for the word level alignment (i.e., number of matching
     * words, the length of the sequences aligned). The second element gives the
     * statistics for the character level alignment
     */
    public static Stats[] processSingleJob_getAlignmentStatsOnly(
            String gtFile, // input text 1: ground truth text
            String candFile, // input text 2: OCR output text (or the candidate text)
            String ignoredChars) // The list of characters to be ignored
    {

        if (gtFile == null || candFile == null) {
            return null;
        }

        NumberFormat ft = NumberFormat.getInstance();
        ft.setMaximumFractionDigits(4);

        // select appropriate text preprocessor
        TextPreprocessor tp;
        tp = new TextPreprocessorUniversal(null); // if you want to use another locale, simply give it as a parameter to TextPreprocessorUniversal

        if (ignoredChars != null) {
            TextPreprocessor.IGNORED_CHARS = ignoredChars;
        } else {
            TextPreprocessor.IGNORED_CHARS = "";
        }

        // Define the level of alignment
        RecursiveAlignmentTool.WORD_LEVEL_ALIGNMENT = true;

        // Initialize and run the recursive alignment tool for the word level alignment
        RecursiveAlignmentTool tool = new RecursiveAlignmentTool(gtFile, candFile, tp);
        Stats[] sts = tool.estimateWordAndCharacterOCRaccuracies();

        //System.out.println(ft.format(stWordAlignment.getOCRAccuracy()) + "\t" + ft.format(stCharAlignment.getOCRAccuracy()));     
        return sts;
    }

    /**
     *
     * @param gtFile input text 1: ground truth text
     * @param candFile input text 2: OCR output text (or the candidate text)
     * @param ignoredChars The list of characters to be ignored
     * @param level alignment level: "c" or "w" (for character and word level
     * alignment respectively)
     * @return This method returns the alignment output in an ArrayList.
     */
    public static ArrayList<AlignedSequence> processSingleJob_getAlignedSequence(
            String gtFile, // input text 1: ground truth text
            String candFile, // input text 2: OCR output text (or the candidate text)
            String ignoredChars, // The list of characters to be ignored
            String level) // alignment level: "c" or "w" (for character and word level alignment respectively)
    {

        if (gtFile == null || candFile == null) {
            return null;
        }

        // select appropriate text preprocessor
        TextPreprocessor tp;
        tp = new TextPreprocessorUniversal(null); // if you want to use another locale, simply give it as a parameter to TextPreprocessorUniversal

        TextPreprocessor.IGNORED_CHARS = "";
        if (ignoredChars != null) {
            TextPreprocessor.IGNORED_CHARS = ignoredChars;
        }

        // Define the level of alignment
        RecursiveAlignmentTool.WORD_LEVEL_ALIGNMENT = true;
        if (level != null && level.toLowerCase().startsWith("c")) {
            RecursiveAlignmentTool.WORD_LEVEL_ALIGNMENT = false;
        }

        // Initialize and run the recursive alignment tool for the word level alignment
        RecursiveAlignmentTool tool = new RecursiveAlignmentTool(gtFile, candFile, tp);
        ArrayList<AlignedSequence> out = tool.align();

        return out;
    }
}
