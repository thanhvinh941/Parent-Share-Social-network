package com.se1.postservice.domain.db.write;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.se1.postservice.domain.entity.TopicTag;

@Mapper
public interface WTopicTagMapper {

	Integer updateTopicTag(TopicTag topicTag);
}
