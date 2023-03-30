package com.se1.userservice.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se1.userservice.domain.common.SCMConstant;
import com.se1.userservice.domain.model.Contact;
import com.se1.userservice.domain.model.User;
import com.se1.userservice.domain.payload.ApiResponseEntity;
import com.se1.userservice.domain.payload.ContactDto;
import com.se1.userservice.domain.payload.UserContactDto;
import com.se1.userservice.domain.payload.UserDetail;
import com.se1.userservice.domain.payload.UserDto;
import com.se1.userservice.domain.payload.response.RabbitRequest;
import com.se1.userservice.domain.rabbitMQ.payload.ContactResponse;
import com.se1.userservice.domain.repository.ContactRepository;
import com.se1.userservice.domain.repository.UserRepository;
import com.se1.userservice.domain.restClient.ChatServiceRestTemplate;

@Service
public class ContactService {

	@Autowired
	private RabbitSenderService rabbitSenderService;
	
	@Autowired
	private ContactRepository contactRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ChatServiceRestTemplate restTemplate;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	public void processCreate(Contact contactCreate, ApiResponseEntity apiResponseEntity) throws Exception {
		User userReciver = userRepository.findById(contactCreate.getUserReciverId()).orElse(null);
		if (userReciver == null) {
			throw new Exception("Người dùng không tồn tại");
		}

		Contact contact = null;
		try {
			contact = contactRepository.save(contactCreate);
			User userSender = userRepository.findById(contact.getUserSenderId()).orElse(null);
			
			UserDetail userSenderDto = new UserDetail();
			BeanUtils.copyProperties(userSender, userSenderDto);
			
			UserDetail userReciverDto = new UserDetail();
			BeanUtils.copyProperties(userReciver, userReciverDto);
			
			ContactResponse contactResponse = new ContactResponse();
			BeanUtils.copyProperties(contact, contactResponse);
			contactResponse.setUserReciver(userReciverDto);
			contactResponse.setUserSender(userSenderDto);
			
			if (contact.getStatus() == 1) {
				//Send to system
				RabbitRequest rabbitResponse = new RabbitRequest();
				rabbitResponse.setAction(SCMConstant.SYSTEM_CONTACT);
				rabbitResponse.setData(contactResponse);

				rabbitSenderService.convertAndSendSysTem(rabbitResponse);

			}
		} catch (DataAccessException e) {
			throw new Exception(e.getMessage());
		}
		apiResponseEntity.setData(contact);
		apiResponseEntity.setErrorList(null);
		apiResponseEntity.setStatus(1);
	}

	public void processUpdate(long userId, long userLoginId, int statusUpdate, ApiResponseEntity apiResponseEntity)
			throws Exception {

		Contact oldContact = contactRepository.findByUserReciverIdAndUserSenderId(userId, userLoginId);

		if (oldContact != null) {
			long userReciverId = oldContact.getUserReciverId();
			long userSenderId = oldContact.getUserSenderId();

			validStatus(oldContact.getStatus(), statusUpdate);

			long contactId = contactRepository.updateContact(userReciverId, userSenderId, statusUpdate);

			Contact contactUpdate = contactRepository.findById(contactId).orElse(null);

			if (contactUpdate != null) {

				//Send to system
				RabbitRequest rabbitResponse = new RabbitRequest();
				rabbitResponse.setAction(SCMConstant.SYSTEM_CONTACT);
				rabbitResponse.setData(contactUpdate);
				rabbitSenderService.convertAndSendSysTem(rabbitResponse);

				apiResponseEntity.setData(true);
				apiResponseEntity.setErrorList(null);
				apiResponseEntity.setStatus(1);
			} else {
				throw new Exception("Thao tác không hợp lệ");
			}
		}else {
			throw new Exception("Tin nhắn không hợp lệ");
		}

	}

	private void validStatus(int statusOld, int statusUpdate) throws Exception {
		if (statusOld == 2 && statusUpdate != 0) {
			throw new Exception("Thao tác không hợp lệ");
		}

		if (statusOld == 1 && statusUpdate == 1) {
			throw new Exception("Thao tác không hợp lệ");
		}

		if (statusOld == 0 && statusUpdate != 1) {
			throw new Exception("Thao tác không hợp lệ");
		}
	}

	public void processGetListFriend(UserDetail userDetail, ApiResponseEntity apiResponseEntity) {
		Long userId = userDetail.getId();
		
		Map<Integer , List<ContactDto>> mapResult = new HashMap<>();
		mapResult.put(2, getContactResponse(userId));
		
		apiResponseEntity.setData(mapResult);
		apiResponseEntity.setErrorList(null);
		apiResponseEntity.setStatus(1);
	}	

	public void processGetContactRequest(UserDetail userDetail, ApiResponseEntity apiResponseEntity) {
		Long userId = userDetail.getId();
		List<Contact> contact = contactRepository.findByUserId(userId, 1);
		List<Long> userFriendIds = contact.stream().map(c -> c.getUserReciverId().equals(userId) ? c.getUserSenderId() : c.getUserReciverId()).collect(Collectors.toList());
		List<User> userFriends = (List<User>) userRepository.findAllById(userFriendIds);
		List<UserDetail> userFriendResponse = userFriends.stream().map(uf -> {
			UserDetail ud = new UserDetail();
			BeanUtils.copyProperties(uf, ud);
			return ud;
		}).collect(Collectors.toList());
		
		List<Contact> contactFriendReciver = contact.stream().filter(c->c.getUserReciverId() == userId).collect(Collectors.toList());
		List<ContactDto> contactResponsesReciver = contactFriendReciver.stream().map(ct->{
			ContactDto contactDto = new ContactDto();
			BeanUtils.copyProperties(ct, contactDto);
			contactDto.setTopicContactId(ct.getTopicId());
			Long userFriendId = ct.getUserReciverId().equals(userId) ? ct.getUserSenderId() : ct.getUserReciverId();
			UserDetail userFriend = userFriendResponse.stream().filter(u -> userFriendId.equals(u.getId())).findFirst().get();
			contactDto.setUserFriend(userFriend);
			
			return contactDto;
		}).collect(Collectors.toList());
		
		List<Contact> contactFriendSender = contact.stream().filter(c->c.getUserReciverId() == userId).collect(Collectors.toList());
		List<ContactDto> contactResponsesSender = contactFriendSender.stream().map(ct->{
			ContactDto contactDto = new ContactDto();
			BeanUtils.copyProperties(ct, contactDto);
			contactDto.setTopicContactId(ct.getTopicId());
			Long userFriendId = ct.getUserReciverId().equals(userId) ? ct.getUserSenderId() : ct.getUserReciverId();
			UserDetail userFriend = userFriendResponse.stream().filter(u -> userFriendId.equals(u.getId())).findFirst().get();
			contactDto.setUserFriend(userFriend);
			
			return contactDto;
		}).collect(Collectors.toList());
		
		Map<String , List<ContactDto>> mapResult = new HashMap<>();
		mapResult.put("Sender", contactResponsesSender);
		mapResult.put("Reciver", contactResponsesReciver);
		apiResponseEntity.setData(mapResult);
		apiResponseEntity.setErrorList(null);
		apiResponseEntity.setStatus(1);
		
	}

	boolean checkValidContact(String topicId){
		boolean result = false;
		ApiResponseEntity userChatParentResult = (ApiResponseEntity) restTemplate.existChat(topicId);
		if (userChatParentResult.getStatus() == 1) {
			String apiResultStr;
			try {
				apiResultStr = objectMapper.writeValueAsString(userChatParentResult.getData());
				result = objectMapper.readValue(apiResultStr, Boolean.class);
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
		}
		
		return result;
	}

	public void processGetListContactForChat(UserDetail userDetail, ApiResponseEntity apiResponseEntity) {
		Long userId = userDetail.getId();
		
		List<ContactDto> contactDtos = getContactResponse(userId);
		
	}
	
	private List<ContactDto> getContactResponse(Long userId){
		List<Contact> contact = contactRepository.findByUserId(userId, null);
		List<Contact> contactIsValid = contact.stream()
				.filter(c->c.getStatus() == 2)
				.collect(Collectors.toList());
		List<Contact> contactValid = contact.stream()
				.filter(c->c.getStatus() != 2)
				.filter(c->checkValidContact(c.getTopicId()))
				.collect(Collectors.toList());
		List<Contact> contactMerge = new ArrayList<>(contactIsValid);
		contactMerge.addAll(contactValid);
		List<Long> userFriendIds = contactMerge.stream().map(c -> c.getUserReciverId().equals(userId) ? c.getUserSenderId() : c.getUserReciverId()).collect(Collectors.toList());
		List<User> userFriends = (List<User>) userRepository.findAllById(userFriendIds);
		List<UserDetail> userFriendResponse = userFriends.stream().map(uf -> {
			UserDetail ud = new UserDetail();
			BeanUtils.copyProperties(uf, ud);
			return ud;
		}).collect(Collectors.toList());
		List<ContactDto> contactResponses = contactMerge.stream().map(ct->{
			ContactDto contactDto = new ContactDto();
			BeanUtils.copyProperties(ct, contactDto);
			contactDto.setTopicContactId(ct.getTopicId());
			Long userFriendId = ct.getUserReciverId().equals(userId) ? ct.getUserSenderId() : ct.getUserReciverId();
			UserDetail userFriend = userFriendResponse.stream().filter(u -> userFriendId.equals(u.getId())).findFirst().get();
			contactDto.setUserFriend(userFriend);
			
			return contactDto;
		}).collect(Collectors.toList());
		
		return contactResponses;
	}
	
}