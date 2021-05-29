package com.shopizer.inventory.csv.in.product;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.github.slugify.Slugify;
import com.ibm.icu.text.Transliterator;
import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.product.PersistableImage;
import com.salesmanager.shop.model.catalog.product.PersistableProduct;
import com.salesmanager.shop.model.catalog.product.PersistableProductPrice;
import com.salesmanager.shop.model.catalog.product.ProductDescription;
import com.salesmanager.shop.model.catalog.product.ProductSpecification;
import com.salesmanager.shop.model.entity.EntityExists;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class ProductImport {

	private Map<String, Integer> skuMap = new HashMap<String, Integer>();

	/**
	 * Fields used in xls file with Product mapping
	 * 
	 * barcode sku image image_file name_<lang> description_<lang> quantity
	 * collection category height width length weight price deal -> % discounted
	 * pre-order dimensions -> CM | IN (centimeters or inches) position -> placement
	 * in listing page (ordering) import -> yes | no
	 * 
	 * NEED JAVA 1.8
	 */

	/**
	 * Supported languages CSV template must contain name_<language> and
	 * description<language>
	 */
//	private static  Logger LOG;// = LogManager.getLogger(ProductImport.class);
	
	
	private static Properties mProps;
	private static String baseUrl;

	private static String FILE_NAME;
	private static String[] langs; // = { "en", "ru" };

	private HttpHeaders httpHeader;

	// private String baseUrl =
	// "http://ec2-35-182-159-217.ca-central-1.compute.amazonaws.com:8080";
//	private String baseUrl = "http://localhost:8080";

	private boolean DRY_RUN = false;

	private final int MAX_COUNT = 6;

	/**
	 * Where to find images
	 */
	private static String IMAGE_BASE_DIR; // = "/Users/carlsamson/Documents/csti/projects-proposals/bam/BAM-excel/";

	/**
	 * where to find csv
	 */
	// private String FILE_NAME =
	// "/Users/carlsamson/Documents/dev/workspaces/shopizer-tools/tools/src/main/resources/2017-01-25.csv";
	// private String FILE_NAME =
	// "/Users/carlsamson/Documents/csti/shopizer/Revamp-2.5/Shopizer-demo/products-import.csv";
//	private String FILE_NAME = "/Users/carlsamson/Documents/csti/projects-proposals/bam/BAM-excel/BAM-import_list_02.csv";
	// private String FILE_NAME =
	// "/Users/carlsamson/Documents/dev/workspaces/shopizer-tools/tools/src/main/resources/BAM-import.csv";

	private int iterationCount = 1;

//	public static void main(String[] args) {
//		log =  LogManager.getLogger(ProductImport.class);
//
//		mProps = PropertyManage.getInstance().getProperties();
//		LOG.debug(mProps.toString());
//
//		FILE_NAME = mProps.getProperty("product.file");
//		baseUrl = mProps.getProperty("serverAPI.baseUrl");
//		IMAGE_BASE_DIR = mProps.getProperty("product.image_base_dir");
//		langs = mProps.getProperty("language").trim().split(",");
//		LOG.debug(Arrays.toString(langs));
//
//		ProductImport productsImport = new ProductImport();
//		try {
//			productsImport.importProducts();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	public void importProducts() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		CSVFormat format = CSVFormat.EXCEL.withHeader().withDelimiter(';');

		/*
		 * FileInputStream is = new FileInputStream(FILE_NAME); InputStreamReader isr =
		 * new InputStreamReader(is,StandardCharsets.UTF_8); int data = isr.read();
		 * while(data != -1) { System.out.print((char) data); data = isr.read(); }
		 */

		BufferedReader in = new BufferedReader(
				new InputStreamReader(new FileInputStream(FILE_NAME), StandardCharsets.UTF_8));

		@SuppressWarnings("resource")
		CSVParser parser = new CSVParser(in, format);

		HttpHeaders httpHeader = getHeader();

		int i = 0;
		int count = 0;
		int countCreate = 0;
		int countUpdate = 0;
		for (CSVRecord record : parser) {

			String code = record.get("sku");

			if (StringUtils.isBlank(code)) {
				log.debug("Skipping code " + i);
				i++;
				continue;
			}

			String barcode = record.get("barcode");

			if (StringUtils.isBlank(barcode)) {
				barcode = this.alternativeIdentifier(record);
				if (StringUtils.isBlank(barcode)) {
					log.debug("Skipping barcode " + i);
					i++;
					continue;
				}

			}

			String imp = record.get("import");// import this row ? yes|no
			log.info("Import status [" + imp + "]");

			if (StringUtils.isBlank(imp) || "no".equals(imp)) {
				log.info("Skipping import " + i);
				i++;
				continue;
			}

			code = uniqueSku(code);

			String categoryCode = record.get("category");
			String price = record.get("price");

			String orderString = record.get("position");
			if (StringUtils.isBlank(orderString)) {
				orderString = String.valueOf(i);
			}

			int order = Integer.parseInt(orderString);

			if (StringUtils.isBlank(categoryCode)) {
				log.info("No category, skipping " + code);
				continue;
			}

			if (StringUtils.isBlank(price)) {
				log.info("No price, skipping " + code);
				continue;
			} else {
				// int randomPrice = this.randPrice(150, 300);//when there is no price
			}

			String stringQuantity = record.get("quantity");
			if (StringUtils.isBlank(stringQuantity)) {
				stringQuantity = "1";
			}
			int quantity = Integer.parseInt(stringQuantity);

			price = price.replaceAll(",", ".").trim();

			BigDecimal productPrice = new BigDecimal(price);

			Category category = new Category();
			category.setCode(categoryCode);
			List<Category> categories = new ArrayList<Category>();
			categories.add(category);

			/** specification **/
			String manufacturer = record.get("collection");// brand - manufacturer ...
			ProductSpecification specs = new ProductSpecification();
			specs.setManufacturer(manufacturer);

			// sizes are required when loading a product
			String dimensions = record.get("dimensions");

//			String W = convertDimension(record.get("width"),dimensions).toString();
//			String L = convertDimension(record.get("length"),dimensions).toString();
//			String H = convertDimension(record.get("length"),dimensions).toString();

			specs.setHeight(convertDimension(record.get("height"), dimensions));
			specs.setWidth(convertDimension(record.get("width"), dimensions));
			specs.setLength(convertDimension(record.get("length"), dimensions));
			specs.setWeight(convertWeight(record.get("weight")));
			// specs.setDimensionUnitOfMeasure(dimensionUnitOfMeasure);

			// core properties
			PersistableProduct product = new PersistableProduct();
			product.setId(Long.parseLong(code));
			product.setSku(code);
			product.setRefSku(barcode);
			product.setProductSpecifications(specs);
			product.setCategories(categories);

			product.setSortOrder(order);// set the order or iterator as sort order
			product.setAvailable(true);// force availability
			product.setProductVirtual(false);// force tangible good
			product.setQuantityOrderMinimum(1);// force to 1 minimum when ordering
			product.setProductShipeable(true);// all items are shipeable

			/** images **/
			String image = record.get("image_file");
			if (!StringUtils.isBlank(image)) {

				StringBuilder imageName = new StringBuilder();
				imageName.append(IMAGE_BASE_DIR).append(image.trim());// TODO remove

				File imgPath = new File(imageName.toString());
				log.debug("imgPath=" + imgPath);

				byte[] bytes = this.extractBytes(imgPath);
				if (bytes != null) {
					PersistableImage persistableImage = new PersistableImage();

					persistableImage.setBytes(bytes);
					persistableImage.setName(imgPath.getName());

					List<PersistableImage> images = new ArrayList<PersistableImage>();
					images.add(persistableImage);

					product.setImages(images);
				} else {
					// имя есть, нет самого файла
				}

			} else {
				// что делать если нет имени файла картинки
			}

			product.setQuantity(quantity);
			product.setQuantityOrderMaximum(maximumQuantity(quantity));
			product.setQuantityOrderMinimum(minimumQuantity(quantity));

			PersistableProductPrice persistableProductPrice = new PersistableProductPrice();
			persistableProductPrice.setDefaultPrice(true);

			persistableProductPrice.setOriginalPrice(productPrice);

			// apply a discunted price
			if (record.isSet("deal")) {
				String discount = record.get("deal");
				BigDecimal discountedPrice = this.createDiscountedPrice(productPrice, discount);
				if (discountedPrice != null) {
					persistableProductPrice.setDiscountedPrice(discountedPrice);
				}
			}

			List<PersistableProductPrice> productPriceList = new ArrayList<PersistableProductPrice>();
			productPriceList.add(persistableProductPrice);

			product.setProductPrices(productPriceList);

			List<ProductDescription> descriptions = new ArrayList<ProductDescription>();

			for (int langLenth = 0; langLenth < langs.length; langLenth++) {

				ProductDescription description = new ProductDescription();

				String lang = langs[langLenth];

				if (!record.isSet("pre") || !StringUtils.isBlank(record.get("pre-order"))) {
					// something specific must be written ?
				}

				description = new ProductDescription();
				description.setLanguage(lang);
				description.setTitle(record.get("name_" + lang));
				description.setName(record.get("name_" + lang));
				description.setDescription(record.get("description_" + lang));
				if (StringUtils.isBlank(description.getName())) {
					description.setName(description.getDescription());
				}
				description.setFriendlyUrl(minimalFriendlyUrlCreator(description.getName(), product.getSku()));
				descriptions.add(description);

			}

			product.setDescriptions(descriptions);

			ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
			String json = writer.writeValueAsString(product);

			log.info("Line " + i + " ********************");
			log.debug(json);

			// проверяем уникальность товара
			HttpEntity<String> entityHeader = new HttpEntity<String>(httpHeader);
			log.debug("product. code=" + product.getSku());
			ResponseEntity<EntityExists> responseUnique = restTemplate.exchange(
					baseUrl + "/api/v1/private/product/unique?code=" + product.getSku(), HttpMethod.GET, entityHeader,
					EntityExists.class);

			log.debug("status=" + responseUnique.getStatusCodeValue());
			log.debug("Product is Exists:" + responseUnique.getBody().isExists());

			// post to create category v0 API web service

			if (responseUnique.getBody().isExists()) {
				// существует - обновить
				log.debug("Товар code=" + product.getSku() + " существует - обновляем:");
				countUpdate++;

			} else {
				// создать новый товар
				
				if (!DRY_RUN) {
					HttpEntity<String> entity = new HttpEntity<String>(json, httpHeader);
					ResponseEntity<PersistableProduct> response = restTemplate
							.postForEntity(baseUrl + "/api/v1/private/product", entity, PersistableProduct.class);
//				PersistableProduct prod = (PersistableProduct) response.getBody();
				}
				log.info("Created product in row " + i + " Dry Run [" + DRY_RUN + "]");
				countCreate++;
			}

			i++;// rows

			if (i == MAX_COUNT) {
				System.out.println("Reached MAX_COUNT [" + MAX_COUNT + "]");
				break;
			}
		}

		log.info("------------------------------------");
		log.info("--------------Товары: создано="+countCreate+", обновлено="+countUpdate+" ----------------");
		log.info("Product import done " + count + " Dry Run [" + DRY_RUN + "]");
		log.info("------------------------------------");

	}

	protected HttpHeaders getHeader() {
		/*
		 * if (DRY_RUN) return null; if(this.httpHeader != null) { return
		 * this.httpHeader; }
		 */

		/*
		 * RestTemplate restTemplate = new RestTemplate(); final
		 * ResponseEntity<AuthenticationResponse> response =
		 * restTemplate.postForEntity(baseUrl +"/api/v1/private/login", new
		 * HttpEntity<>(new AuthenticationRequest("admin", "password")),
		 * AuthenticationResponse.class); final HttpHeaders headers = new HttpHeaders();
		 * headers.setContentType(new MediaType("application", "json",
		 * Charset.forName("UTF-8"))); headers.add("Authorization", "Bearer " +
		 * response.getBody().getToken()); this.httpHeader = headers; return
		 * this.httpHeader;
		 */
		HttpHeaders headers = new HttpHeaders();
		MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
		headers.setContentType(mediaType);
		// Basic Authentication
		String authorisation = mProps.getProperty("serverAPI.user") + ":" + mProps.getProperty("serverAPI.password");
		byte[] encodedAuthorisation = Base64.encode(authorisation.getBytes());
		headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
		return headers;

	}

	/**
	 * Can be used to generate a sku on the fly
	 * 
	 * @return
	 */
	private String generateSku() {
		String sku = UUID.randomUUID().toString();
		sku = sku.replace("-", "");
		return sku;
	}

	/**
	 * Makes sure that the sku is unique if not, add suffix of a numeric
	 * incrementable value
	 * 
	 * @return
	 */
	private String uniqueSku(String sku) {
		sku = sku.replace("-", "");
		sku = sku.replace(" ", "");
		if (skuMap.containsKey(sku)) {
			int count = skuMap.get(sku);
			count = count++;
			sku = count + sku;
		} else {
			skuMap.put(sku, iterationCount);
		}

		return sku.trim();
	}

	private BigDecimal convertDimension(String value, String dimensions) {

		// in our case we convert from cm to inches 1 * 0.393701
		if (StringUtils.isBlank(value)) {
			value = "0";
		}
		value = value.replaceAll(",", ".").trim();
		// System.out.println("Dimension " + value);
		BigDecimal decimalValue = new BigDecimal(value);

		if (StringUtils.isBlank(dimensions) || "CM".equals(dimensions)) {
			decimalValue = decimalValue.multiply(new BigDecimal("0.393701"));
		}

		BigDecimal scaled = decimalValue.setScale(0, RoundingMode.HALF_UP);
		return scaled;

	}

	private BigDecimal convertWeight(String value) {

		// in our case we already have the good weight
		if (StringUtils.isBlank(value)) {
			value = "0";
		}
		value = value.replaceAll(",", ".").trim();
		BigDecimal decimalValue = new BigDecimal(value);
		BigDecimal scaled = decimalValue.setScale(0, RoundingMode.HALF_UP);
		// decimalValue = decimalValue.multiply(new BigDecimal("0.393701"));
		return scaled;

	}

	private BigDecimal createDiscountedPrice(BigDecimal price, String discount) {

		if (StringUtils.isNotBlank(discount) && !discount.equals("0")) {
			double percent = 1 - Double.parseDouble(discount) / 100d;
			price = price.multiply(new BigDecimal(percent));
			return roundAmount(price);
		}

		return null;

	}

	private BigDecimal roundAmount(BigDecimal amount) {
		BigDecimal scaled = amount.setScale(0, RoundingMode.HALF_UP);
		return scaled;
	}

	public byte[] extractBytes(File imgPath) throws Exception {

		if (!imgPath.exists()) {
			log.info("--------------------------------------");
			log.info("IMAGE NOT FOUND " + imgPath.getName());
			log.info("IMAGE PATH " + imgPath.getAbsolutePath());
			log.info("--------------------------------------");
			return null;
		}

		FileInputStream fis = null;
		BufferedInputStream inputStream = null;
		try {
			fis = new FileInputStream(imgPath);
			inputStream = new BufferedInputStream(fis);
			byte[] fileBytes = new byte[(int) imgPath.length()];
			inputStream.read(fileBytes);
			return fileBytes;
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (fis != null) {
				fis.close();
			}
		}

	}

	public int randPrice(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

	public String minimalFriendlyUrlCreator(String productName, String code) {
		productName = productName.toLowerCase();
		productName = productName.replace("  ", " ");

		// remove accents
		productName = Normalizer.normalize(productName, Normalizer.Form.NFD);
		productName = productName.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

		// разбираем строку на слова
//		String[] mProd = productName.split(" ");
//		log.debug(Arrays.toString(mProd));
		Transliterator ruToLatin = Transliterator.getInstance("Russian-Latin/BGN");
		String result = ruToLatin.transform(productName);

		Slugify slg = new Slugify();
		log.debug("Slugify=" + slg.slugify(result));

		/*
		 * result = Normalizer.normalize(result, Normalizer.Form.NFD) .replace("ʹ", "")
		 * .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
		 * .replaceAll("[^\\p{Alnum}]+", "-");
		 * 
		 * //обрезаем последний - if(result.endsWith("-")) { result =
		 * result.substring(0, result.length()-1); }
		 */

//		productName = "p"+code+"-"+result.replace(" ", "-");
		productName = "p" + code + "-" + slg.slugify(result);

		return productName;
	}

	public int maximumQuantity(int quantity) {
		if (quantity == 1) {
			return quantity;
		}

		if (quantity > 0) {
			return quantity++;
		}

		return quantity;
	}

	public int minimumQuantity(int quantity) {

		return Integer.parseInt("1");
	}

	private String alternativeIdentifier(CSVRecord record) {
		return record.get("sku");
	}

	/**
	 * Формирует ссылку типа p+<code>+translirate name
	 * 
	 * @param url
	 * @param code
	 * @param lang
	 * @return
	 */
	private String getFriendlyURL(String url, String code, String lang) {
		String friendlyUrl = "";
		friendlyUrl += "p" + code;
		return friendlyUrl;
	}

}
