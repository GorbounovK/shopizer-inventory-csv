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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;
import com.salesmanager.shop.model.entity.EntityExists;

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
	private static String baseUrl;

	private static String FILE_NAME; 
	private String langs[] = { "en", "ru" };

	public static void main(String[] args) {
		mProps = PropertyManage.getInstance().getProperties();
		LOG.debug(mProps.toString());

		FILE_NAME = mProps.getProperty("category.file");
		baseUrl = mProps.getProperty("serverAPI.baseUrl");

		CategoryImport categoryImport = new CategoryImport();
		try {
			categoryImport.importCategory();
		} catch (Exception e) {
			// e.printStackTrace();
			LOG.error(e);
		}

	}

	/**
	 * Формирует заголовок API запроса с данными авторизации
	 * 
	 * @return
	 */
	private HttpHeaders getHeader() {
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
		// MediaType.APPLICATION_JSON //for application/json
		headers.setContentType(mediaType);
		// Basic Authentication
		// String authorisation = "admin" + ":" + "Montreal2016!";
		String authorisation = mProps.getProperty("serverAPI.user") + ":" + mProps.getProperty("serverAPI.password");
		byte[] encodedAuthorisation = Base64.encode(authorisation.getBytes());
		headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
		return headers;
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void importCategory() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(';');

		BufferedReader in = new BufferedReader(
				new InputStreamReader(new FileInputStream(FILE_NAME), StandardCharsets.UTF_8));

		// new FileReader(fileName)

//		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in, format);

		Map<String, PersistableCategory> categoryMap = new HashMap<String, PersistableCategory>();

		int i = 0;
		for (CSVRecord record : parser) {
			String code = record.get("code");

			if (StringUtils.isBlank(code)) {
				// пропускаем строки без кода
				LOG.info("Skipping line " + i);
				i++;
				continue;
			}

			// core properties
			PersistableCategory category = new PersistableCategory();
			category.setCode(record.get("code"));
			category.setSortOrder(Integer.parseInt(record.get("position")));
			category.setVisible(Integer.parseInt(record.get("visible")) == 1 ? true : false);

			List<CategoryDescription> descriptions = new ArrayList<CategoryDescription>();

			// add english description
			for (int langLenth = 0; langLenth < langs.length; langLenth++) {
				CategoryDescription description = new CategoryDescription();

				String lang = langs[langLenth];
				description.setLanguage(lang);
				description.setTitle(record.get("title_" + lang));
				description.setName(record.get("name_" + lang));
				description.setDescription(description.getName());
				description.setFriendlyUrl(record.get("friendlyUrl_" + lang));
				description.setHighlights(record.get("highlights_" + lang));

				descriptions.add(description);

				LOG.trace("для строки=" + i + " записано описание для языка=" + lang);
			}
			category.setDescriptions(descriptions);
			LOG.trace("descriptions:" + descriptions.toString());

			categoryMap.put(category.getCode(), category);

			/*
			 * родителю прописываем детей
			 */
			if (!StringUtils.isBlank(record.get("parent"))) {
				PersistableCategory parent = categoryMap.get(record.get("parent"));
				if (parent != null) {
					Category parentCategory = new Category();
					parentCategory.setCode(parent.getCode());
					category.setParent(parentCategory);
					parent.getChildren().add(category);
				}
			}

			LOG.trace("---------------------");
			i++;// rows
		}
		LOG.debug("прочитано " + i + " строк");
		parser.close();

		HttpHeaders httpHeader = getHeader();
		LOG.debug("Свормирован заголовок авторизации");

		// now save each category
		int countCreate = 0;
		int countUpdate = 0;
		for (PersistableCategory category : categoryMap.values()) {

//			if (category.getParent() == null) {// only root category
				HttpEntity<String> entityHeader = new HttpEntity<String>(httpHeader);

				LOG.debug("------выгружаем-------");
				LOG.debug("parent=" + category.getParent());
				ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
				String json = writer.writeValueAsString(category);

				LOG.info(json);

				// проверяем уникальность кода
				LOG.debug("code=" + category.getCode());
				ResponseEntity<EntityExists> responseUnique = restTemplate.exchange(
						baseUrl + "/api/v1/private/category/unique?code=" + category.getCode(), HttpMethod.GET,
						entityHeader, EntityExists.class);

				LOG.debug("status=" + responseUnique.getStatusCodeValue());
				LOG.debug("isExists:" + responseUnique.getBody().isExists());

				if (responseUnique.getBody().isExists()) {
					// категория существует - обновляем
					LOG.debug("категория code=" + category.getCode() + " существует - обновляем:");
					
					countUpdate ++;
				} else {
					// категория не существут - создаем
					LOG.debug("категория code=" + category.getCode() + " не существует - создаем");
					// post to create category web service
					HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);

					ResponseEntity<PersistableCategory> response = restTemplate
							.postForEntity(baseUrl + "/api/v1/private/category", entity, PersistableCategory.class);
//					PersistableCategory cat = (PersistableCategory) response.getBody();
					LOG.debug("Код возврата = " + response.getStatusCodeValue());
					countCreate++;
				}
				LOG.info("-------------- создано="+countCreate+", обновлено="+countUpdate+" ----------------");

//			}

		}

		LOG.info("------------------------------------");
		LOG.info("Category import done");
		LOG.info("------------------------------------");

	}

}
