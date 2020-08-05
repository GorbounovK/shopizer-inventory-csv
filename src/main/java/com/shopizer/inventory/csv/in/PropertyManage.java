package com.shopizer.inventory.csv.in;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PropertyManage {
	private static final Logger LOG = LogManager.getLogger(PropertyManage.class);
	private static Properties mProps;
	private static PropertyManage instance;

	/**
	*
	*/
	private PropertyManage() {

	}

	public static PropertyManage getInstance() {
		if (instance == null) {
			instance = new PropertyManage("AppImport.properties");
		}
		return instance;
	}

	/**
	 *
	 * @param path
	 */
	private PropertyManage(final String path) {
		setProperties(path);
	}

	/**
	 *
	 * @return Properties
	 */
	public Properties getProperties() {
		return mProps;
	}

	/**
	 *
	 * @param path
	 */
	public static void setProperties(final String path) {
		Properties props = new Properties();
		try {
			InputStream is;
			is = new FileInputStream(path);
			if (is != null) {
				Reader reader = new InputStreamReader(is, "UTF-8");
				props.load(reader);
				LOG.trace(props.toString());
				is.close();
			}
		} catch (Exception e) {
			LOG.error("Ошибка получения properties", e);
		}
		LOG.trace("Настройки прочитаны.");
		mProps = props;
	}
}
