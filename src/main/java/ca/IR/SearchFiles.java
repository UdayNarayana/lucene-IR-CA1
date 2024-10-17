package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Scanner;

public class SearchFiles {
    public static void main(String[] args) throws Exception {
        // Ensure correct number of arguments
        if (args.length < 4) {
            System.out.println("Usage: SearchFiles <indexDir> <queriesFile> <scoreType> <outputFile>");
            return;
        }

        String indexPath = args[0];  // Path to the index
        String queriesPath = args[1]; // Path to the queries file
        int scoreType = Integer.parseInt(args[2]); // Scoring method
        String outputPath = args[3]; // Path for output

        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);

        // Set the similarity based on score type
        switch (scoreType) {
            case 0:
                searcher.setSimilarity(new ClassicSimilarity()); // Vector Space Model
                break;
            case 1:
                searcher.setSimilarity(new BM25Similarity()); // BM25
                break;
            case 2:
                searcher.setSimilarity(new BooleanSimilarity()); // Boolean
                break;
            case 3:
                searcher.setSimilarity(new LMDirichletSimilarity()); // LMDirichlet
                break;
            case 4:
                searcher.setSimilarity(new LMJelinekMercerSimilarity(0.7f)); // LMJelinekMercer
                break;
            default:
                System.out.println("Invalid score type");
                return;
        }

        StandardAnalyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("contents", analyzer);

        File queryFile = new File(queriesPath);
        try (Scanner scanner = new Scanner(queryFile);
             PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) { // Writing output file

            int queryNumber = 1; // Initialize query number
            while (scanner.hasNextLine()) {
                String queryString = scanner.nextLine().trim(); // Get query string
                if (queryString.isEmpty()) continue; // Skip empty lines

                try {
                    Query query = parser.parse(queryString);
                    ScoreDoc[] hits = searcher.search(query, 50).scoreDocs; // Get top 50 results

                    // Iterate through the hits
                    int rank = 1; // Initialize rank
                    for (ScoreDoc hit : hits) {
                        Document doc = searcher.doc(hit.doc);
                        String docID = doc.get("id"); // Get the document ID from the indexed document

                        // Format: <queryID> Q0 <documentID> <rank> <score> STANDARD
                        writer.println(queryNumber + " 0 " + docID + " " + rank + " " + hit.score + " STANDARD");
                        rank++;
                    }

                    queryNumber++; // Increment query number after processing each query
                } catch (ParseException e) {
                    System.out.println("Error parsing query: " + queryString);
                }
            }
        }
        reader.close();
    }
}
