package com.nmrs.umb.biometriclinux.controllers;

import com.nmrs.umb.biometriclinux.dal.DbManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommunityController {

	@Autowired
	DbManager dbManager;

	@GetMapping("/community")
	public String greeting() {
		return "community";
	}

}
