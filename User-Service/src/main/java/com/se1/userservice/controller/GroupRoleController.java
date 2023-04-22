package com.se1.userservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se1.userservice.domain.payload.ApiResponseEntity;
import com.se1.userservice.domain.payload.CreateGroupRoleRequest;
import com.se1.userservice.domain.payload.UserDetail;
import com.se1.userservice.domain.service.GroupRoleService;

@RestController
@RequestMapping("/group-role/external")
public class GroupRoleController {

	@Autowired
	private ApiResponseEntity apiResponseEntity;

	@Autowired
	private GroupRoleService groupRoleService;

	@Autowired
	private ObjectMapper objectMapper;
	
	@PostMapping("/create")
	public ResponseEntity<?> createGroupRole(@RequestBody CreateGroupRoleRequest request,  @RequestHeader("user_detail") String userDetailHeader) throws JsonMappingException, JsonProcessingException{
		
		UserDetail userDetail = objectMapper.readValue(userDetailHeader, UserDetail.class);
		
		try {
			groupRoleService.processGoupRole(request, userDetail, apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		
		return ResponseEntity.ok(apiResponseEntity);
		
	}
}
