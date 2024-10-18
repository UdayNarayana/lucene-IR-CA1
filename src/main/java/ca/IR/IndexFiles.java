package ca.IR;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class IndexFiles {

    private Analyzer analyzer;
    private FSDirectory indexDirectory;

    public IndexFiles(String indexPath) throws IOException {
        this.analyzer = new EnglishAnalyzer();
        this.indexDirectory = FSDirectory.open(Paths.get(indexPath));
    }

    public void indexDocuments(String filePath) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(indexDirectory, config);
             BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            StringBuilder docContent = new StringBuilder();
            int docId = 0;
            String docTitle = "";

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (docContent.length() > 0) {
                        addDocument(writer, docId, docTitle, docContent.toString());
                        docContent.setLength(0);
                        docTitle = "";
                    }
                    docId = Integer.parseInt(line.split(" ")[1].trim());
                    System.out.println("Document ID: " + docId);
                } else if (line.startsWith(".T")) {
                    StringBuilder titleBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(".A") || line.startsWith(".B")) {
                            break;
                        }
                        titleBuilder.append(line.trim()).append(" ");
                    }
                    docTitle = titleBuilder.toString().trim();
                    System.out.println("Title: " + docTitle);
                } else if (line.startsWith(".W")) {
                    StringBuilder contentBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(".I")) {
                            docContent.append(contentBuilder.toString().trim());
                            addDocument(writer, docId, docTitle, docContent.toString());
                            docContent.setLength(0);
                            docId = Integer.parseInt(line.split(" ")[1].trim());
                            System.out.println("New Document ID: " + docId);
                            break;
                        }
                        contentBuilder.append(line.trim()).append(" ");
                    }
                    if (line == null) {
                        docContent.append(contentBuilder.toString().trim());
                        addDocument(writer, docId, docTitle, docContent.toString());
                    }
                }
            }
        }
    }

    private void addDocument(IndexWriter writer, int docId, String title, String content) throws IOException {
        Document document = new Document();
        document.add(new TextField("id", String.valueOf(docId), Field.Store.YES));
        document.add(new TextField("title", title, Field.Store.YES));
        document.add(new TextField("content", content, Field.Store.YES));
        writer.addDocument(document);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java LuceneIndexer <index_directory> <dataset_file>");
            System.exit(1);
        }

        String indexPath = args[0];
        String datasetFilePath = args[1];

        try {
            IndexFiles indexer = new IndexFiles(indexPath);
            indexer.indexDocuments(datasetFilePath);
            System.out.println("Indexing completed successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
