package cranfield;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Indexer {

	// Directory where the index will be saved
	private static String INDEX_DIRECTORY = "../cran_index";

	public static void main(String[] args) throws IOException, ParseException {
		// Indexer method
		indexer();
		
		// Parse the queries for hits
		MyQueryParser.search();
	}

	public static void indexer() throws IOException {
		// Analyser that is used to process TextField
		Analyzer analyzer = new MyAnalyzer();

		// To store the index
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		// Index opening in CREATE mode
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

		IndexWriter iwriter = new IndexWriter(directory, config);

		// Reading from the CRAN data-set
		Path docDir = Paths.get("../cran/cran.all.1400");
		InputStream stream = Files.newInputStream(docDir);
		BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		String currentLine = br.readLine();
		String line = "";
		String docNo[];
		String docType = "";
		int count = 0;

		// Creating documents as per CRAN data-set with pattern matching to 
		// separate the title, author, bibliography and content.
		// Adding each field to the document
		while (currentLine != null) {
			if (currentLine.matches("(\\.I)( )(\\d)*")) {
				Document document = new Document();
				docNo = currentLine.split(" ");
				Field pathField = new StringField("path", docNo[1], Field.Store.YES);
				document.add(pathField);
				currentLine = br.readLine();
				while (currentLine != null && !(currentLine.matches("(\\.I)( )(\\d)*"))) {
					if (currentLine.matches("(\\.T)")) {
						docType = "title";
						currentLine = br.readLine();
						while (!currentLine.matches("(\\.A)")) {
							line += " ";
							line += currentLine;
							currentLine = br.readLine();
						}

						document.add(new TextField(docType, line, Field.Store.YES));
						line = "";
					} else if (currentLine.matches("(\\.A)")) {
						docType = "author";
						currentLine = br.readLine();
						while (!currentLine.matches("(\\.B)")) {
							line += " ";
							line += currentLine;
							currentLine = br.readLine();
						}

						document.add(new TextField(docType, line, Field.Store.YES));
						line = "";
					} else if (currentLine.matches("(\\.B)")) {
						docType = "bibliography";
						currentLine = br.readLine();
						while (!currentLine.matches("(\\.W)")) {
							line += " ";
							line += currentLine;
							currentLine = br.readLine();
						}

						document.add(new TextField(docType, line, Field.Store.YES));
						line = "";
					} else if (currentLine.matches("(\\.W)")) {
						docType = "words";
						currentLine = br.readLine();
						while (currentLine != null && !currentLine.matches("(\\.I)( )(\\d)*")) {
							line += " ";
							line += currentLine;
							currentLine = br.readLine();
						}

						document.add(new TextField(docType, line, Field.Store.YES));
						line = "";
						
						// Save the document to the index
						iwriter.addDocument(document);
						count++;
					}
				}
			}
		}

		System.out.println("Indexing of the documents is complete: " + count + " documents indexed");

		// Close everything
		iwriter.close();
		directory.close();
	}
}