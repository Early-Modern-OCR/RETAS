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
 * This class simply merges hyphenated tokens at the end of each line. Ignores
 * the characters in the INGORED_CHARS list.
 *
 * @author Ismet Zeki Yalniz
 */
import java.util.Locale;

public class TextPreprocessorUniversal extends TextPreprocessor {

    public TextPreprocessorUniversal(Locale loc) {
        if (loc == null) {
            locale = new Locale("en", "US");
        } else {
            locale = loc;
        }
    }

    public String processText(String s) {

        char[] charAr = s.toCharArray();
        char[] output = new char[charAr.length + 1];
        int backIndex = 0;
        for (int i = 0; i < charAr.length; i++) {

            char ch = charAr[i];

            // connect phonemes of words which are seperated by a dash
            // -------------------------------------------------------------
            // MERGE HYPHENATED WORDS:
            if (ch == '-' && i < (charAr.length - 1)) {

                // CASE: Merge hyphenated words at the end of each line.
                // Regular exp: HYPHEN (SPACE|TAB)* (NEWLINE|RETURN)
                int j = i + 1;
                while (charAr[j] == ' ' || charAr[j] == '\t') {
                    j++;
                }
                if (charAr[j] == '\n' || charAr[j] == '\r') {
                    //    j++;
                    i = j;
                    continue;
                }
                //i = j-1;
            }

            // TODO: it is possible to speed up the following code by hashing the list of ignored characters for O(1) time look-up. Current implementation has O(k) complexity where k is the total number of characters in the ingored char list. 
            // output the char
            //  if ( isValidChar(ch) ) {
            if (IGNORED_CHARS.indexOf(ch) == -1) {
                output[backIndex] = ch;
                backIndex++;
            }
        }
        output[backIndex] = '\0';
        String result = new String(output).substring(0, backIndex);

        // MCZ - adding trim because leading spaces result in empty tokens which
        // puts an empty word into the alignment that doesn't exist in the text:
        // String t[] = " a b c ".split("\\s+");
        // System.out.println(Arrays.toString(t));
        // outputs: [,a,b,c]
        // we could do it in the character array above, but that's also where we're
        // removing ignored characters so that removal could result in extra leading/trailing
        // spaces and the logic to figure out which ones to remove would be messy. Much
        // easier to just trim once all the processing is done.
        return (result.trim());
    }

    public boolean isValidChar(char a) {
        if (IGNORED_CHARS.indexOf(a) == -1) {
            return true;
        }
        return false;
    }
}
