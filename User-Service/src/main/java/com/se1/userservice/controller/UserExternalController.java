package com.se1.userservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se1.userservice.domain.payload.ApiResponseEntity;
import com.se1.userservice.domain.payload.UserDetail;
import com.se1.userservice.domain.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/external")
@RequiredArgsConstructor
public class UserExternalController {

	private final UserService service;
	private final ObjectMapper objectMapper;

	@PostMapping("/findById")
	public ResponseEntity<?> findById(@RequestHeader("user_detail") String userDetailHeader,
			@RequestParam("id") Long id) throws Exception {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail userDetail = objectMapper.readValue(userDetailHeader, UserDetail.class);

		try {
			Object response = service.processFindUserById(userDetail.getId(), id);
			apiResponseEntity.setData(response);
			apiResponseEntity.setErrorList(null);
			apiResponseEntity.setStatus(1);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);
	}

	@PostMapping("/findByName")
	public ResponseEntity<?> findByName(@RequestParam("name") String name, @RequestParam("offset") Integer offset,
			@RequestHeader("user_detail") String userDetailHeader)
			throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail userDetail = objectMapper.readValue(userDetailHeader, UserDetail.class);

		try {
			Object response = service.processFindByName(userDetail.getId(), name.trim(), offset);
			apiResponseEntity.setData(response);
			apiResponseEntity.setErrorList(null);
			apiResponseEntity.setStatus(1);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);
	}

	@PostMapping("/findAllExpert")
	public ResponseEntity<?> findAllExpert(@RequestHeader("user_detail") String userDetailHeader,
			@RequestParam("offset") Integer offset) throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail userDetail = objectMapper.readValue(userDetailHeader, UserDetail.class);
		try {
			Object response = service.findAllExpert(userDetail, offset);
			apiResponseEntity.setData(response);
			apiResponseEntity.setErrorList(null);
			apiResponseEntity.setStatus(1);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);
	}

	@PostMapping("/report/{id}")
	public ResponseEntity<?> report(@PathVariable("id") Long id,@RequestParam(name = "reason", required = false) String reason, @RequestHeader("user_detail") String userDetailHeader)
			throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail userDetail = objectMapper.readValue(userDetailHeader, UserDetail.class);

		try {
			service.report(id, userDetail, apiResponseEntity, reason);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);
	}
}
