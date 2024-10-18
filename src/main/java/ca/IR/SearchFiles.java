package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.document.Document;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.nio.file.Paths;

public class SearchFiles {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java CranfieldQuery <indexdir> <qryfile> <resultfile>");
            System.exit(1);
        }

        String indexPath = args[0];   // First argument: path to the index directory
        String qryFile = args[1];     // Second argument: path to cran.qry file
        String resultFile = args[2];  // Third argument: path to store result output

        // Load the index directory
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(indexPath))));
        QueryParser parser = new QueryParser("content", new StandardAnalyzer());

        BufferedReader br = new BufferedReader(new FileReader(qryFile));
        FileWriter resultWriter = new FileWriter(resultFile);
        String line;
        int queryId = 0;

        while ((line = br.readLine()) != null) {
            if (line.startsWith(".W")) {
                queryId++;
                StringBuilder queryText = new StringBuilder();
                while ((line = br.readLine()) != null && !line.startsWith(".")) {
                    queryText.append(line).append(" ");
                }

                Query query = parser.parse(queryText.toString());
                TopDocs results = searcher.search(query, 50);

                for (ScoreDoc sd : results.scoreDocs) {
                    Document d = searcher.doc(sd.doc);
                    resultWriter.write(queryId + " Q0 " + d.get("id") + " " + sd.score + "\n");
                }
            }
        }
        resultWriter.close();
        br.close();

        System.out.println("Querying completed.");
    }
}
