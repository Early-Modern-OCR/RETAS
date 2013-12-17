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

import java.util.Comparator;

class TermPosComparator implements Comparator<IndexEntry> {
    // Comparator interface requires defining compare method.
    public int compare(IndexEntry a, IndexEntry b) {
        if (a.getPos() < b.getPos()) {
            return -1;

        } else if (a.getPos() > b.getPos()) {
            return 1;

        } else {
           if ( a.getNumOfTokens() < b.getNumOfTokens() ){
                return -1;
           }else if ( a.getNumOfTokens() > b.getNumOfTokens() ) {
                return 1;
           }else{
                return 0;
           }
        }
    }
}