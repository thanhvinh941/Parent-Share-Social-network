package com.se1.postservice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.se1.postservice.domain.payload.CreatePostRequest;
import com.se1.postservice.domain.payload.UserDetail;
import com.se1.postservice.domain.payload.UpdatePostRequest;
import com.se1.postservice.domain.service.PostService;
import com.se1.postservice.domain.service.impl.LikePostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/post/external")
@RequiredArgsConstructor
public class PostExternalController {

	private final PostService postService;

	private final ObjectMapper objectMapper;
	private final LikePostService likePostService;

	@PostMapping("/findAllPost")
	public ResponseEntity<?> findAllPost(@RequestHeader("user_detail") String userDetail,
			@RequestParam("offset") Integer offset) throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
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

	@PostMapping("/findAllPostByUserId")
	public ResponseEntity<?> findAllPostByUserId(@RequestParam("user-id") Long userId,
			@RequestParam("offset") Integer offset) throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		try {
			postService.findAllPostByUserId(userId, apiResponseEntity, offset);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);
	}

	@PostMapping("/getAllPost")
	public ResponseEntity<?> getAllPost(@RequestHeader("user_detail") String userDetail,
			@RequestParam("offset") Integer offset) throws JsonMappingException, JsonProcessingException {
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		try {
			postService.findAllPostByUserId(detail.getId(), apiResponseEntity, offset);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);
	}

	@PostMapping("/create")
	public ResponseEntity<?> save(@RequestBody CreatePostRequest postRequest, @RequestHeader("user_detail") String userDetail)
			throws JsonMappingException, JsonProcessingException {

		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		try {
			postService.processSavePost(postRequest, detail, apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}

	@PostMapping("/findByTitle")
	public ResponseEntity<?> getByTitle(@RequestParam("title") String title, 
			@RequestParam("offset") Integer offset,
			@RequestParam(name = "topicTagId", required = false) Integer topicTagId,
			@RequestHeader("user_detail") String userDetail) throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		if(title == null || title.isEmpty()) {
			title = " ";
		}
		
		Map<String, Object> param = new HashMap<>();
		param.put("title", title);
		param.put("context", title);
		param.put("hashTag", title);

		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);

		try {
			postService.findAllPostByCondition(detail.getId(), param, apiResponseEntity, offset, topicTagId);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}

	@PostMapping("/findById")
	public ResponseEntity<?> findById(@RequestParam("postId") Long id, @RequestHeader("user_detail") String userDetail)
			throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);

		try {
			postService.findById(id, detail.getId(), apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}
	
	@PostMapping("/findPostAllMost")
	public ResponseEntity<?> findPostAllMost(@RequestHeader("user_detail") String userDetail)
			throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);

		try {
			postService.findPostAllMost(detail.getId(), apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}

	@PostMapping("/{postId}/like")
	public ResponseEntity<?> likePost(@PathVariable("postId") Long postId,
			@RequestHeader("user_detail") String userDetail) throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);
		try {
			likePostService.likePost(postId, apiResponseEntity, detail);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}

	@PostMapping("/{postId}/dislike")
	public ResponseEntity<?> dislikePost(@PathVariable("postId") Long postId,
			@RequestHeader("user_detail") String userDetail) throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);
		try {
			likePostService.dislikePost(postId, apiResponseEntity, detail);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}
	
	@PostMapping("/update")
	public ResponseEntity<?> update(@RequestHeader("user_detail") String userDetail, @RequestBody UpdatePostRequest postRequest)
			throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);

		try {
			postService.update(postRequest, detail, apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}
	
	@PostMapping("/delete")
	public ResponseEntity<?> delete(@RequestHeader("user_detail") String userDetail, @RequestParam("postId") Long postId)
			throws JsonMappingException, JsonProcessingException {
		ApiResponseEntity apiResponseEntity = new ApiResponseEntity();
		UserDetail detail = objectMapper.readValue(userDetail, UserDetail.class);

		try {
			postService.delete(postId, detail, apiResponseEntity);
		} catch (Exception e) {
			apiResponseEntity.setData(null);
			apiResponseEntity.setErrorList(List.of(e.getMessage()));
			apiResponseEntity.setStatus(0);
		}
		return ResponseEntity.ok().body(apiResponseEntity);

	}
}
