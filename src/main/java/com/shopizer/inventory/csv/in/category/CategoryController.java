package com.shopizer.inventory.csv.in.category;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.salesmanager.shop.model.catalog.category.ReadableCategory;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Controller
public class CategoryController {
	@Autowired
	private CategoryImport categoryImport;
	
	
	@GetMapping("/categoryDeleteAll")
	public String categoryDeleteAll() {
		log.debug("controller categoryDeleteAll");
		return("index");
	}
	
	@GetMapping("/categoryList")
	public String categoryList(Model model) {
		log.debug("controller categoryList");
		List<ReadableCategory>categoriesList = categoryImport.listCategory().getCategories();
		log.debug("categoriesList.size="+categoriesList.size());
		model.addAttribute("categoryes", categoriesList);
		return("index");
	}
	
	@GetMapping("/delete/{id}")
	public String categoryDelete(@PathVariable("id") long id, Model model) {
		log.debug("controller /category/delete/"+id);
		return("index");
	}
}
