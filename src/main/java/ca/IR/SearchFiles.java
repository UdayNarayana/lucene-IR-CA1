package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Scanner;

public class SearchFiles {

    public static void main(String[] args) throws Exception {
        String indexPath = args[0];   // Path to the index
        String queriesPath = args[1];  // Path to the queries
        String outputPath = args[2];   // Path for output.txt
        int scoreType = Integer.parseInt(args[3]);  // Score type (0 for Classic, 1 for BM25)

        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);

        // Set similarity based on the score type
        if (scoreType == 0) {
            searcher.setSimilarity(new ClassicSimilarity());
        } else if (scoreType == 1) {
            searcher.setSimilarity(new BM25Similarity());
        }

        StandardAnalyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("contents", analyzer);

        // Open output file for writing search results
        PrintWriter writer = new PrintWriter(new FileWriter(outputPath));

        // Reading the query file and executing the search
        try (Scanner scanner = new Scanner(new File(queriesPath))) {
            int queryID = 1; // Assuming queries are sequentially numbered

            while (scanner.hasNextLine()) {
                String queryString = scanner.nextLine();
                Query query = parser.parse(queryString);
                System.out.println("Searching for: " + queryString);
                ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;

                for (int rank = 0; rank < hits.length; rank++) {
                    Document doc = searcher.doc(hits[rank].doc);
                    String docID = doc.get("filename"); // Assuming document ID is stored in the filename field
                    float score = hits[rank].score;

                    // Write output in TREC format: queryID Q0 docID rank score STANDARD
                    writer.println(queryID + " Q0 " + docID + " " + (rank + 1) + " " + score + " STANDARD");
                }

                queryID++;
            }
        }

        writer.close();
        reader.close();
    }
}
