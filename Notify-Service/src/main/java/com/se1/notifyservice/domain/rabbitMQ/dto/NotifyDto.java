package com.se1.notifyservice.domain.rabbitMQ.dto;

import lombok.Data;

@Data
public class NotifyDto {
	private Long userId;
	private String param;
	private String value;
}
