package ca.IR;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

        // Open directory for index storage
        Directory dir = FSDirectory.open(Paths.get(indexPath));
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(dir, config);

        // Index each document in the specified directory
        File[] files = docsDir.listFiles();
        if (files != null) {
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
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            String docID = null;
            StringBuilder title = new StringBuilder();
            StringBuilder author = new StringBuilder();
            StringBuilder content = new StringBuilder();
            boolean isContent = false;  // Flag to indicate if we're reading content

            while ((line = br.readLine()) != null) {
                if (line.startsWith(".I")) {
                    if (docID != null) {
                        // Add the previous document before starting a new one
                        addDocument(writer, docID, title.toString(), author.toString(), content.toString());
                        content.setLength(0); // Clear content for new document
                        title.setLength(0); // Clear title for new document
                        author.setLength(0); // Clear author for new document
                    }
                    // Extract document ID (number after .I)
                    docID = line.substring(3).trim(); // Capture the number after .I
                } else if (line.startsWith(".T")) {
                    // Read title lines
                    line = br.readLine(); // Read the next line for title content
                    while (line != null && !line.startsWith(".")) {
                        title.append(line).append(" ");
                        line = br.readLine();
                    }
                } else if (line.startsWith(".A")) {
                    // Read author lines
                    line = br.readLine(); // Read the next line for author content
                    while (line != null && !line.startsWith(".")) {
                        author.append(line).append(" ");
                        line = br.readLine();
                    }
                } else if (line.startsWith(".W")) {
                    // Content starts after .W
                    isContent = true;
                    continue; // Skip this line
                }

                // Append content lines if we are in the content section
                if (isContent) {
                    content.append(line).append(" "); // Append line to content
                }
            }

            if (docID != null) {
                // Add the last document after exiting the loop
                addDocument(writer, docID, title.toString(), author.toString(), content.toString());
            }
        }
    }

    private static void addDocument(IndexWriter writer, String docID, String title, String author, String textContent) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("title", title, Field.Store.YES)); // Store title
        doc.add(new TextField("author", author, Field.Store.YES)); // Store author
        doc.add(new TextField("contents", textContent, Field.Store.YES)); // Store main content
        doc.add(new TextField("documentID", docID, Field.Store.YES)); // Use the extracted docID
        writer.addDocument(doc);
    }
}
