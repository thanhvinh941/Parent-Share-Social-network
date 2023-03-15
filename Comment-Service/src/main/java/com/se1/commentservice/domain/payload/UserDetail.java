package com.se1.commentservice.domain.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetail {
	
	private Long id;
	private String name;
	private String imageUrl;
	private String role;
	private boolean isExpert;
	private Double rating;
	private Byte status;
}