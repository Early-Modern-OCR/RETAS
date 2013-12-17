V1.1
----
Recursive Text Alignment Tool Copyright (C) 2013 by the University of Massachusetts at Amherst released under the GNU  GPL v3.0 (see license.txt)

Written by I. Zeki Yalniz

ACKNOWLEDGMENT
==============
This software was developed at the Center for Intelligent Information Retrieval (CIIR), University of Massachusetts Amherst.  
Basic research to develop the software was funded by the CIIR and the National Science Foundation while its 
application was supported by a grant from the Mellon Foundation. Any opinions, findings and conclusions or recommendations expressed in this
material are the authors' and do not necessarily reflect those of the sponsor. 

CITATION AND CONTACT INFORMATION
================================
We ask that any publications using this software acknowledge the following paper:  
Ismet Zeki Yalniz and R Manmatha: A Fast Alignment Scheme for Automatic OCR Evaluation of Books. ICDAR 2011: 754-758

For further information please contact either 
I. Zeki Yalniz (zeki@cs.umass.edu) or R. Manmatha (manmatha@cs.umass.edu) or info@ciir.cs.umass.edu. 

HOW TO COMPILE:
===============
Inside the source folder, type the following command to compile the code (tested for Java version 1.6):
"javac *.java"

HOW TO USE THE TOOL
===================

1 - COMMAND LINE INTERFACE:
---------------------------

USAGE: RecursiveAligmentTool <refFilename> <candFilename> <outputFilename> -opt <configFile>

<refFilename> is the reference (ground truth) text filename
<candFilename> is the candidate (OCR output) text filename
<outputFilename> is the filename for the alignment output (optional)
<configFile> file must contain the following arguments on each line:
        ignoredChars=<listOfChars>
        alignmentFormat=<COLUMN|LINES> (default is lines)
        level=<W|C> (level of alignment can be either character or word level. Default is W.)

The screen output format is:
        <refFilename> <candFilename> <OCR_accuracy>

Example command: java RecursiveAlignmentTool texts/adventuresofhuck_ground_truth.txt texts/adventuresofhuck00clemrich_OCR_output.txt texts/alignmentOutput.txt -opt config.txt
		
An example configuration file includes the three lines below:
------------------------------
level=CHAR
alignmentFormat=LINES
ignoredChars=,.'";:!?()[]{}<>`-+=/\$@%#|&^*_~
------------------------------


2 - RETAS JAVA API
--------------------------
2.a)
This method returns the alignment output in an ArrayList. 
It does not produce any text output 
	
    public static ArrayList<AlignedSequence> processSingleJob_getAlignedSequence(
            String gtFile,  // input text 1: ground truth text
            String candFile,  // input text 2: OCR output text (or the candidate text)
            String ignoredChars, // The list of characters to be ignored
            String level ) // alignment level: "c" or "w" (for character and word level alignment respectively)    
	
2.b) 

This function produces the alignment at the word or character level and produces a text output file. 
The output file has two formats. One can also choose the characters to be ignored for the alignment. 

Stats st = RecursiveAlignmentTool.processSingleJob(
            gtFile,    // (String) input text 1: ground truth text
            candFile,  // (String) input text 2: OCR output text
            alignmentLevel,  // (String) The level of alignment: 'c' for the character and 'w' for the the word level alignment.
            outputFormat,    // (String) The format of the alignment output: 'column' or 'line' 
            ignoredChars, // (String) The list of characters to be ignored
            alignFile     //  (String) The filename for the alignment output
            );  		

"Stats" object contains the total number of matching characters/words and the total number of chars/words in the input texts. 
OCR accuracy is defined to be the total number of matching chars/words divided by the total number of chars/words in the ground truth file. 
One can calculate OCR accuracy by calling the getOCRaccuracy() method as:

double ocrAccuracy = st.getOCRaccuracy();  
 
2.c)  

If the number of matching chars/words is the only concern, then this method is faster. 

Stats sts[] = RecursiveAlignmentTool.processSingleJob_getAlignmentStatsOnly(
            gtFile,    // (String) input text 1: ground truth text
            candFile,  // (String) input text 2: OCR output text
            ignoredChars, // (String) The list of characters to be ignored
			);
			
sts[0] contains the word level alignment statistics
sts[1] contains the character level alignment statistics

 
