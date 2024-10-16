package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Scanner;

public class SearchFiles {

    public static void main(String[] args) throws Exception {
        String indexPath = args[0];
        String queriesPath = args[1];
        int scoreType = Integer.parseInt(args[2]);  // Score type parameter
        String outputPath = args[3];

        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);

        // Set similarity based on scoreType
        switch (scoreType) {
            case 0:
                searcher.setSimilarity(new ClassicSimilarity());  // Vector Space Model
                break;
            case 1:
                searcher.setSimilarity(new BM25Similarity());  // BM25
                break;
            case 2:
                searcher.setSimilarity(new BooleanSimilarity());  // Boolean Similarity
                break;
            case 3:
                searcher.setSimilarity(new LMDirichletSimilarity());  // LMDirichlet
                break;
            case 4:
                searcher.setSimilarity(new LMJelinekMercerSimilarity(0.7f));  // LMJelinekMercer
                break;
            default:
                System.out.println("Invalid score type. Please provide a value between 0 and 4.");
                return;
        }

        StandardAnalyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("contents", analyzer);

        File queryFile = new File(queriesPath);
        try (Scanner scanner = new Scanner(queryFile)) {
            while (scanner.hasNextLine()) {
                String queryString = scanner.nextLine();
                Query query = parser.parse(queryString);
                System.out.println("Searching for: " + queryString);
                ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;

                for (ScoreDoc hit : hits) {
                    Document doc = searcher.doc(hit.doc);
                    System.out.println("Found in file: " + doc.get("filename") + " with score: " + hit.score);
                }
            }
        }

        reader.close();
    }
}
