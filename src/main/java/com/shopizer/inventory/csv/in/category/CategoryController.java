package com.shopizer.inventory.csv.in.category;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.salesmanager.shop.model.catalog.category.ReadableCategory;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Controller
@RequestMapping("/category")
public class CategoryController {
	@Autowired
	private CategoryImport categoryImport;
	
	
	@GetMapping("/deleteAll")
	public String categoryDeleteAll() {
		log.debug("controller categoryDeleteAll");
		return("index");
	}
	
	@GetMapping("/list")
	public String categoryList(Model model) {
		log.debug("controller categoryList");
		List<ReadableCategory>categoriesList = categoryImport.listCategory();
		log.debug("categoriesList.size="+categoriesList.size());
		model.addAttribute("categoryes", categoriesList);
		return("index");
	}
	@GetMapping("/list/{id}")
	public String categoryListById(@PathVariable("id") int id, Model model) {
		log.debug("controller /category/list/"+id);
		ReadableCategory category = categoryImport.listCategoryById(id);
		log.debug("categoryListById.size="+category.toString());
		model.addAttribute("categoryes", category);
		return("index");
	}
	
	@GetMapping("/delete/{id}")
	public String categoryDelete(@PathVariable("id") int id, Model model) {
		log.debug("controller /category/delete/"+id);
		categoryImport.deleteCategory(id);
		model.addAttribute("categoryes", categoryImport.listCategory());
		return("index");
	}
	
	@GetMapping("/importXML")
	public String importXML(Model model) {
		log.debug("controller importXML");
		categoryImport.importCategoryFromXML();
//		log.debug("categoriesList.size="+categoriesList.size());
//		model.addAttribute("categoryes", categoriesList);
		return("index");
	}

}
