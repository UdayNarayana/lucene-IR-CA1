package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SearchFiles {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: SearchFiles <indexDir> <queriesFile> <scoreType> <outputFile>");
            return;
        }

        String indexPath = args[0];
        String queriesPath = args[1];
        int scoreType = Integer.parseInt(args[2]);
        String outputPath = args[3];

        try (DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
             PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {

            IndexSearcher searcher = new IndexSearcher(reader);
            setSimilarity(searcher, scoreType);

            StandardAnalyzer analyzer = new StandardAnalyzer();
            String[] fields = {"title", "contents"};
            Map<String, Float> boosts = new HashMap<>();
            boosts.put("title", 3.0f);  // Increase the boost for the title field
            boosts.put("contents", 1.0f);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);

            File queryFile = new File(queriesPath);
            try (Scanner scanner = new Scanner(queryFile)) {
                int queryNumber = 1;

                while (scanner.hasNextLine()) {
                    String queryString = scanner.nextLine().trim();
                    if (queryString.isEmpty()) continue;

                    try {
                        queryString = escapeSpecialCharacters(queryString); // Escape special characters
                        Query query = parser.parse(queryString);

                        // Run the search and retrieve top 100 results
                        ScoreDoc[] hits = searcher.search(query, 1400).scoreDocs;

                        int rank = 1;
                        for (ScoreDoc hit : hits) {
                            Document doc = searcher.doc(hit.doc);

                            // Fetching the indexed 'documentID' instead of internal docId
                            String docID = doc.get("documentID");

                            // Write result in TREC format: queryNumber Q0 docID rank score runTag
                            writer.println(queryNumber + " 0 " + docID + " " + rank + " " + hit.score + " STANDARD");
                            rank++;
                        }
                        queryNumber++;
                    } catch (Exception e) {
                        System.out.println("Error parsing query: " + queryString);
                    }
                }
            }
        }

        System.out.println("Search completed. Results written to: " + outputPath);
    }

    // Method to set the similarity model
    private static void setSimilarity(IndexSearcher searcher, int scoreType) {
        switch (scoreType) {
            case 0:
                searcher.setSimilarity(new ClassicSimilarity()); // Classic TF-IDF
                break;
            case 1:
                searcher.setSimilarity(new BM25Similarity(1.5f, 0.75f)); // Tuned BM25
                break;
            case 2:
                searcher.setSimilarity(new BooleanSimilarity()); // Boolean
                break;
            default:
                throw new IllegalArgumentException("Invalid score type: " + scoreType);
        }
    }

    // Method to escape special characters in queries
    public static String escapeSpecialCharacters(String queryString) {
        String[] specialChars = {"\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "/"};
        for (String specialChar : specialChars) {
            queryString = queryString.replace(specialChar, "\\" + specialChar);
        }
        return queryString;
    }
}
