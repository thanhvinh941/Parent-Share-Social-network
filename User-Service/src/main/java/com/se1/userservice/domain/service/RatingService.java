package com.se1.userservice.domain.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.se1.userservice.domain.model.Rating;
import com.se1.userservice.domain.model.User;
import com.se1.userservice.domain.payload.UserDetail;
import com.se1.userservice.domain.payload.request.DoRatingRequest;
import com.se1.userservice.domain.repository.RatingRepository;
import com.se1.userservice.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RatingService {

	private final RatingRepository ratingRepository;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;
	
	Map<String, Object> getRatingByUserId(Long userId, Long currentId) {
		List<Rating> expertRatings = ratingRepository.findByUserRatedId(userId);
		if (expertRatings != null && expertRatings.size() > 0) {
			List<Double> ratings = expertRatings.stream().map(ex -> ex.getRating()).collect(Collectors.toList());
			Boolean isRate = false;
			if (currentId != null) {
				List<Rating> isRateList = expertRatings.stream().filter(r -> r.getUserRatingId().equals(currentId))
						.collect(Collectors.toList());
				if (isRateList.size() > 0) {
					isRate = true;
				}
			}
			return Map.of("rating", ratings.stream().mapToDouble(d -> d).average().getAsDouble(), "count",
					Integer.valueOf(ratings.size()), "isRate", isRate);
		}
		return Map.of("rating", 0.0, "count", Integer.valueOf(0), "isRate", false);
	}

	public Boolean processDoRating(DoRatingRequest request, Long userId) throws Exception {
		Long expertId = request.getExpertId();
		Double rate = 0.0;
		if (request.getRate() <= 5 && request.getRate() > 0) {
			rate = request.getRate();
		} else if (request.getRate() > 5) {
			rate = 5.0;
		}
		User user = userRepository.findExpertById(expertId);
		if (user == null) {
			throw new Exception("Chuyên gia không tồn tại hoặc người dùng không phải chuyên gia");
		}
		Rating rating = new Rating();
		Rating ratingFind = ratingRepository.findByUserRatedIdAndUserRatingId(expertId, userId);
		if (ratingFind != null) {
			BeanUtils.copyProperties(ratingFind, rating);
			rating.setRating(rate);
		} else {
			rating.setCreateAt(new Date());
			rating.setRating(rate);
			rating.setUserRatedId(expertId);
			rating.setUserRatingId(userId);
		}

		Rating ratingSave = ratingRepository.save(rating);
		if (ratingSave == null) {
			throw new Exception("Đánh giá chuyên gia thất bại");
		}
		return true;
	}

	public List<Map<String, Object>> processGetUesrRating(Long expertid, Long id) throws Exception {
		User userExpert = userRepository.findExpertById(expertid);
		if (userExpert == null) {
			throw new Exception("Chuyên gia không tồn tại hoặc người dùng không phải chuyên gia");
		}

		List<Rating> ratings = ratingRepository.findByUserRatedId(expertid);
		List<Long> userRatingIds = ratings.stream().map(m -> m.getUserRatingId()).collect(Collectors.toList());
		Iterable<User> userRatings = userRepository.findAllById(userRatingIds);
		List<Map<String, Object>> response = StreamSupport.stream(userRatings.spliterator(), false).map(m -> {
			UserDetail detail = new UserDetail();
			BeanUtils.copyProperties(m, detail);
			Map<String, Object> map = objectMapper.convertValue(detail, new TypeReference<Map<String, Object>>() {
			});
			map.remove("role");
			map.remove("isExpert");
			map.remove("expertInfo");
			return map;
		}).collect(Collectors.toList());

		return response;
	}
}
