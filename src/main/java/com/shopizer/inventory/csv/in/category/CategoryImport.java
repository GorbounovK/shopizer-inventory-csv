package com.shopizer.inventory.csv.in.category;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.salesmanager.shop.model.entity.EntityExists;
import com.github.slugify.Slugify;
import com.ibm.icu.text.Transliterator;
//import com.salesmanager.web.entity.catalog.category.Category;
//import com.salesmanager.web.entity.catalog.category.CategoryDescription;
//import com.salesmanager.web.entity.catalog.category.PersistableCategory;
import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.category.CategoryDescription;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.model.catalog.category.ReadableCategoryList;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class CategoryImport {
//	private static final Logger LOG = LogManager.getLogger(CategoryImport.class);
//	private static Properties mProps;
	@Value("${serverAPI.baseUrl}")
	private String baseUrl;
	@Value("${serverAPI.user}")
	private String serverAPIUser;
	@Value("${serverAPI.password}")
	private String serverAPIPassword;

	@Value("${category.file}")
	private static String FILE_NAME;
	private String langs[] = { "en", "ru" };

	public void main(String[] args) {
//		mProps = PropertyManage.getInstance().getProperties();
		log.trace("baseUrl=" + baseUrl + ";" + "serverAPIUser=" + serverAPIUser);

//		FILE_NAME = mProps.getProperty("category.file");
//		baseUrl = mProps.getProperty("serverAPI.baseUrl");

		CategoryImport categoryImport = new CategoryImport();
		try {
			categoryImport.importCategory();
		} catch (Exception e) {
			// e.printStackTrace();
			log.error(e);
		}

	}

	/**
	 * Формирует заголовок API запроса с данными авторизации
	 * 
	 * @return
	 */
	private HttpHeaders getHeader() {
//		{
//			  "password": "password",
//			  "username": "admin@shopizer.com"
//			}
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
		// MediaType.APPLICATION_JSON //for application/json
		headers.setContentType(mediaType);
		// Basic Authentication
		// String authorisation = "admin" + ":" + "Montreal2016!";
//		String authorisation = mProps.getProperty("serverAPI.user") + ":" + mProps.getProperty("serverAPI.password");
		String authorisation = serverAPIUser + ":" + serverAPIPassword;

		byte[] encodedAuthorisation = Base64.getEncoder().encode(authorisation.getBytes());
		headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
		return headers;
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void importCategory() throws Exception {

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
				log.info("Skipping line " + i + " пустой код");
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

				log.trace("для строки=" + i + " записано описание для языка=" + lang);
			}
			category.setDescriptions(descriptions);
			log.trace("descriptions:" + descriptions.toString());

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

			log.trace("---------------------");
			i++;// rows
		}
		log.debug("прочитано " + i + " строк");
		parser.close();

//		HttpHeaders httpHeader = getHeader();
//		log.trace("Свормирован заголовок авторизации");
//
//		// now save each category
//		int countCreate = 0;
//		int countUpdate = 0;
//		for (PersistableCategory category : categoryMap.values()) {
//
////			if (category.getParent() == null) {// only root category
//				HttpEntity<String> entityHeader = new HttpEntity<String>(httpHeader);
//
//				log.debug("------выгружаем-------");
//				log.debug("parent=" + category.getParent());
//				ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
//				String json = writer.writeValueAsString(category);
//
//				log.trace(json);
//
//				// проверяем уникальность кода
//				log.debug("code=" + category.getCode());
//				ResponseEntity<EntityExists> responseUnique = restTemplate.exchange(
//						baseUrl + "/api/v1/private/category/unique?code=" + category.getCode(), HttpMethod.GET,
//						entityHeader, EntityExists.class);
//
//				log.debug("status=" + responseUnique.getStatusCodeValue());
//				log.debug("isExists:" + responseUnique.getBody().isExists());
//
//				if (responseUnique.getBody().isExists()) {
//					// категория существует - обновляем
//					log.debug("категория code=" + category.getCode() + " существует - обновляем:");
//					updateCategory(category);
//					countUpdate ++;
//				} else {
//					// категория не существут - создаем
//					log.debug("категория code=" + category.getCode() + " не существует - создаем");
//					// post to create category web service
//					HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);
//
//					ResponseEntity<PersistableCategory> response = restTemplate
//							.postForEntity(baseUrl + "/api/v1/private/category", entity, PersistableCategory.class);
////					PersistableCategory cat = (PersistableCategory) response.getBody();
//					log.debug("Код возврата = " + response.getStatusCodeValue());
//					countCreate++;
//				}
//				log.info("-------------- создано="+countCreate+", обновлено="+countUpdate+" ----------------");
//
////			}
//
//		}
		postCategoryToApiServer(categoryMap);
		log.info("------------------------------------");
		log.info("Category import done");
		log.info("------------------------------------");

	}

	public void postCategoryToApiServer(Map<String, PersistableCategory> categoryMap) {
		RestTemplate restTemplate = new RestTemplate();
		try {

			HttpHeaders httpHeader = getHeader();
			log.trace("Свормирован заголовок авторизации");

			// now save each category
			int countCreate = 0;
			int countUpdate = 0;
			for (PersistableCategory category : categoryMap.values()) {

//			if (category.getParent() == null) {// only root category
				HttpEntity<String> entityHeader = new HttpEntity<String>(httpHeader);

				log.debug("------выгружаем-------");
				log.debug("parent=" + category.getParent());
				ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
				String json;
				json = writer.writeValueAsString(category);

				log.trace(json);

				// проверяем уникальность кода
				log.debug("code=" + category.getCode());
				ResponseEntity<EntityExists> responseUnique = restTemplate.exchange(
						baseUrl + "/api/v1/private/category/unique?code=" + category.getCode(), HttpMethod.GET,
						entityHeader, EntityExists.class);

				log.debug("StatusCode=" + responseUnique.getStatusCodeValue());
				log.debug("isExists:" + responseUnique.getBody().isExists());

				if (responseUnique.getBody().isExists()) {
					// категория существует - обновляем
					log.debug("категория code=" + category.getCode() + " существует - обновляем:");
					updateCategory(category);
					countUpdate++;
				} else {
					// категория не существут - создаем
					log.debug("категория code=" + category.getCode() + " не существует - создаем");
					// post to create category web service
					HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);

					ResponseEntity<PersistableCategory> response = restTemplate
							.postForEntity(baseUrl + "/api/v1/private/category", entity, PersistableCategory.class);
//					PersistableCategory cat = (PersistableCategory) response.getBody();
					log.debug("Код возврата = " + response.getStatusCodeValue());
					countCreate++;
				}
				log.info("-------------- создано=" + countCreate + ", обновлено=" + countUpdate + " ----------------");

//			}

			}
		} catch (IOException e) {
			log.error("error", e);
		}
	}

	/**
	 * 
	 */
	public void importCategoryFromXML() {
		try {
			File fXmlFile = new File("export_prom.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();
			log.trace("Root :" + doc.getDocumentElement().getNodeName());

			Map<String, PersistableCategory> categoryMap = new HashMap<String, PersistableCategory>();

			NodeList nList = doc.getElementsByTagName("category");
			log.trace("Прочитано " + nList.getLength() + " категорий");
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);

//				log.trace("Category = " + nNode.getNodeName());

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					log.trace("---------------------------");
					log.trace("id = " + eElement.getAttribute("id"));
					log.trace("parentId = " + eElement.getAttribute("parentId"));
					log.trace("Наименование = " + nNode.getTextContent());

					// core properties
					PersistableCategory category = new PersistableCategory();
					category.setCode(eElement.getAttribute("id"));
					category.setSortOrder(i * 10);
					category.setVisible(true);

					List<CategoryDescription> descriptions = new ArrayList<CategoryDescription>();

					// add english description
					for (int langLenth = 0; langLenth < langs.length; langLenth++) {
						CategoryDescription description = new CategoryDescription();

						String lang = langs[langLenth];
						description.setLanguage(lang);
						String enName = minimalFriendlyNameCreator(nNode.getTextContent());
						description.setFriendlyUrl(minimalFriendlyUrlCreator(enName, eElement.getAttribute("id")));
						description.setHighlights("0");
						if (lang == "ru") {
							description.setTitle(nNode.getTextContent());
							description.setName(nNode.getTextContent());
							description.setDescription(description.getName());
						} else if (lang == "en") {
							description.setTitle(enName);
							description.setName(enName);
							description.setDescription(description.getName());
						}

						descriptions.add(description);
//						log.trace("description:" + description.toString());

						log.trace("для id = " + i + " записано описание для языка=" + lang);
					}
					category.setDescriptions(descriptions);


					/*
					 * родителю прописываем детей
					 */
					if (!StringUtils.isBlank(eElement.getAttribute("parentId"))) {
						log.trace("parentId=" + eElement.getAttribute("parentId"));
						PersistableCategory parent = categoryMap.get(eElement.getAttribute("parentId"));
						if (parent != null) {
							Category parentCategory = new Category();
							parentCategory.setCode(parent.getCode());
							category.setParent(parentCategory);
							parent.getChildren().add(category);
							log.trace("Родителю=" + parent.getCode() + " добавлен ребенок=" + category.getCode());
						}
					}
					log.trace("---------------------");
					categoryMap.put(category.getCode(), category);

				}
			} // цикл по категориям

			postCategoryToApiServer(categoryMap);
			log.info("------------------------------------");
			log.info("Category import done");
			log.info("------------------------------------");

		} catch (Exception e) {
			log.error("error", e);
		}
	}

	/**
	 * 
	 * @param mCategory
	 */
	private void updateCategory(PersistableCategory mCategory) {
		log.debug(mCategory);
	}

	/**
	 * 
	 * @return Список всех категорий
	 */
	public List<ReadableCategory> listCategory() {
		log.trace("------ service listCategory ------");
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders httpHeader = getHeader();
		HttpEntity<String> entityHeader = new HttpEntity<String>(httpHeader);
		ResponseEntity<ReadableCategoryList> responseUnique = restTemplate.exchange(
				baseUrl + "/api/v1/category?count=200&lang=ru", HttpMethod.GET, entityHeader,
				ReadableCategoryList.class);
		log.trace("StatusCode=" + responseUnique.getStatusCode());
		log.trace("RecordsTotal=" + responseUnique.getBody().getRecordsTotal());
		return responseUnique.getBody().getCategories();
	}

	/**
	 * 
	 * @return Список категорий от указанного родителя
	 */
	public ReadableCategory listCategoryById(int id) {
		log.debug("------ service listCategoryById ------");
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders httpHeader = getHeader();
		HttpEntity<String> entityHeader = new HttpEntity<String>(httpHeader);
		ResponseEntity<ReadableCategory> responseUnique = restTemplate.exchange(
				baseUrl + "/api/v1/category/"+id+"?lang=ru", HttpMethod.GET, entityHeader,
				ReadableCategory.class);
		log.trace("Url=" + "/api/v1/category/"+id+"?lang=ru");
		log.trace("StatusCode=" + responseUnique.getStatusCode());
		log.trace("RecordsTotal=" + responseUnique.getBody().toString());
		return responseUnique.getBody();
	}

	/**
	 * Удалает категорию по id
	 * 
	 * @param id
	 */
	public void deleteCategory(int id) {
		log.trace("------ service deleteCategory ------");
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders httpHeader = getHeader();
		HttpEntity<String> entityHeader = new HttpEntity<String>(httpHeader);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/api/v1/private/category/" + id,
				HttpMethod.DELETE, entityHeader, String.class);
		log.trace("StatusCode=" + response.getStatusCode());
		log.trace("------ service deleteCategory complete------");

	}

	/**
	 * 
	 * @param productName
	 * @param code
	 * @return
	 */
	public String minimalFriendlyUrlCreator(String name, String code) {
		log.trace("url=" + "c" + code + "-" + minimalFriendlyNameCreator(name));
		return "c" + code + "-" + minimalFriendlyNameCreator(name);
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public String minimalFriendlyNameCreator(String name) {
		String pName = name.toLowerCase();
		pName = pName.replace("  ", " ");

		// remove accents
		pName = Normalizer.normalize(pName, Normalizer.Form.NFD);
		pName = pName.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

		// разбираем строку на слова
		Transliterator ruToLatin = Transliterator.getInstance("Russian-Latin/BGN");
		String result = ruToLatin.transform(pName);

		Slugify slg = new Slugify();
//		log.trace("Slugify=" + slg.slugify(result));
		return slg.slugify(result);
	}
}
