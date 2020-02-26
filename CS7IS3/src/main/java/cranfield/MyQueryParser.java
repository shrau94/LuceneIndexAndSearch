package cranfield;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BooleanSimilarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;

public class MyQueryParser {

	// the location of the index
	private static String INDEX_DIRECTORY = "../cran_index";

	// Limiting the number of search results
	private static int MAX_RESULTS = 1400;
	
	// Path for the file with all the queries
	private static String QUERIES_PATH = "../cran/cran.qry";
	
	// Path of the result of query
	private static String RESULT_PATH = "../qry.result";
	
	
	
	public static void search() throws IOException, ParseException {

		Analyzer analyzer = new MyAnalyzer();
		OutputStream os = null;
		
		// Emptying the file contents if it is already filled with values
		os = new FileOutputStream(new File(RESULT_PATH));
		String emptydata = "";
		os.write(emptydata.getBytes(), 0, emptydata.length());

		// Open the folder that contains our search index
		Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

		// Create objects to read and search across the index
		DirectoryReader ireader = DirectoryReader.open(directory);
		
		// Creating searcher to search across index
		IndexSearcher isearcher = new IndexSearcher(ireader);
		
		// Setting similarity metrics for the searcher
		isearcher.setSimilarity(new BM25Similarity());
//		isearcher.setSimilarity(new ClassicSimilarity());
//		isearcher.setSimilarity(new BooleanSimilarity());

		// Creating the  parser and adding "title", "author", "bibliography", "words"
		MultiFieldQueryParser parser = new MultiFieldQueryParser(
				new String[] { "title", "author", "bibliography", "words" }, analyzer);

		// Parsing all the queries in the given file
		String queryString = "";
		Path docDir = Paths.get(QUERIES_PATH);
		InputStream stream = Files.newInputStream(docDir);
		BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
		String queryLine = br.readLine();
		String line = "";
		int count = 0;
		while (queryLine != null) {
			if (queryLine.matches("(\\.I)( )(\\d)*")) {
				count++;
				line = "";
				queryLine = br.readLine();
				if (queryLine.matches("(\\.W)")) {
					queryLine = br.readLine();
					while (queryLine != null && !(queryLine.matches("(\\.I)( )(\\d)*"))) {
						line += " ";
						line += queryLine;
						queryLine = br.readLine();
					}
					if (queryLine != null) {
						queryString = line.trim();
						if (queryString.length() > 0) {
							
							// parse the query with the parser
							Query query = parser.parse(QueryParser.escape(queryString));

							// Get the set of results
							ScoreDoc[] hits = isearcher.search(query, MAX_RESULTS).scoreDocs;

							// Writing the results in a file
							os = new FileOutputStream(new File(RESULT_PATH), true);
							for (int i = 0; i < hits.length; i++) {
								Document hitDoc = isearcher.doc(hits[i].doc);
								String data = count + " 0 " + hitDoc.get("path") + " 0 " + hits[i].score + " STANDARD"
										+ "\n";
								os.write(data.getBytes(), 0, data.length());
							}
						}
					}
				}
			}
		}
		queryString = line.trim();
		if (queryString.length() > 0) {
			Query query = parser.parse(QueryParser.escape(queryString));

			ScoreDoc[] hits = isearcher.search(query, MAX_RESULTS).scoreDocs;

			os = new FileOutputStream(new File(RESULT_PATH), true);
			
			for (int i = 0; i < hits.length; i++) {
				Document hitDoc = isearcher.doc(hits[i].doc);
				String data = count + " 0 " + hitDoc.get("path") + " 0 " + hits[i].score + " STANDARD" + "\n";
				os.write(data.getBytes(), 0, data.length());
			}
		}
		
		System.out.println("Query parsing is complete: " + count + " queries parsed");
		System.out.println("Results generated at " + RESULT_PATH);
		
		// Closing everything
		os.close();
		ireader.close();
		directory.close();
	}
}
