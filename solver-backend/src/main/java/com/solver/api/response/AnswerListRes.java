package com.solver.api.response;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.solver.common.model.BaseResponse;
import com.solver.db.entity.answer.Answer;
import com.solver.db.entity.answer.FavoriteAnswer;
import com.solver.db.entity.code.Code;
import com.solver.db.entity.user.User;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("AnswerListRes")
public class AnswerListRes extends BaseResponse{
	private List<AnswerInfo> answerList;
	
	public static AnswerListRes of(Integer statusCode, String message, List<Answer> answerList, String nickname) {
		AnswerListRes res = new AnswerListRes();
		
		List<AnswerInfo> tempAnswerList = new ArrayList<AnswerInfo>();
		
		for (Answer answer : answerList) {
			AnswerInfo answerInfo = new AnswerInfo();
			User user = answer.getUser();
			Code type = answer.getCode();
			
			answerInfo.setAnswerId(answer.getId());
			answerInfo.setContent(answer.getContent());
			answerInfo.setUserId(user.getId());
			answerInfo.setNickname(user.getNickname());
			answerInfo.setType(type.getCodeName());
			answerInfo.setProfileUrl(user.getProfileUrl());
			answerInfo.setRegDt(answer.getRegDt());
			answerInfo.setLikeCount(answer.getFavoriteAnswer().size());
			answerInfo.setCommentCount(answer.getComment().size());
			
			List<FavoriteAnswer> favoriteAnswerList = answer.getFavoriteAnswer();
			
			for (FavoriteAnswer favoriteAnswer : favoriteAnswerList) {
				if(favoriteAnswer.getUser().getNickname().equals(nickname)) {
					answerInfo.setMyFavoriteAnswer(true);
					break;
				}
			}
			
			tempAnswerList.add(answerInfo);
		}
		
		res.setStatusCode(statusCode);
		res.setMessage(message);
		res.setAnswerList(tempAnswerList);
		
		return res;
	}
	
	public static AnswerListRes of(Integer statusCode, String message) {
		AnswerListRes res = new AnswerListRes();
		
		res.setStatusCode(statusCode);
		res.setMessage(message);
		
		return res;
	}
}

@Getter
@Setter
class AnswerInfo{
	private String answerId;
	private String userId;
	private String nickname;
	private String profileUrl;
	private String content;
	private String type;
	private Date regDt;
	private int likeCount;
	private int commentCount;
	private boolean myFavoriteAnswer;
}