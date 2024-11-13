package ca.IR;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
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
            boosts.put("title", 4.0f);
            boosts.put("contents", 1.5f);

            MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, boosts);

            parseQueries(queriesPath, parser, searcher, writer);
        }

        System.out.println("Search completed. Results written to: " + outputPath);
    }

    private static void parseQueries(
            String queriesPath,
            MultiFieldQueryParser parser,
            IndexSearcher searcher,
            PrintWriter writer) {
        File queryFile = new File(queriesPath);

        if (!queryFile.exists() || !queryFile.canRead()) {
            System.out.println("Query file not found or is not readable: " + queriesPath);
            return;
        }

        try (Scanner scanner = new Scanner(queryFile)) {
            int queryNumber = 1;
            StringBuilder queryBuilder = new StringBuilder();
            boolean readingQuery = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                if (line.startsWith(".I")) {
                    if (readingQuery) {
                        processQuery(queryBuilder.toString(), queryNumber, parser, searcher, writer);
                        queryBuilder.setLength(0);
                    }
                    queryNumber++;
                    readingQuery = false;
                } else if (line.startsWith(".W")) {
                    readingQuery = true;
                } else if (readingQuery) {
                    queryBuilder.append(line).append(" ");
                }
            }
            if (queryBuilder.length() > 0) {
                processQuery(queryBuilder.toString(), queryNumber, parser, searcher, writer);
            }
        } catch (Exception e) {
            System.out.println("Error reading the query file: " + e.getMessage());
        }
    }

    private static void processQuery(
            String queryString,
            int queryNumber,
            MultiFieldQueryParser parser,
            IndexSearcher searcher,
            PrintWriter writer) {
        try {
            queryString = escapeSpecialCharacters(queryString.trim());
            Query query = parser.parse(queryString);

            ScoreDoc[] hits = searcher.search(query, 50).scoreDocs;

            int rank = 1;
            for (ScoreDoc hit : hits) {
                Document doc = searcher.doc(hit.doc);
                int docID = doc.getField("documentID").numericValue().intValue(); // Retrieve docID as integer
                writer.println(queryNumber + " 0 " + docID + " " + rank + " " + hit.score + " STANDARD");
                rank++;
            }
        } catch (Exception e) {
            System.out.println("Error parsing query: " + queryString);
        }
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
                searcher.setSimilarity(new LMDirichletSimilarity()); // LMDirichletSimilarity
                break;
            default:
                throw new IllegalArgumentException("Invalid score type: " + scoreType);
        }
    }

    public static String escapeSpecialCharacters(String queryString) {
        String[] specialChars = {"\\", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", "/"};
        for (String specialChar : specialChars) {
            queryString = queryString.replace(specialChar, "\\" + specialChar);
        }
        return queryString;
    }
}
