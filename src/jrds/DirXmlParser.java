package jrds;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.apache.commons.digester.Digester;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public abstract class DirXmlParser {
	private final Logger logger = Logger.getLogger(DescFactory.class);
	final Pattern p = Pattern.compile(".*.xml");
	final FileFilter filter = new  FileFilter(){
		public boolean accept(File file) {
			if(! file.isHidden()) {
				if(file.isDirectory())
					importDir(file);
				else if(file.isFile() && file.getName().endsWith(".xml")) {
					try {
						InputStream xmlStream = new FileInputStream(file);
						digester.parse(xmlStream);
						xmlStream.close();
					} catch (FileNotFoundException e) {
						logger.error("File  "+ file + " cannot be read: " + e);
					} catch (IOException e) {
						logger.error("File  "+ file + " cannot be read: " + e);
					} catch (SAXException e) {
						logger.error("File  "+ file + " not parsable: " + e, e);
					}
				}
			}
			return  false;
		}
	};
	Digester digester = new Digester() {
		@Override
		public void error(SAXParseException exception) throws SAXException {
	        /*logger.error("Parse Error at line " + exception.getLineNumber() +
	                " column " + exception.getColumnNumber() + ": " +
	                exception.getMessage() + " for " + getXMLReader());*/
			throw exception;
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
	        log.error("Parse Fatal Error at line " + exception.getLineNumber() +
	                " column " + exception.getColumnNumber() + ": " +
	                exception.getMessage(), exception);
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException {
            log.warn("Parse Warning Error at line " + exception.getLineNumber() +
                    " column " + exception.getColumnNumber() + ": " +
                    exception.getMessage(), exception);
		}
		
	};

	DirXmlParser() {
		init();
	}

	abstract void init();

	public void importDescUrl(URL ressourceUrl) throws IOException {
		String path = ressourceUrl.toString();
		if(path != null) {
		String [] urlelems = path.split("[:!]");
		if("file".equals(urlelems[0]))
			importDir(new File(urlelems[1]));
		else if("jar".equals(urlelems[0]))
			importJar(urlelems[2]);
		}
		else 
			logger.error("ressource " + ressourceUrl + "can't be loaded" );
	}

	/**
	 * Recursively walk a directory tree and return a List of all
	 * Files found; the List is sorted using File.compareTo.
	 *
	 * @param aStartingDir is a valid directory, which can be read.
	 */
	public void importDir(File filePath) {
		if(filePath.isDirectory()) 
			filePath.listFiles(filter);
	}

	public void importJar(String jarfile) throws IOException {
		JarFile probesjar;
		URL jarUrl = new URL("file:" + jarfile);
		probesjar = new JarFile(jarfile);
		Enumeration e = probesjar.entries();
		while(e.hasMoreElements()) {
			ZipEntry z = (ZipEntry) e.nextElement();
			if( !z.isDirectory() && z.getName().endsWith(".xml")) {
				InputStream xmlStream = probesjar.getInputStream(z);
				try {
					digester.parse(xmlStream);
				} catch (SAXParseException e1) {
					logger.error(jarUrl + "!/" + z + ": " +
			        "Parse Error at line " + e1.getLineNumber() +
	                " column " + e1.getColumnNumber() + ": " +
	                e1.getMessage());
					digester.clear();
				} catch (SAXException e1) {
					e1.printStackTrace();
				}
				xmlStream.close();
			}

		}
	}

}