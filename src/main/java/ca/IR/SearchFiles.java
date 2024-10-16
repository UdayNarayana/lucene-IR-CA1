package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Scanner;

public class SearchFiles {
    public static void main(String[] args) throws Exception {
        // Check for proper number of arguments
        if (args.length < 4) {
            System.out.println("Usage: SearchFiles <indexDir> <queriesFile> <scoreType> <outputFile>");
            return;
        }

        String indexPath = args[0];  // Path to the index
        String queriesPath = args[1]; // Path to the queries
        int scoreType = Integer.parseInt(args[2]); // Score type parameter
        String outputPath = args[3]; // Output file path

        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);

        // Set similarity based on scoreType
        switch (scoreType) {
            case 0:
                searcher.setSimilarity(new ClassicSimilarity()); // Vector Space Model
                break;
            case 1:
                searcher.setSimilarity(new BM25Similarity()); // BM25
                break;
            case 2:
                searcher.setSimilarity(new BooleanSimilarity()); // Boolean Similarity
                break;
            case 3:
                searcher.setSimilarity(new LMDirichletSimilarity()); // LMDirichlet
                break;
            case 4:
                searcher.setSimilarity(new LMJelinekMercerSimilarity(0.7f)); // LMJelinekMercer
                break;
            default:
                System.out.println("Invalid score type. Please provide a value between 0 and 4.");
                return;
        }

        StandardAnalyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("contents", analyzer);

        File queryFile = new File(queriesPath);
        try (Scanner scanner = new Scanner(queryFile);
             PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) { // Open output file

            int queryNumber = 1; // Initialize query number before the loop
            while (scanner.hasNextLine()) {
                String queryString = scanner.nextLine().trim(); // Trim whitespace
                System.out.println("Query String: " + queryString); // Log the query

                try {
                    Query query = parser.parse(queryString);
                    System.out.println("Searching for: " + queryString);
                    ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;

                    int rank = 1; // Initialize rank
                    for (ScoreDoc hit : hits) {
                        Document doc = searcher.doc(hit.doc);
                        // Ensure "documentID" is stored correctly in your index
                        String docID = doc.get("documentID"); // assuming you store the document ID as "documentID"
                        writer.println(queryNumber + " Q0 " + docID + " " + rank + " " + hit.score + " STANDARD");
                        rank++; // Increment rank
                    }
                    queryNumber++; // Increment query number after processing each query
                } catch (ParseException e) {
                    System.out.println("Failed to parse query: " + queryString);
                    e.printStackTrace(); // Print the error stack trace
                }
            }
        }
        reader.close();
    }
}
