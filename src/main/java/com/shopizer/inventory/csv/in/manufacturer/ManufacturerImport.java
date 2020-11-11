package com.shopizer.inventory.csv.in.manufacturer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

//import com.salesmanager.web.entity.catalog.manufacturer.ManufacturerDescription;
//import com.salesmanager.web.entity.catalog.manufacturer.PersistableManufacturer;
import com.salesmanager.shop.model.catalog.manufacturer.ManufacturerDescription;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;
import com.salesmanager.shop.model.entity.EntityExists;
import com.shopizer.inventory.csv.in.PropertyManage;
import com.shopizer.inventory.csv.in.category.CategoryImport;

public class ManufacturerImport {

//	private String FILE_NAME = "/Users/carlsamson/Documents/dev/workspaces/shopizer-inventoty-xls/shopizer-inventory-csv/src/main/resources/manufacturer-loader.csv";
	private static final Logger LOG = LogManager.getLogger(ManufacturerImport.class);
	private static Properties mProps;
	private static String baseUrl;

	private static String FILE_NAME;
	private String langs[] = { "en", "ru" };

	public static void main(String[] args) {

		mProps = PropertyManage.getInstance().getProperties();
		LOG.debug(mProps.toString());

		FILE_NAME = mProps.getProperty("manufacturer.file");
		baseUrl = mProps.getProperty("serverAPI.baseUrl");

		ManufacturerImport manufacturerImport = new ManufacturerImport();
		try {
			manufacturerImport.importManufacturer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @throws Exception
	 */
	public void importManufacturer() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(';');

		BufferedReader in = new BufferedReader(
				new InputStreamReader(new FileInputStream(FILE_NAME), StandardCharsets.UTF_8));

		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in, format);

		HttpHeaders httpHeader = getHeader();

		int i = 0;
		int countCreate = 0;
		int countUpdate = 0;

		for (CSVRecord record : parser) {

			if (StringUtils.isBlank(record.get("code"))) {
				continue;
			}

			// core properties
			PersistableManufacturer manufacturer = new PersistableManufacturer();
			manufacturer.setCode(record.get("code"));

			List<ManufacturerDescription> descriptions = new ArrayList<ManufacturerDescription>();

			for (int langLenth = 0; langLenth < langs.length; langLenth++) {

				ManufacturerDescription description = new ManufacturerDescription();
				String lang = langs[langLenth];
				description.setLanguage(lang);
				description.setTitle(record.get("title_" + lang));
				description.setName(record.get("name_" + lang));
				description.setDescription(description.getName());
				description.setFriendlyUrl(record.get("friendly_url_" + lang));

				descriptions.add(description);

//				// add french description
//				description = new ManufacturerDescription();
//				description.setLanguage("fr");
//				description.setTitle(record.get("title_fr"));
//				description.setName(record.get("name_fr"));
//				description.setDescription(description.getName());
//				description.setFriendlyUrl(record.get("friendly_url_fr"));
//
//				descriptions.add(description);
				LOG.trace("для строки=" + i + " записано описание для языка=" + lang);

			}
			manufacturer.setDescriptions(descriptions);

			ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
			String json = writer.writeValueAsString(manufacturer);

			LOG.info(json);

			// проверяем уникальность кода
			HttpEntity<String> entityHeader = new HttpEntity<String>(httpHeader);
			LOG.debug("code=" + manufacturer.getCode());
			ResponseEntity<EntityExists> responseUnique = restTemplate.exchange(
					baseUrl + "/api/v1/private/manufacturer/unique?code=" + manufacturer.getCode(), HttpMethod.GET,
					entityHeader, EntityExists.class);

			LOG.debug("status=" + responseUnique.getStatusCodeValue());
			LOG.debug("isExists:" + responseUnique.getBody().isExists());

			if(responseUnique.getBody().isExists()) {
				//существует - обновить
				LOG.debug("Бренд code=" + manufacturer.getCode() + " существует - обновляем:");
				countUpdate ++;
				
			}else {
				// создать новый бренд
				LOG.debug("Бренд code=" + manufacturer.getCode() + " не существует - создаем");
				HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);
				// post to create category web service
				ResponseEntity<PersistableManufacturer> response = restTemplate.postForEntity(
						baseUrl + "/api/v1/private/manufacturer", entity,
						PersistableManufacturer.class);
//				PersistableManufacturer manuf = (PersistableManufacturer) response.getBody();
				LOG.debug("Код возврата = " + response.getStatusCodeValue());
				countCreate++;
	
			}
			LOG.info("--------------Бренды: создано="+countCreate+", обновлено="+countUpdate+" ----------------");

			System.out.println("--------------------- " + i);
		}

		System.out.println("------------------------------------");
		System.out.println("Manufacturer import done");
		System.out.println("------------------------------------");

	}

	private HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
		headers.setContentType(mediaType);
		// Basic Authentication
		String authorisation = mProps.getProperty("serverAPI.user") + ":" + mProps.getProperty("serverAPI.password");
		byte[] encodedAuthorisation = Base64.encode(authorisation.getBytes());
		headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
		return headers;
	}

}
