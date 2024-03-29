package com.se1.chatservice.payload;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ChatDto {
	private Long id;
	private User user;
	private String content;
	private int status;
	private String topicId;
	private ChatDto chatParent;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createAt;
	private Boolean isFile;

	@Data
	public static class User {
		private Long id;
		private String email;
		private String name;
		private String imageUrl;
		private Boolean isExpert;
		private Double rating = null;
		private int status;
		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
		private Date lastTime = new Date();
	}
}
