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

public class Stats {
    long numOfMatches;
    long ocrLength;
    long gtLength;
    
    public Stats(long matches, long ocrlen, long gtlen ){
        numOfMatches = matches;
        ocrLength = ocrlen;
        gtLength = gtlen;
    }
    public long getNumOfMatches(){
        return numOfMatches;
    }
    public long getOcrLength(){
        return ocrLength;
    }
    public long getGtLength(){
        return gtLength;
    }
    public double getOCRAccuracy(){
        return ((double)numOfMatches/gtLength);
    }
}
