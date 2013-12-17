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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Locale;

public abstract class TextPreprocessor {

    public Locale locale = null;
    public static String IGNORED_CHARS = ".,;:=-+/'`&|$#@!%^*()[]_\"}{\\<>?~";

    public abstract String processText(String s);

    public abstract boolean isValidChar(char a);

    public String toLowerCase(String s) {
        return s.toLowerCase(locale);
    }

    public static String getEncoding(String filename) {
        FileInputStream fi;
        InputStreamReader ir;
        String encoding = "";
        try {
            fi = new FileInputStream(filename);
            ir = new InputStreamReader(fi);
            encoding = ir.getEncoding();
            ir.close();
            fi.close();
        } catch (IOException ex) {
            System.out.println("TextPreprocessor: can not read file ->" + filename);
        }
        // System.out.println("encoding = " + encoding);
        return encoding;
    }

    public static void convertFileFormat(String inputFile, String outputFile, String inputEncoding, String outputEncoding) {
        Reader input = null;
        try {
            input = new InputStreamReader(new FileInputStream(inputFile), inputEncoding);
            Writer output = new OutputStreamWriter(new FileOutputStream(outputFile), outputEncoding);
            char[] buffer = new char[1000];
            int charsRead;
            while ((charsRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, charsRead);
            }
            input.close();
            output.close();
        } catch (Exception ex) {
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
            }
        }
    }

    public static ArrayList<String> readFilenames(String inputfilename, int from, int to) {

        BufferedReader reader = null;
        ArrayList<String> list = new ArrayList<String>(1000);
        String line = "";

        try {
            reader = new BufferedReader(new FileReader(new File(inputfilename)));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (count == to) {
                    break;
                } else {
                    if (!line.equals("") && count >= from) {
                        list.add(line);
                    }
                }
                count++;
            }
        } catch (Exception e) {
        }
        return list;
    }

    public static String[] getFilenames(String folder, String contains) {
        File folderfile = new File(folder);
        File[] files = null;
        MyFileFilter filter = new MyFileFilter(contains);
        String result[] = null;
        if (folderfile.isDirectory()) {
            files = folderfile.listFiles(filter);
            result = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                result[i] = files[i].getAbsolutePath();
            }
        } else {
            System.out.println("wrong input folder for foreign language books");

        }
        return result;
    }

    public static ArrayList<String> getFilenamesRecursive(String rootFolder) {
        ArrayList<String> result = new ArrayList<String>(10000);
        File[] files = null;
        // MyFileFilter filter = new MyFileFilter(contains);
        File folderfile = new File(rootFolder);

        if (folderfile.isDirectory()) {
            files = folderfile.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    result.addAll(getFilenamesRecursive(files[i].getAbsolutePath()));
                } else {
                    String filename = files[i].getAbsolutePath();
                    if (filename.substring(filename.lastIndexOf('.') + 1).equalsIgnoreCase("unique")) {
                        result.add(filename);
                    }
                }
            }
        } else {
            System.out.println("TextPreprocessor:getFilenamesRecursive: input folder is not valid");
        }
        return result;
    }

    public static boolean isWesternEuropeanLanguage(String s) {
        //Portuguese, French, German, Dutch, English, Danish, Swedish, Norwegian, and Icelandic,
        if (s.equalsIgnoreCase("english")
                || s.equalsIgnoreCase("french")
                || s.equalsIgnoreCase("spanish")
                || s.equalsIgnoreCase("italian")
                || s.equalsIgnoreCase("latin")
                || s.equalsIgnoreCase("german")
                || s.equalsIgnoreCase("portuguese")
                || s.equalsIgnoreCase("dutch")
                || s.equalsIgnoreCase("danish")
                || s.equalsIgnoreCase("swedish")
                || s.equalsIgnoreCase("norwegian")
                || s.equalsIgnoreCase("icelandic")) {
            return true;
        }
        return false;
    }

    public static String extractFilename(String s) {
        if (s == null || s.equals("")) {
            return "";
        } else {
            int start = s.lastIndexOf("\\");
            if (start == -1) {
                start = s.lastIndexOf('/');
            } else {
                start++;
            }
            if (start == -1) {
                start = 0;
            }
            int end = s.lastIndexOf('.');
            if (end == -1) {
                end = s.length();
            }
            return s.substring(start, end);
        }
    }

    public static String readFile(String filename) {

        File file = new File(filename);
        return readFile(file);
    }

    public static String readFile(File file) {

        if (file == null || !file.exists()) {
            System.out.println("TextPreprocessor: Can not read the file " + file.getAbsolutePath());
            return null;
        }

        StringBuffer bufc = null;
        FileReader fr = null;

        try {
            fr = new FileReader(file);

            int theChar;
            bufc = new StringBuffer();

            while (((theChar = fr.read()) != -1)) {
                bufc.append((char) theChar);
            }
        } catch (IOException ioe) {
            System.out.println("TextPreprocessor: Can not read the file " + file.getAbsolutePath());
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ioe) {
                }
            }
        }
        fr = null;
        return bufc.toString();
    }

    public static class MyFileFilter implements FileFilter {

        String contains = "";

        public MyFileFilter(String c) {
            contains = c;
        }

        public boolean accept(File file) {
            if (file.getName().toLowerCase().contains(contains)) {
                return true;
            }

            return false;
        }
    }
}
