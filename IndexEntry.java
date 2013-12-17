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
public class IndexEntry {

    private String term;
    private long fre;
    private long pos;
    private int numOfTokens;

    public IndexEntry(String t, long f, long p, int n){
        term = t;
        fre = f;
        pos = p;
        numOfTokens = n;
    }

    public boolean setFrequency(long f){
        if (f >= 0){
            fre = f;
            return true;
        }
        return false;

    }
    public boolean setPos(long p){
        if (p >= 0){
            pos = p;
            return true;
        }
        return false;
   }
    public boolean setNumOfTokens(int n){
       //if (n >= 0){
            numOfTokens = n;
            return true;
       //}
       //return false;
    }
    public void setTerm(String t){
        term = t;
    }

    public long getFrequency(){
        return fre;
    }
    public long getPos(){
        return pos;
    }
    public int getPosInt(){
        return (int)pos;
    }

    public int getNumOfTokens(){
        return numOfTokens;
    }
    public String getTerm(){
        return term;
    }
    public void incrementFre(){
        fre++;
    }

}
