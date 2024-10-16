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
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Scanner;

public class SearchFiles {
    public static void main(String[] args) throws Exception {
        String indexPath = args[0];
        String queriesPath = args[1];

        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new BM25Similarity());

        StandardAnalyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("contents", analyzer);

        File queryFile = new File(queriesPath);
        try (Scanner scanner = new Scanner(queryFile)) {
            while (scanner.hasNextLine()) {
                String queryString = scanner.nextLine().trim(); // Trim whitespace
                System.out.println("Query String: " + queryString); // Log the query

                try {
                    Query query = parser.parse(queryString);
                    System.out.println("Searching for: " + queryString);
                    ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;

                    for (ScoreDoc hit : hits) {
                        Document doc = searcher.doc(hit.doc);
                        System.out.println("Found in file: " + doc.get("filename") + " with score: " + hit.score);
                    }
                } catch (ParseException e) {
                    System.out.println("Failed to parse query: " + queryString);
                    e.printStackTrace(); // Print the error stack trace
                }
            }
        }
        reader.close();
    }
}
