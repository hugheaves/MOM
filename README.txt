Readme File for Maximum Oligo Mapping (MOM) Software v0.6

Contents
======================

   1. Version History
   2. System Requirements
   3. Packaging
   4. Execution
   5. Parameters
   6. Input Format
   7. Output Format

1. Version History
==================

Version    Date	       Description
-------    ----        -----------
0.1        09/15/2008  Initial release
0.2        04/02/2009  RNA support, lowercase Fasta files, performance enhancements, minor bugfixes
0.3RC      06/01/2009  Paired read initial implementation, improved output format compatibility with Eland
0.4        07/15/2009  Improved performance when using a large number of small reference sequences.
0.5        02/11/2009  Fixed issue with output locations in paired read implementation
0.6        07/10/2009  Added forwardStrandOnly parameter

2. System Requirements
======================

The Maximum Oligo Mapping software will run on any hardware and operating system that supports a JavaSE JRE 1.6 or later. Although the software will work with a 32bit JRE for small datasets, a 64bit JRE is recommended for maximum performance and the ability to utilize more memory. A minimum of 16GB RAM recommended for Human genome searches.

3. Packaging
======================

The software is packaged in two jar files. The main program jar file is OligoMap.jar, and the required utility routines are in fastutil-5.1.5.jar. Both jar files must be placed in the same directory for the program to function.

4. Execution
======================

OligoMap.jar can be executed using the standard java command line JRE invocation. For example, assuming the "java" or "java.exe" executable is in the path, the program can be executed using the following command line:

java -Xmx15000M -jar OligoMap.jar @searchparams.txt

"-Xmx" is the Java option specifying the maximum memory allocation (heap size) in megabytes (15,000MB) for the JVM. This should be set close to the physical memory size of the machine, but allowing for several hundred megabytes of operating system overhead. For example, with 16GB of physical memory, setting this value to 15000M allows for approximately 1000MB of operating system overhead. "searchParams.txt" is the name of a text file containing the search parameters. Detailed information on the available parameters and parameter usage is in the next section.

Search parameters can also be specified individually on the command line instead of a text file, or combined with parameters in a text file. For example:

java -Xmx15000M -jar OligoMap.jar @searchparams.txt k=2 query=testSeq1.txt output=results_testSeq1.txt

In this case, parameter values would be read from searchParams.txt, but the values for the "k", "query", and "output" parameters would be taken from the command line.

5. Parameters
======================

Search parameters are specified using the format "NAME=VALUE" where NAME is the name of the parameter, and "VALUE" is the value for that parameter. Parameter names are case sensitive. Parameters can be specified individually on the command line as shown above, or placed one per line in a text file. An example text file is included in the distribution as "example_params.txt".
Parameter Descriptions

Parameter: numThreads
Description: Number of threads to use for search and indexing. It is recommended to set this to the number of physical CPU cores.
Required: No
Default Value: 1
Valid Values: Integer >= 1

Parameter: s
Description: Size of query sequences (# bases)
Required: Yes
Valid Values: Integer >= 1

Parameter: k
Description: Maximum number of allowed mismatching bases
Required: Yes
Valid Values: Integer >= 0

Parameter: q
Description: Minimum number of bases in valid match (i.e. minimum sequence length)
Required: Yes
Valid Values: Integer >= 1

Parameter: searchMethod
Description: The search algorithm that should be used
Required: Yes
Valid Values: indexQueries - index the queries, sequentially read reference sequence
              indexReference - index the reference sequence, sequentially read queries
              pairedQueries - match

Parameter: querySeedInterval
Description: The number of bases to skip between each seed in the query sequence
Required: No
Default: Same as seedSize (i.e. query seeds will not overlap)
Valid Values: Integer >= 1

Parameter: maxMatchesPerQuery
Description: The maximum number of equal matches per query sequence. When this value is exceeded, additional matches for the query will be ignored.
Required: No
Default: Unlimited
Valid Values: Integer >= 1 or not specified (unlimited)

Parameter: seedLength
Description: The number of bases to use in the seed
Required: No
Default: floor (s / k + 1)
Valid Values: Integer >= 1

Parameter: query - single query file for single read mapping
           query1, query2 - paired query files for paired read mapping
Description: The filename containing the query sequences in FASTA or RAW format.
Required: Yes
Valid Values: Any valid filename

Parameter: pairedReadMinGap
Description: the distance (in bases) between reads below the reads are considered not to be part of a paired read
Required: No
Valid Values: Integer >= 1 <= pairedReadMaxGap

Parameter: pairedReadMaxGap
Description: the distance (in bases) between reads above which the reads are considered not to be part of a paired read
Required: No
Valid Values: Integer >= pairedReadMinGap

Parameter: reference1 ... referenceN (multiple parameters)
Description: The filename containing reference sequences in FASTA or RAW format. Multiple reference sequences may be specified (parameter name is reference1, reference2, etc.).
Required: Yes
Valid Values: Any valid filename

Parameter: output
Description: The name of the output file
Required: Yes
Valid Values: Any valid filename

Parameter: forwardStrandOnly
Description: If set to true, only matches on the forward strand will be returned. (Matches on the reverse complement will be ignored)
Required: No
Valid Values: true / false

6. Input Format
======================

MOM supports input files in either FASTA or "raw" format. "Raw" format files are in a propriatary format that holds only the sequence data with no header information. As there are no headers or other delimiters in the file, raw files are limited to one sequence per file. For reference files, the sequence can be broken into multiple lines, or containted on a single line. All lines in the raw reference file will be combined into a single sequence. For "raw" format query (read) files, each read should be on a seperate line.

A raw format query file would looks like this:

CAGCTCTTGTGGTCGCTCAAAGCTACTTTAATACGAACAA
ACATTATAGCGCGGACGTTCCAGGCAGAGTCCTGTTCTAA
CGGTACGTTTCGGAGGCTGGTGCGCGCTATTCCCGATACT
ACGCGACAGGGTGCGAGCCACACTCGTGTTGTGAACAAGA

This would be equivalent to the same FASTA file:

>Read1
CAGCTCTTGTGGTCGCTCAAAGCTACTTTAATACGAACAA
>Read2
ACATTATAGCGCGGACGTTCCAGGCAGAGTCCTGTTCTAA
>Read3
CGGTACGTTTCGGAGGCTGGTGCGCGCTATTCCCGATACT
>Read4

For FASTA reference files, MOM supports multiple FASTA segments per file. For the FASTA queries file, there must be one FASTA header per read.

7. Output Format
======================

Single Reads
------------

For single reads, MOM creates an Eland compatible output file. The output fields are tab delimited. The field descriptions are as follows:

1. Read Id - Either the FASTA id for this read from the queries file, or in the case of "raw" input format, the file name followed by the line number

2. Matching Read Sequence - The matching portion of the read sequence in the case of a single unique match. In the case of multiple matches or no matches, this will contain the entire read sequence.

3. Match Type- One of NM (no match), Un (Unique match with n number of mismatches), Rn (multiple matches with n number of mismatches)

If there are one or more matches, the following fields will be filled:

4. Number of zero mismatch matches - The number of matches containing no mismatches.

5. Number of one mismatch matches - The number of matches containing one mismatch.

6. Number of two mismatch matches - The number of matches containing two mismatches.

If there is a unique match, the following fields will be filled:

7. Reference Id - The FASTA id of the matching reference sequence, or the name of the input file in the case of "raw" format reference files.

8. Match position - The position of the match within the reference sequence (1...n, where 1 is the first base in the reference sequence)

9. Forward or Reverse - Read matched on either the forward (F) or reverse complementary (R) strand of the reference sequence.

10. Unused - In MOM, this field is unused and will always contain "."

11. Mismatching bases  - A space separated list of the mismatches between the reference and query sequences. The format is "nB" where n is the position of the mismatch in the matching portion of the read (not from the first base in the entire read), and B is the base that was found in the reference genome.

Paired Reads
------------

For paired reads, MOM an output file, with each pair of reads creating one or more lines of output in the file. For pairs of reads where there are multiple unique matches for the pair (within gap range specified), one line of output is generated for each combination of match locations.

Individual fields in the output file follow the same format as the single read (Eland) format, but with one set of fields for each read in the pair. 

Field definitions:
 
1. Match Status -
N - No Match - 
U - Unique Match, In Range -
M - Multiple Matches, In Range -
E - Unique Match, Outside Range - 
X - Multiple Matches, Outside Range 

2. Distance between reads (999,999,999 for different regions) 

3. Read 1 - Read Id
4. Read 1 - Total number of matches against this read
5. Read 1 - Matching sequence (for a single match)
6. Read 1 - Read start match position (for a single match)
7. Read 1 - Read match length (for a single match)
8. Read 1 - SNP's  (for a single match)
9. Read 1 - Match Position on Reference
10. Read 1 - Forward or reverse direction (F/R)
11. Read 1 - Reference Sequence Id

12. Read 2 - Read Id
13. Read 2 - Total number of matches against this read
14. Read 2 - Matching sequence (for a single match)
15. Read 2 - Read start match position (for a single match)
16. Read 2 - Read match length (for a single match)
17. Read 2 - SNP's  (for a single match)
18. Read 2 - Match Position on Reference
19. Read 2 - Forward or reverse direction (F/R)
20. Read 2 - Reference Sequence Id
