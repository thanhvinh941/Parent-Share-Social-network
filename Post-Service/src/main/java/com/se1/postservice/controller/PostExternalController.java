package com.se1.postservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se1.postservice.domain.payload.ApiResponseEntity;
import com.se1.postservice.domain.payload.PostRequest;
import com.se1.postservice.domain.payload.UserDetail;
import com.se1.postservice.domain.service.PostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/post/external")
@RequiredArgsConstructor
public class PostExternalController {

	private final PostService postService;

	private final ObjectMapper objectMapper;

	private final ApiResponseEntity apiResponseEntity;
	
	@PostMapping("/findAllPost")
	public ResponseEntity<?> findAllPost(@RequestHeader("user_detail") String userDetail, @RequestParam("offset") int offset) throws JsonMappingException, JsonProcessingException{
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);
		
		try {
			postService.findAllPost(detail, apiResponseEntity, offset);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);	
	}
	
	@PostMapping("/findPost")
	public ResponseEntity<?> findPostById(@RequestParam ("post-id") Long postId) throws JsonMappingException, JsonProcessingException{

		try {
			postService.findPostById(postId, apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);	
	}
	
	@PostMapping("/getAllPost")
	public ResponseEntity<?> getAllPost(@RequestHeader("user_detail") String userDetail) throws JsonMappingException, JsonProcessingException{
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);
	
		try {
			postService.processGetAllPost(detail, apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);
	}
	
	@PostMapping("/create")
	public ResponseEntity<?> save(@RequestBody PostRequest postRequest, @RequestHeader("user_detail") String userDetail)
			throws JsonMappingException, JsonProcessingException {

		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);
		
		try {
			postService.processSavePost(postRequest, detail, apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}
	
	@PostMapping("/getByTitle")
	public ResponseEntity<?> getByTitle(@RequestParam("title") String title, @RequestHeader("user_detail") String userDetail)
			throws JsonMappingException, JsonProcessingException {

		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);
		
		try {
			postService.processGetByTitle(title, detail, apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}
}
