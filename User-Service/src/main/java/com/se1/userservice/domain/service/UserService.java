package com.se1.userservice.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.se1.userservice.domain.common.SCMConstant;
import com.se1.userservice.domain.db.dto.ReportUserDto;
import com.se1.userservice.domain.db.read.RUserMapper;
import com.se1.userservice.domain.db.write.WUserMapper;
import com.se1.userservice.domain.model.AuthProvider;
import com.se1.userservice.domain.model.Contact;
import com.se1.userservice.domain.model.FindAllUserRequest;
import com.se1.userservice.domain.model.ReportUser;
import com.se1.userservice.domain.model.Subscribe;
import com.se1.userservice.domain.model.User;
import com.se1.userservice.domain.model.UserRole;
import com.se1.userservice.domain.payload.ApiResponseEntity;
import com.se1.userservice.domain.payload.FindAllReportRequest;
import com.se1.userservice.domain.payload.GetApiRequestEntity;
import com.se1.userservice.domain.payload.ReportUserResponseDto;
import com.se1.userservice.domain.payload.UpdateApiRequestEntity;
import com.se1.userservice.domain.payload.UserDetail;
import com.se1.userservice.domain.payload.UserResponseDto;
import com.se1.userservice.domain.payload.request.CreateUserRequest;
import com.se1.userservice.domain.payload.request.UpdateUserRequest;
import com.se1.userservice.domain.payload.response.UserResponseForClient;
import com.se1.userservice.domain.payload.response.UserResponseForClient.ExpertInfo;
import com.se1.userservice.domain.repository.ContactRepository;
import com.se1.userservice.domain.repository.ReportUserRepository;
import com.se1.userservice.domain.repository.SubscriberRepository;
import com.se1.userservice.domain.repository.UserRepository;
import com.se1.userservice.domain.restClient.SystemServiceRestTemplateClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository repository;
	private final RUserMapper rUserMapper;
	private final RatingService ratingService;
	private final SystemServiceRestTemplateClient restTemplateClient;
	private final WUserMapper wUserMapper;
	private final PasswordEncoder passwordEncoder;
	private final ReportUserRepository reportUserRepository;
	private final ContactRepository contactRepository;
	private final SubscriberRepository subscriberRepository;
	private final PlatformTransactionManager transactionManager;

	public User save(User user) throws Exception {

		User userSave = null;
		try {
			userSave = repository.save(user);
		} catch (DataAccessException e) {
			throw new Exception(e.getMessage());
		}
		return userSave;
	}

	public User findByEmail(String email) throws Exception {
		User userFind = null;
		try {
			userFind = repository.findByEmail(email);
		} catch (DataAccessException e) {
			throw new Exception(e.getMessage());
		}
		return userFind;
	}

	public User findById(Long id) throws Exception {
		User userFind = null;
		try {
			userFind = repository.findById(id).get();
		} catch (DataAccessException e) {
			throw new Exception(e.getMessage());
		}
		return userFind;
	}

	public static String camelToSnake(String str) {
		String result = "";

		char c = str.charAt(0);
		result = result + Character.toLowerCase(c);

		for (int i = 1; i < str.length(); i++) {

			char ch = str.charAt(i);
			if (Character.isUpperCase(ch)) {
				result = result + '_';
				result = result + Character.toLowerCase(ch);
			}

			else {
				result = result + ch;
			}
		}

		return result;
	}

	public void processFindUserByEmail(String email, ApiResponseEntity apiResponseEntity) throws Exception {
		User userFind = null;
		try {
			userFind = repository.findByEmail(email);
		} catch (DataAccessException e) {
			throw new Exception(e.getMessage());
		}

		validationUser(userFind);
		generatorResponse(userFind, apiResponseEntity);
	}

	public Object processFindUserById(Long currentUserId, Long id) throws Exception {
		User userFind = null;
		try {
			userFind = repository.findById(id).orElse(null);
		} catch (DataAccessException e) {
			throw new Exception(e.getMessage());
		}

		validationUser(userFind);

		return generatorResponseForClient(userFind, currentUserId);
	}

	private void validationUser(User userFind) throws Exception {
		if (userFind == null) {
			throw new Exception("Không tìm thấy người dùng hợp lệ");
		}

		if (userFind.getDelFlg()) {
			throw new Exception("Tài khoảng người dùng đã bị xóa");
		}

		if (!userFind.getEmailVerified()) {
			throw new Exception("Tài khoảng chưa được xác thực");
		}
	}

	private UserResponseDto convertUserEntityToUserResponseEntity(User user, Double rating, Integer ratingCount) {
		UserResponseDto userResponseDto = null;
		if (user != null) {
			userResponseDto = new UserResponseDto();
			userResponseDto.setId(user.getId());
			userResponseDto.setEmail(user.getEmail());
			userResponseDto.setEmailVerified(user.getEmailVerified());
			userResponseDto.setImageUrl(user.getImageUrl());
			userResponseDto.setName(user.getName());
			userResponseDto.setPassword(user.getPassword());
			userResponseDto.setProvider(user.getProvider());
			userResponseDto.setProviderId(user.getProviderId());
			userResponseDto.setRole(user.getRole().name());
			userResponseDto.setStatus(user.getStatus());
			userResponseDto.setRating(rating);
			userResponseDto.setIsExpert(user.getIsExpert());
			userResponseDto.setTopicId(user.getTopicId());
			userResponseDto.setRatingCount(ratingCount);
		}

		return userResponseDto;
	}

	private void generatorResponse(User userFind, ApiResponseEntity apiResponseEntity) {
		Double rating = 0.0;
		Integer ratingCount = 0;
		if (userFind.getIsExpert()) {
			rating = (Double) ratingService.getRatingByUserId(userFind.getId(), null).get("rating");
			ratingCount = (Integer) ratingService.getRatingByUserId(userFind.getId(), null).get("count");
		}
		UserResponseDto userResponseDto = convertUserEntityToUserResponseEntity(userFind, rating, ratingCount);

		apiResponseEntity.setData(userResponseDto);
		apiResponseEntity.setErrorList(null);
		apiResponseEntity.setStatus(1);
	}

	private Object generatorResponseForClient(User userFind, Long currentUserId) {
		Double rating = 0.0;
		Integer ratingCount = null;
		Boolean isRate = false;
		if (userFind.getIsExpert()) {
			rating = (Double) ratingService.getRatingByUserId(userFind.getId(), currentUserId).get("rating");
			ratingCount = (Integer) ratingService.getRatingByUserId(userFind.getId(), currentUserId).get("count");
			isRate = (Boolean) ratingService.getRatingByUserId(userFind.getId(), currentUserId).get("isRate");
		}
		UserResponseForClient userResponseDto = convertUserEntityToUserResponseForClient(userFind, rating,
				currentUserId, ratingCount, isRate);

		return userResponseDto;
	}

	private UserResponseForClient convertUserEntityToUserResponseForClient(User userFind, double rating,
			Long currentUserId, Integer ratingCount, Boolean isRate) {
		UserResponseForClient userResponseDto = new UserResponseForClient();
		BeanUtils.copyProperties(userFind, userResponseDto);

		if (currentUserId != null) {
			Contact contact = contactRepository.findByUserReciverIdAndUserSenderId(userFind.getId(), currentUserId);
			if (contact != null) {
				Integer status = 0;
				if (contact.getStatus() == 1 && contact.getUserReciverId().equals(currentUserId)) {
					status = 3;
				} else if (contact.getStatus() == 1) {
					status = 1;
				} else if (contact.getStatus() == 2) {
					status = 2;
				}

				UserResponseForClient.ContactInfo contactInfo = new UserResponseForClient.ContactInfo();
				contactInfo.setStatus(status);
				contactInfo.setTopicContactId(contact.getTopicId());
				userResponseDto.setContactInfo(contactInfo);
			}

		}

		if (userFind.getIsExpert()) {
			ExpertInfo expertInfo = new ExpertInfo();
			if (currentUserId != null) {
				Subscribe subscribe = subscriberRepository.findByUserExpertIdAndUserSubscriberId(userFind.getId(),
						currentUserId);
				Boolean isSubscriber = false;
				if (subscribe != null) {
					isSubscriber = true;
				}
				expertInfo.setIsSub(isSubscriber);
			}
			BeanUtils.copyProperties(userFind, expertInfo);
			expertInfo.setRating(rating);
			expertInfo.setRatingCount(ratingCount);
			expertInfo.setIsRate(isRate);
			userResponseDto.setExpertInfo(expertInfo);
		}

		return userResponseDto;
	}

	public Object processFindByName(Long currentUserId, String name, Integer offset) {
		List<User> users = repository.findByName(name, currentUserId);
		users = users.stream().filter(u -> !u.getRole().equals(UserRole.admin))
				.filter(u -> !u.getId().equals(currentUserId)).collect(Collectors.toList());

		Collections.sort(users, new Comparator<User>() {

			@Override
			public int compare(User o1, User o2) {

				Integer u1Sort = o1.getIsExpert() != null ? (o1.getIsExpert() ? 0 : 1) : 1;

				Integer u2Sort = o2.getIsExpert() != null ? (o2.getIsExpert() ? 0 : 1) : 1;

				return u1Sort.compareTo(u2Sort);
			};
		});

		List<UserResponseForClient> responseList = users.stream().filter(ul -> ul.getEmailVerified() && !ul.getDelFlg())
				.map(ul -> {
					Double rating = 0.0;
					Integer ratingCount = null;
					Boolean isRate = false;
					if (ul.getIsExpert()) {
						rating = (Double) ratingService.getRatingByUserId(ul.getId(), currentUserId).get("rating");
						ratingCount = (Integer) ratingService.getRatingByUserId(ul.getId(), currentUserId).get("count");
						isRate = (Boolean) ratingService.getRatingByUserId(ul.getId(), currentUserId).get("isRate");
					}
					UserResponseForClient userResponseDto = convertUserEntityToUserResponseForClient(ul, rating,
							currentUserId, ratingCount, isRate);
					return userResponseDto;
				}).collect(Collectors.toList());

		return responseList;
	}

	public void processUpdateStatus(Long id, Integer status, ApiResponseEntity apiResponseEntity) {
		wUserMapper.updateUserStatus(id, status);
	}

	private boolean checkImage(String extension) {
		return extension.equals("jpeg;base64") || extension.equals("png;base64") || extension.equals("pdf;base64")
				|| extension.equals("jpg;base64");
	}

	public Map<Integer, String> processcreate(CreateUserRequest request, UserDetail userDetail,
			ApiResponseEntity apiResponseEntity) throws Exception {
		Map<Integer, String> error = new HashMap<>();
		String userRole = userDetail.getRole();
		if (!userRole.equals("admin")) {
			error.put(403, "Hành động không được phép");
			return error;
		}

		User user = generatorCreateEntity(request, error);
		if (error.size() > 0) {
			return error;
		}

		User userSave = repository.save(user);

		apiResponseEntity.setData(Map.of("userId", userSave.getId(), "role", userSave.getRole()));
		apiResponseEntity.setErrorList(null);
		apiResponseEntity.setStatus(1);

		return error;
	}

	private User generatorCreateEntity(CreateUserRequest request, Map<Integer, String> error) throws Exception {
		User user = new User();
		String requestRole = request.getRole();
		Boolean isExpert = requestRole.equals("expert");
		if (!requestRole.equals("admin") && !isExpert) {
			error.put(200, "Chỉ được phép tạo chuyên gia hoặc admin");
		}

		String imageUrl = request.getImageUrlBase64();
		if (isExpert && Objects.isNull(imageUrl)) {
			error.put(200, "Chuyên gia cần phải có hình ảnh");
		}

		BeanUtils.copyProperties(request, user);
		if (!requestRole.equals("admin")) {
			String[] imageBase64 = imageUrl.split(",");
			boolean isImage = checkImage(imageBase64[0].split("/")[1]);

			if (!isImage) {
				error.put(200, "Chỉ nhận file hình ảnh hoặc file pdf");
			}

			user.setImageUrl(restTemplateClient.uploadFile(imageUrl));
			com.se1.userservice.domain.payload.request.CreateUserRequest.ExpertInfo expertInfo = request
					.getExpertInfo();
			if (Objects.isNull(expertInfo)) {
				error.put(200, "Thông tin chuyên gia không được trống");
			}

			if (Objects.isNull(expertInfo.getDescription()) || expertInfo.getDescription().size() < 0) {
				error.put(200, "Mô tả chuyên gia không được trống");
			}

			BeanUtils.copyProperties(expertInfo, user);
		}

		user.setPassword(passwordEncoder.encode(user.getPassword()));
		user.setCreateAt(new Date());
		user.setUpdateAt(new Date());
		user.setLastTime(new Date());
		user.setEmailVerified(true);
		user.setProvider(AuthProvider.local);
		user.setStatus(SCMConstant.DEL_FLG_OFF);
		user.setDelFlg(false);
		user.setTopicId(UUID.randomUUID().toString());
		user.setRole(UserRole.valueOf(requestRole));
		user.setIsExpert(isExpert);

		return user;

	}

	public void findAllReport(FindAllReportRequest request, UserDetail userDetail, Integer offset,
			ApiResponseEntity apiResponseEntity) {
		String nameQuery = !request.getName().isEmpty() ? " u.name like '%" + request.getName() + "%'" : "";
		String emailQuery = !request.getEmail().isEmpty() ? " u.email like '%" + request.getEmail() + "%'" : "";

		List<String> mergeQuery = new ArrayList<>();
		if (!nameQuery.equals("")) {
			mergeQuery.add(nameQuery);
		}

		if (!emailQuery.equals("")) {
			mergeQuery.add(emailQuery);
		}

		List<ReportUserDto> allUser = rUserMapper.findAllHaveReport(mergeQuery, offset);
		List<ReportUserResponseDto> responseDtos = allUser.stream().map(rp -> {
			ReportUserResponseDto dto = new ReportUserResponseDto();
			BeanUtils.copyProperties(rp, dto);
			if (rp.getReasons() != null) {
				dto.setReasons(List.of(rp.getReasons().split(",")));
			}
			return dto;
		}).collect(Collectors.toList());

		apiResponseEntity.setData(responseDtos);
		apiResponseEntity.setErrorList(null);
		apiResponseEntity.setStatus(1);
	}

	public void update(UpdateUserRequest request, UserDetail userDetail, ApiResponseEntity apiResponseEntity)
			throws Exception {
		Boolean isCurrenUser = userDetail.getId().equals(request.getId());
		if (!userDetail.getRole().equals("admin") && !isCurrenUser) {
			throw new Exception("Hành đông không cho phép");
		}
		Optional<User> userFind = repository.findById(request.getId());
		if (userFind.isEmpty()) {
			throw new Exception("Người dùng không tồn tại");
		}

		User userOld = userFind.get();
		if (request.getName() != null) {
			userOld.setName(request.getName());
		}
		String imageUrl = "";
		if (request.getImageUrlBase64() != null) {
			imageUrl = restTemplateClient.uploadFile(request.getImageUrlBase64());
		}
		if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.isBlank()) {
			userOld.setImageUrl(imageUrl);
		}
		if (request.getPassword() != null) {
			userOld.setPassword(passwordEncoder.encode(request.getPassword()));
		}
		if (userOld.getRole().name().equals("expert") && request.getExpertInfo() != null) {
			com.se1.userservice.domain.payload.request.UpdateUserRequest.ExpertInfo expertInfo = request
					.getExpertInfo();
			if (expertInfo.getPhoneNumber() != null) {
				userOld.setPhoneNumber(expertInfo.getPhoneNumber());
			}
			if (expertInfo.getJobTitle() != null) {
				userOld.setJobTitle(expertInfo.getJobTitle());
			}
			if (expertInfo.getSpecialist() != null) {
				userOld.setSpecialist(expertInfo.getSpecialist());
			}
			if (expertInfo.getWorkPlace() != null) {
				userOld.setWorkPlace(expertInfo.getWorkPlace());
			}
		}

		User userUpdate = repository.save(userOld);
		if (userUpdate != null) {
			apiResponseEntity.setData(userUpdate.getId());
			apiResponseEntity.setErrorList(null);
			apiResponseEntity.setStatus(1);
		}

	}

	public Object findAllExpert(UserDetail userDetail, Integer offset) {
		@SuppressWarnings("removal")
		Long userDetailId = userDetail != null ? userDetail.getId() : Long.valueOf("0");

		List<User> users = repository.findAllByRole(UserRole.expert);
		List<UserResponseForClient> responseList = users.stream().filter(ul -> ul.getEmailVerified() && !ul.getDelFlg())
				.map(ul -> {
					Double rating = 0.0;
					Integer ratingCount = null;
					Boolean isRate = false;
					if (ul.getIsExpert()) {
						rating = (Double) ratingService.getRatingByUserId(ul.getId(), userDetailId).get("rating");
						ratingCount = (Integer) ratingService.getRatingByUserId(ul.getId(), userDetailId).get("count");
						isRate = (Boolean) ratingService.getRatingByUserId(ul.getId(), userDetailId).get("isRate");
					}
					UserResponseForClient userResponseDto = convertUserEntityToUserResponseForClient(ul, rating,
							userDetailId, ratingCount, isRate);
					return userResponseDto;
				}).collect(Collectors.toList());

		return responseList;
	}

	public void delete(Long id, UserDetail userDetail, ApiResponseEntity apiResponseEntity) {
		repository.deleteById(id);

		apiResponseEntity.setData(true);
		apiResponseEntity.setErrorList(null);
		apiResponseEntity.setStatus(1);
	}

	public void report(Long id, UserDetail userDetail, ApiResponseEntity apiResponseEntity, String reason) {
		ReportUser reportUser = new ReportUser();
		reportUser.setUserId(id);
		reportUser.setReason(reason);

		reportUserRepository.save(reportUser);
		apiResponseEntity.setData(true);
		apiResponseEntity.setErrorList(null);
		apiResponseEntity.setStatus(1);
	}

	public Object findAll(FindAllUserRequest request, UserDetail userDetail, Integer offset) {
		Long userId = userDetail.getId();

		String userQuery = String.format(" id != %d", userId);
		String nameQuery = !request.getName().isEmpty() ? " name like '%" + request.getName() + "%'" : "";
		String emailQuery = !request.getEmail().isEmpty() ? " email like '%" + request.getEmail() + "%'" : "";
		String providerQuery = (!Objects.isNull(request.getProvider()) && request.getProvider().size() > 0)
				? " provider in (" + String.join(", ",
						request.getProvider().stream().map(p -> String.format("'%s'", p)).collect(Collectors.toList()))
						+ ")"
				: "";
		String roleQuery = (!Objects.isNull(request.getRole()) && request.getRole().size() > 0) ? " role in ("
				+ String.join(", ",
						request.getRole().stream().map(r -> String.format("'%s'", r)).collect(Collectors.toList()))
				+ ")" : "";

		List<String> mergeQuery = List.of(userQuery, nameQuery, emailQuery, providerQuery, roleQuery);
		mergeQuery = mergeQuery.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
		List<User> allUser = rUserMapper.findAll(mergeQuery, offset);
		return allUser;
	}

	public Object processUpdateEmailStatus(Long id) {
		wUserMapper.updateEmailStatus(id);
		return true;
	}

	public List<User> getUser(GetApiRequestEntity request) throws Exception {
		List<User> users = new ArrayList<>();
		try {
			users = rUserMapper.selectByConditionStr(request.getConditionStr(), request.getOrder(), request.getLimit(),
					request.getOffset());
		} catch (Exception e) {
			log.error("Call Internal API ERROR getUser ", e);
			throw new Exception(e.getMessage());
		}

		return users;
	}

	public Boolean updateUser(UpdateApiRequestEntity request) {
		TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
		try {
			wUserMapper.updateUser(request.getRecordStr(), request.getConditionStr(), request.getOrder(),
					request.getLimit(), request.getOffset());
		} catch (Exception e) {
			transactionManager.rollback(txStatus);
			log.error("Call Internal API ERROR updateUser ", e);
			throw e;
		}
		transactionManager.commit(txStatus);
		return true;
	}
	
	public Boolean insertUser(UpdateApiRequestEntity request) {
		TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
		try {
			wUserMapper.insertUser(request.getRecordStr(), request.getConditionStr(), request.getOrder(),
					request.getLimit(), request.getOffset());
		} catch (Exception e) {
			transactionManager.rollback(txStatus);
			log.error("Call Internal API ERROR updateUser ", e);
			throw e;
		}
		transactionManager.commit(txStatus);
		return true;
	}
}
