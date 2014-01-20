package org.bbbmanager.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

abstract public class FileUtils {
	public static String getFileContents(File f){
		try {
			StringBuilder sb = new StringBuilder();
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			
			String line = null;
			if( (line = br.readLine() ) != null){
				sb.append(line);
			}
			
			fr.close();
			br.close();
			
			return sb.toString();
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}
}
