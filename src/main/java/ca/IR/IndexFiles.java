package ca.IR;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IndexFiles {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: IndexFiles <indexDir> <docsDir>");
            return;
        }

        String indexPath = args[0];  // Directory to save the index
        String docsPath = args[1];   // Directory of the documents

        // Check if documents directory exists
        File docsDir = new File(docsPath);
        if (!docsDir.exists() || !docsDir.isDirectory()) {
            System.out.println("Document directory does not exist or is not a directory: " + docsPath);
            return;
        }

        // Opening the directory for index storage
        Directory dir = FSDirectory.open(Paths.get(indexPath));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        // Index each document in the specified directory
        File[] files = docsDir.listFiles();
        if (files != null) { // Null check for listFiles()
            for (File file : files) {
                if (file.isFile()) {
                    System.out.println("Indexing file: " + file.getName());
                    indexDoc(writer, file);
                }
            }
        } else {
            System.out.println("No files found in the directory: " + docsPath);
        }

        writer.close();
        System.out.println("Indexing completed.");
    }

    static void indexDoc(IndexWriter writer, File file) throws IOException {
        try (InputStream stream = Files.newInputStream(file.toPath())) {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String currentLine;
            Document document = new Document();
            String docType = "";
            String docID = ""; // Initialize docID

            while ((currentLine = inputReader.readLine()) != null) {
                // Output the line being processed (for verbosity)
                System.out.println("Processing: " + currentLine);

                if (currentLine.contains(".I")) {
                    // Start of a new document
                    docID = currentLine.split(" ")[1]; // Extract the document ID from the line
                    Field idField = new StringField("id", docID, Field.Store.YES); // Add the document ID to the document
                    document.add(idField);
                    currentLine = inputReader.readLine();

                    while (currentLine != null && !currentLine.startsWith(".I")) {
                        if (currentLine.startsWith(".T")) {
                            docType = "Title";
                        } else if (currentLine.startsWith(".A")) {
                            docType = "Author";
                        } else if (currentLine.startsWith(".W")) {
                            docType = "Words";
                        } else if (currentLine.startsWith(".B")) {
                            docType = "Bibliography";
                        } else {
                            // Add the content of the section to the document
                            document.add(new TextField(docType, currentLine, Field.Store.YES));
                        }
                        currentLine = inputReader.readLine();
                    }

                    // Add the document to the index
                    System.out.println("Adding document ID: " + docID);
                    writer.addDocument(document);
                }
            }
        }
    }
}
