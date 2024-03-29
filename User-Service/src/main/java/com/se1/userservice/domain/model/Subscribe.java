package com.se1.userservice.domain.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
@Entity
public class Subscribe {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	private Long userExpertId;
	
	private Long userSubscriberId;
	
	@Column(nullable = false)
	@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date createAt;
}
