package com.shopizer.inventory.csv.in.category;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

//import com.salesmanager.web.entity.catalog.category.Category;
//import com.salesmanager.web.entity.catalog.category.CategoryDescription;
//import com.salesmanager.web.entity.catalog.category.PersistableCategory;
import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.category.CategoryDescription;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;

import com.shopizer.inventory.csv.in.PropertyManage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CategoryImport {
	private static final Logger LOG = LogManager.getLogger(CategoryImport.class);
    private static Properties mProps;

	private static String FILE_NAME; // = "/Users/carlsamson/Documents/dev/workspaces/shopizer-inventoty-xls/shopizer-inventory-csv/src/main/resources/category-loader.csv";
	

	public static void main(String[] args) {
		mProps = PropertyManage.getInstance().getProperties();
		FILE_NAME = mProps.getProperty("category.file");
		
		CategoryImport categoryImport = new CategoryImport();
		try {
			categoryImport.importCategory();
		} catch (Exception e) {
			//e.printStackTrace();
			LOG.error(e);
		}

	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void importCategory() throws Exception {
		
		RestTemplate restTemplate = new RestTemplate();
		
		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(',');


		
		BufferedReader in = new BufferedReader(
				   new InputStreamReader(
		                      new FileInputStream(FILE_NAME), StandardCharsets.ISO_8859_1));
		
		//new FileReader(fileName)
		
		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in,format);

		Map<String,PersistableCategory> categoryMap = new HashMap<String,PersistableCategory>();

		int i = 0;
		//for (CSVRecord record : records) {
		for(CSVRecord record : parser){
		    //String lastName = record.get("Last Name");
		    //String firstName = record.get("First Name");
			if(StringUtils.isBlank(record.get("code"))) {
				continue;
			}
			
			
			LOG.debug(record.get("code"));
			LOG.debug(record.get("name_en"));
			LOG.debug(record.get("name_fr"));
			LOG.debug(record.get("title_en"));
			LOG.debug(record.get("title_fr"));
			LOG.debug(record.get("friendlyUrl_en"));
			LOG.debug(record.get("friendlyUrl_fr"));
			LOG.debug(record.get("position"));
			LOG.debug(record.get("visible"));
			LOG.debug(record.get("parent"));
			
			//core properties
			PersistableCategory category = new PersistableCategory();
			category.setCode(record.get("code"));
			category.setSortOrder(Integer.parseInt(record.get("position")));
			category.setVisible(Integer.parseInt(record.get("visible"))==1?true:false);
			
			List<CategoryDescription> descriptions = new ArrayList<CategoryDescription>();
			
			//add english description
			CategoryDescription description = new CategoryDescription();
			
			description.setLanguage("en");
			description.setTitle(record.get("title_en"));
			description.setName(record.get("name_en"));
			description.setDescription(description.getName());
			description.setFriendlyUrl(record.get("friendlyUrl_en"));
			description.setHighlights(record.get("highlights_en"));
			
			descriptions.add(description);
			
			//add french description
			description = new CategoryDescription();
			description.setLanguage("fr");
			description.setTitle(record.get("title_fr"));
			description.setName(record.get("name_fr"));
			description.setDescription(description.getName());
			description.setFriendlyUrl(record.get("friendlyUrl_fr"));
			description.setHighlights(record.get("highlights_fr"));
			
			descriptions.add(description);
			category.setDescriptions(descriptions);
			
			categoryMap.put(category.getCode(), category);
			
			if(!StringUtils.isBlank(record.get("parent"))) {
				PersistableCategory parent = categoryMap.get(record.get("parent"));
				if(parent!=null) {
					Category parentCategory = new Category();
					parentCategory.setCode(parent.getCode());
					category.setParent(parentCategory);
					parent.getChildren().add(category);
				}
			}
			
			
			
			LOG.info("---------------------");
			i++;//rows
		}
		
		HttpHeaders httpHeader = getHeader();
		
		//now save each category
		for(PersistableCategory category : categoryMap.values()) {
			
			if(category.getParent()==null) {//only root category
			
				ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
				String json = writer.writeValueAsString(category);
				
				System.out.println(json);
				
				
				HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);
	
				//post to create category web service
				ResponseEntity response = restTemplate.postForEntity("http://www.exotikmobilier.com/services/private/DEFAULT/category", entity, PersistableCategory.class);
				PersistableCategory cat = (PersistableCategory) response.getBody();
				
			}
			
		}
		
     
		LOG.info("------------------------------------");
		LOG.info("Category import done");
		LOG.info("------------------------------------");
		
	}
	
	/**
	 * Формирует заголовок API запроса с данными авторизации
	 * @return
	 */
	private HttpHeaders getHeader(){
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
		//MediaType.APPLICATION_JSON //for application/json
		headers.setContentType(mediaType);
		//Basic Authentication
		String authorisation = "admin" + ":" + "Montreal2016!";
		byte[] encodedAuthorisation = Base64.encode(authorisation.getBytes());
		headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
		return headers;
	}

}
