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

public class AlignedSequence {

    String m_reference;
    String m_candidate;

    public AlignedSequence(String r, String c) {
         init(r, c);
    }

    public AlignedSequence(char r, String c) {
        init(Character.toString(r), c);
    }

    public AlignedSequence(String r, char c) {
        init(r, Character.toString(c));
    }

    public AlignedSequence(char r, char c) {
        init(Character.toString(r), Character.toString(c));
    }

    private void init(String r, String c) {
        m_reference = r;
        m_candidate = c;
    }

    public String getReference() {
        return m_reference;
    }

    public String getCandidate() {
        return m_candidate;
    }
}
