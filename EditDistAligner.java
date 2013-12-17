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

import java.util.ArrayList;

public class EditDistAligner {

    String[] reference;
    String[] candidate;
    int s1, e1, s2, e2;
    ArrayList<AlignedSequence> alignment;
    
    int[][] table;
    int[][] trace; 
    
    final int MATCH = 4;
    final int SUBSTITUTION = 3;
    final int INSERTION = 2;
    final int DELETION = 1;

    int SUBSTITUTION_PENALTY = 2;
    int INSERTION_PENALTY = 1;
    int DELETION_PENALTY = 1;   

    
    public void setPenalties(int ins, int del, int sb){
        SUBSTITUTION_PENALTY = sb;
        INSERTION_PENALTY = ins; 
        DELETION_PENALTY = del;
    }
     
    // aligns subsegments of the input sequences 
    public ArrayList<AlignedSequence> align(String[] reference, String[] candidate, int s1, int e1, int s2, int e2) {

        this.reference = reference;
        this.candidate = candidate;
        this.s1 = s1;
        this.s2 = s2;
        this.e1 = e1;
        this.e2 = e2;
        
        runEditDist();
        alignment = backtrace();     
        return alignment;
    }

    private void runEditDist() {
        int penalty;
        int minPenalty;
        int bestOp; 
        
        int m = e1-s1; // ref length
        int n = e2-s2; // cand length
        
        table = new int[m + 1][n + 1];
        trace = new int[m + 1][n + 1];
        table[0][0] = 0;
        trace[0][0] = MATCH;
        
        // init
        for (int i = 1; i <= m; i++) {
            table[i][0] = DELETION_PENALTY * i;
            trace[i][0] = DELETION;
        }
        for (int i = 1; i <= n; i++) {
            table[0][i] = INSERTION_PENALTY * i;
            trace[0][i] = INSERTION;
        }       
        
        // fill the dynamic programming table
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                minPenalty = table[i - 1][j] + DELETION_PENALTY;
                bestOp = DELETION;
               
                penalty = (table[i][j - 1] + INSERTION_PENALTY);
                if ( minPenalty >  penalty){
                    minPenalty = penalty;
                    bestOp = INSERTION;
                }

                if (reference[s1 + i - 1].equals(candidate[s2 + j - 1])) {
                    penalty = table[i - 1][j - 1];
                    if (penalty < minPenalty) {
                        minPenalty = penalty;
                        bestOp = MATCH;
                    }
                } else {
                    penalty = table[i - 1][j - 1] + SUBSTITUTION_PENALTY;
                    if (penalty < minPenalty) {
                        minPenalty = penalty;
                        bestOp = SUBSTITUTION;
                    }
                }
              
                table[i][j] = minPenalty;
                trace[i][j] = bestOp;
            }
        }
    }

    private ArrayList<AlignedSequence> backtrace() {
             
        int i= e1-s1;
        int j= e2-s2;
        ArrayList<Integer> ops = new ArrayList<Integer>();            
        
        while ( (i >= 0) && (j >= 0) ) {
            ops.add(trace[i][j]);
            if ( trace[i][j] == MATCH || trace[i][j] == SUBSTITUTION){
                i--;
                j--;
            }else if (trace[i][j] == INSERTION) {
                j--;
            }else{
                i--;
            }
        }
        
        // recover the alignment
        ArrayList<AlignedSequence> output = new ArrayList<AlignedSequence>();
        String refElement;
        String candElement;
        int refIndex = s1;
        int candIndex = s2;    	

        for (int m = ops.size() - 2; m >= 0; m--) {
            int currentOP = (ops.get(m)).intValue();
            
            if (currentOP != DELETION) {
                candElement = candidate[candIndex];
                candIndex++;
            } else {
                candElement = null;
            }           
            if (currentOP != INSERTION) {
                refElement = reference[refIndex];
                refIndex++;
            } else {
                refElement = null;
            }     
            output.add(new AlignedSequence(refElement, candElement));
        }                
        return output;
    }
}
