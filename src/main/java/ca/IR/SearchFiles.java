package ca.IR;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SearchFiles {

    private FSDirectory indexDirectory;
    private HashMap<String, HashSet<String>> relevanceJudgments;

    public SearchFiles(String indexPath) throws IOException {
        this.indexDirectory = FSDirectory.open(Paths.get(indexPath));
        this.relevanceJudgments = new HashMap<>();
    }

    // Method to read relevance judgments from the file
    public void readRelevanceJudgments(String relevanceFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(relevanceFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+"); // Split by whitespace
                if (parts.length == 3) {
                    String queryId = parts[0];
                    String docId = parts[1];
                    // Add to the relevance judgments map
                    relevanceJudgments.computeIfAbsent(queryId, k -> new HashSet<>()).add(docId);
                }
            }
        }
    }

    // Escape special characters in Lucene queries
    public String escapeSpecialCharacters(String queryString) {
        String[] specialChars = {"\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "/"};
        for (String specialChar : specialChars) {
            queryString = queryString.replace(specialChar, "\\" + specialChar);
        }
        return queryString;
    }

    // Method to search documents with BM25 similarity
    public TopDocs searchWithBM25(String queryString) throws ParseException, IOException {
        queryString = escapeSpecialCharacters(queryString.trim());
        try (DirectoryReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            // Set BM25 as the similarity model
            searcher.setSimilarity(new BM25Similarity());
            QueryParser parser = new QueryParser("content", new EnglishAnalyzer());
            Query query = parser.parse(queryString);
            return searcher.search(query, 50); // Retrieve top 50 results
        }
    }

    public double evaluatePrecision(String queryId, TopDocs results) throws IOException {
        HashSet<String> relevantDocs = relevanceJudgments.get(queryId);
        if (relevantDocs == null || relevantDocs.isEmpty()) {
            return 0.0;
        }
        int relevantRetrieved = 0;
        double precisionSum = 0.0;
        System.out.println("Results for Query ID: " + queryId);
        for (int i = 0; i < results.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = results.scoreDocs[i];
            Document doc = DirectoryReader.open(indexDirectory).document(scoreDoc.doc);
            String docId = doc.get("id");
            boolean isRelevant = relevantDocs.contains(docId);
            // Print the rank, document ID, and relevance status
            System.out.println("Rank: " + (i + 1) + ", Document ID: " + docId + ", Relevant: " + isRelevant);
            if (isRelevant) {
                relevantRetrieved++;
                precisionSum += (double) relevantRetrieved / (i + 1);  // Precision at i+1
            }
        }
        double avgPrecision = precisionSum / relevantDocs.size();  // Average Precision for this query
        System.out.println("Average Precision for Query ID " + queryId + ": " + avgPrecision);
        System.out.println("=========================================");
        return avgPrecision;
    }

    public double calculateMAP(HashMap<String, String> queries) throws IOException, ParseException {
        double totalAP = 0.0;
        System.out.println("Starting MAP calculation...");
        for (Map.Entry<String, String> entry : queries.entrySet()) {
            String queryId = entry.getKey();
            System.out.println("Processing Query ID: " + queryId);
            String query = entry.getValue();
            TopDocs results = searchWithBM25(query); // Search using BM25
            totalAP += evaluatePrecision(queryId, results); // Add precision to totalAP
        }
        return totalAP / queries.size(); // Return Mean Average Precision
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err.println("Usage: java LuceneSearcher <index_directory> <query_file> <relevance_file> <output_file>");
            System.exit(1);
        }

        String indexPath = args[0];
        String queryFilePath = args[1];
        String relevanceFilePath = args[2];
        String outputFilePath = args[3];

        try {
            SearchFiles searcher = new SearchFiles(indexPath);
            searcher.readRelevanceJudgments(relevanceFilePath);

            // Load queries from the query file (assumed to be simple key-value pairs)
            HashMap<String, String> queries = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(queryFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\t");
                    if (parts.length == 2) {
                        queries.put(parts[0], parts[1]);
                    }
                }
            }

            double mapScore = searcher.calculateMAP(queries);
            System.out.println("Mean Average Precision: " + mapScore);
            System.out.println("Search completed successfully.");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
