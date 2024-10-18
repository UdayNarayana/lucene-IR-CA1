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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

public class SearchFiles {

    private FSDirectory indexDirectory;

    public SearchFiles(String indexPath) throws IOException {
        this.indexDirectory = FSDirectory.open(Paths.get(indexPath));
    }

    // Method to escape special characters in Lucene queries
    public String escapeSpecialCharacters(String queryString) {
        String[] specialChars = {"\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "/"};
        for (String specialChar : specialChars) {
            queryString = queryString.replace(specialChar, "\\" + specialChar);
        }
        return queryString;
    }

    // Method to search documents with BM25 similarity
    public void executeQueries(String queriesFilePath, String outputFilePath) throws IOException, ParseException {
        try (DirectoryReader reader = DirectoryReader.open(indexDirectory);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());
            Analyzer analyzer = new EnglishAnalyzer();
            QueryParser parser = new QueryParser("content", analyzer);

            try (Scanner scanner = new Scanner(Paths.get(queriesFilePath))) {
                int queryNumber = 1;
                while (scanner.hasNextLine()) {
                    String queryString = scanner.nextLine().trim();
                    if (queryString.isEmpty()) continue;

                    // Escape special characters in the query
                    queryString = escapeSpecialCharacters(queryString);
                    Query query = parser.parse(queryString);

                    // Execute the search
                    TopDocs results = searcher.search(query, 50);
                    for (ScoreDoc scoreDoc : results.scoreDocs) {
                        Document doc = searcher.doc(scoreDoc.doc);
                        writer.write(String.format("%d Q0 %s %d %.6f LuceneSearch\n",
                                queryNumber, doc.get("id"), scoreDoc.doc + 1, scoreDoc.score));
                    }
                    queryNumber++;
                }
            }
        }
        System.out.println("Search completed. Results written to: " + outputFilePath);
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java SearchFiles <index_directory> <query_file> <output_file>");
            System.exit(1);
        }

        String indexPath = args[0];
        String queryFilePath = args[1];
        String outputFilePath = args[2];

        try {
            SearchFiles searcher = new SearchFiles(indexPath);
            searcher.executeQueries(queryFilePath, outputFilePath);
            System.out.println("Search completed successfully.");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
