package it.unibo.cs.Frnet2RDF.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

	public static List<String> getFilesUnderTreeRec(String filePath) {
		List<String> result = new ArrayList<String>();

		File f = new File(filePath);
		for (File child : f.listFiles()) {
			if (child.isDirectory()) {
				result.addAll(getFilesUnderTreeRec(child.getAbsolutePath()));
			} else {
				result.add(child.getAbsolutePath());
			}
		}

		return result;
	}

	public static long countNumberOfLines(String filename) {
		long n = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			while (br.readLine() != null) {
				n++;
			}
			System.out.println("Number of lines of " + filename + " : " + n);
			br.close();
		} catch (IOException e) {

		}
		return n;
	}

	public static String toTextFile(String input, String filePath) throws IOException {

		File f = new File(filePath);
		Writer outputStreamWriter = new OutputStreamWriter(new FileOutputStream(f));

		outputStreamWriter.write(input);
		outputStreamWriter.flush();
		outputStreamWriter.close();

		return f.getAbsolutePath();

	}

	public static void toTextFile(List<String> rows, String filePath) throws IOException {

		File f = new File(filePath);
		Writer outputStreamWriter = new OutputStreamWriter(new FileOutputStream(f));
		for (String r : rows) {
			outputStreamWriter.write(r + "\n");
		}
		outputStreamWriter.flush();
		outputStreamWriter.close();

	}

	public static boolean deleteFile(String filePath) {
		File file = new File(filePath);
		return file.delete();
	}

	public static void deleteFoler(String path) throws IOException {

		File file = new File(path);

		if (file.isDirectory()) {

			// directory is empty, then delete it
			if (file.list().length == 0) {

				file.delete();

			} else {

				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					deleteFoler(fileDelete.getAbsolutePath());
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
				}
			}

		} else {
			// if file, then delete it
			file.delete();
		}
	}

	public static String readFile(String filename) {
		return readFile(filename, false);
	}

	public static String readFile(String filename, boolean keepCR) {
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line);
				if (keepCR)
					sb.append('\n');
				line = br.readLine();

			}
			result = sb.toString();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static List<String> readFileToListString(String filename) {
		List<String> result = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				result.add(line);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void main(String[] args) {
		countNumberOfLines("/Volumes/VERBATIM HD/work/BN/aloof/wpl.nt");
	}

}
