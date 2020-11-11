/**
 * 
 */
package com.shopizer.inventory.csv.in;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.shopizer.inventory.csv.in.category.CategoryImport;

/**
 * @author gk
 *
 */
public class AppImport {
	private static final Logger LOG = LogManager.getLogger(CategoryImport.class);
    private static Properties mProps;
	private static String baseUrl;
	private static String FILE_NAME; // = "/Users/carlsamson/Documents/dev/workspaces/shopizer-inventoty-xls/shopizer-inventory-csv/src/main/resources/category-loader.csv";

	/**
	 * @param args
	 */
//	public static void main(String[] args) {
//		mProps = PropertyManage.getInstance().getProperties();
//		FILE_NAME = mProps.getProperty("category.file");
//		baseUrl = mProps.getProperty("serverAPI.baseUrl");
//		LOG.debug("FILE_NAME="+FILE_NAME);
//		
//		CategoryImport categoryImport = new CategoryImport();
//		try {
//			categoryImport.importCategory();
//		} catch (Exception e) {
//			//e.printStackTrace();
//			LOG.error(e);
//		}
//
//
//	}

}
