package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SearchFiles {
    private final IndexSearcher searcher;
    private final MultiFieldQueryParser parser;

    public SearchFiles(String indexPath) throws Exception {
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        this.searcher = new IndexSearcher(reader);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        String[] fields = {"title", "contents"};
        Map<String, Float> boosts = new HashMap<>();
        boosts.put("title", 2.0f);
        boosts.put("contents", 1.0f);
        this.parser = new MultiFieldQueryParser(fields, analyzer, boosts);
    }

    public void search(String queriesPath, String outputPath) throws Exception {
        File queryFile = new File(queriesPath);
        try (Scanner scanner = new Scanner(queryFile);
             PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            int queryNumber = 1;

            while (scanner.hasNextLine()) {
                String queryString = scanner.nextLine().trim();
                if (queryString.isEmpty()) continue;

                try {
                    Query query = parser.parse(QueryParser.escape(queryString));
                    TopDocs results = searcher.search(query, 100);
                    ScoreDoc[] hits = results.scoreDocs;

                    int rank = 1;
                    for (ScoreDoc hit : hits) {
                        Document doc = searcher.doc(hit.doc);
                        String docID = doc.get("documentID");
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

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: SearchFiles <indexDir> <queriesFile> <outputFile>");
            return;
        }

        String indexPath = args[0];
        String queriesPath = args[1];
        String outputPath = args[2];

        try {
            SearchFiles searcher = new SearchFiles(indexPath);
            searcher.search(queriesPath, outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
