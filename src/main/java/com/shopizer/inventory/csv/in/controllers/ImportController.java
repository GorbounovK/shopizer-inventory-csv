package com.shopizer.inventory.csv.in.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Controller
public class ImportController {
	@GetMapping("/productImport")
	public String productImportPage() {
		log.debug("/productImport");
		return "index";
	}
}
