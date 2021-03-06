package com.solver.api.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.solver.api.request.ProfilePossibleTimePatchReq;
import com.solver.api.request.ProfileUpdatePatchReq;
import com.solver.api.response.ProfileRes;
import com.solver.api.response.ProfileTabRes;
import com.solver.common.auth.KakaoUtil;
import com.solver.common.model.TokenResponse;
import com.solver.common.util.RandomIdUtil;
import com.solver.db.entity.answer.Answer;
import com.solver.db.entity.answer.Evaluation;
import com.solver.db.entity.code.Category;
import com.solver.db.entity.code.Code;
import com.solver.db.entity.code.FavoriteField;
import com.solver.db.entity.code.PointCode;
import com.solver.db.entity.conference.ConferenceLog;
import com.solver.db.entity.question.BookmarkQuestion;
import com.solver.db.entity.question.Question;
import com.solver.db.entity.user.FavoriteUser;
import com.solver.db.entity.user.Notification;
import com.solver.db.entity.user.PointLog;
import com.solver.db.entity.user.User;
import com.solver.db.entity.user.UserCalendar;
import com.solver.db.repository.answer.AnswerRepository;
import com.solver.db.repository.answer.EvaluationRepository;
import com.solver.db.repository.code.CategoryRepository;
import com.solver.db.repository.code.CodeRepository;
import com.solver.db.repository.code.FavoriteFieldRepository;
import com.solver.db.repository.code.PointCodeRepository;
import com.solver.db.repository.conference.ConferenceLogRepository;
import com.solver.db.repository.question.BookmarkQuestionRepository;
import com.solver.db.repository.question.QuestionRepository;
import com.solver.db.repository.user.FavoriteUserRepository;
import com.solver.db.repository.user.NotificationRepository;
import com.solver.db.repository.user.PointLogRepository;
import com.solver.db.repository.user.UserCalendarRepository;
import com.solver.db.repository.user.UserRepository;

@Service
public class ProfileServiceImpl implements ProfileService{
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	QuestionRepository questionRepository;
	
	@Autowired
	ConferenceLogRepository conferenceLogRepository;
	
	@Autowired
	BookmarkQuestionRepository bookmarkQuestionRepository;
	
	@Autowired
	AnswerRepository answerRepository;
	
	@Autowired
	UserCalendarRepository userCalendarRepository;
	
	@Autowired
	PointLogRepository pointLogRepository;
	
	@Autowired
	FavoriteFieldRepository favoriteFieldRepository;
	
	@Autowired
	EvaluationRepository evaluationRepository;
	
	@Autowired
	PointCodeRepository pointCodeRepository;
	
	@Autowired
	CategoryRepository categoryRepository;
	
	@Autowired
	FavoriteUserRepository favoriteUserRepository;
	
	@Autowired
	KakaoUtil kakaoUtil;

	@Autowired
	NotificationRepository notificationRepository;

	@Autowired
	CodeRepository codeRepository;
	
	private AmazonS3 s3Client;
	
	private String s3Url = "https://solver-bucket.s3.ap-northeast-2.amazonaws.com/";
	
    @PostConstruct          
    public void setS3Client(){
        AWSCredentials credentials = new BasicAWSCredentials("AKIARMPAI5JURFO5RVG5", "2VNZUcbCHPFewyyOxPLZndakCtAEO0ZdvvaRHYww");

        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();
    }

	/* ??????????????? ????????? ?????? ??? ??? ???????????? ??????????????? ?????? ????????? ???????????? - o
	 * ??????????????? sub category ???????????? - o
	 * */
	@Override
	public ProfileRes getProfileInfo(String token, String nickname) {
		Optional<User> user = userRepository.findByNickname(nickname);
		
		List<PointLog> pointList = user.get().getPointLog();
		List<Evaluation> evaluationList = user.get().getEvaluateAnswer();
		List<FavoriteField> favoriteFieldList = user.get().getFavoriteField();
		
		/* ????????? ?????? - entity ????????? ??????????????? ?????? ?????? */
		int point = 0;
		int remainingPoint = 0;
		
		int plus = 0, minus = 0;
		
		for (PointLog pointLog : pointList) {
			PointCode pointCode = pointCodeRepository.findByPointCode(pointLog.getPointCode().getPointCode());
			
			if(pointCode.getPointCode().charAt(0) == '0') {
				plus += pointCode.getValue();
			}else {
				minus += pointCode.getValue();
			}
			
//			if(pointCode.getPointCode().equals("100") || pointCode.getPointCode().equals("101") || pointCode.getPointCode().equals("102")) {
//				remainingPoint -= pointCode.getValue();
//				
//			}
//			else {
//				point += pointCode.getValue();
//			}
		}
		
		point = plus;
		remainingPoint = plus - minus;
		
		/* ????????? ?????? ???*/
		
		/* ?????? ?????? */
		
		float evaluationScore = 0;
		
		for (Evaluation evaluation : evaluationList) {
			evaluationScore += evaluation.getScore();
		}
		
		evaluationScore /= evaluationList.size();
		
		/* ?????? ?????? ??? */
		
		/* ?????? ?????? ?????? ????????? ?????? */
		List<String> favoriteFieldNameList = new ArrayList<>();
		List<String> favoriteFieldCodeList = new ArrayList<>();
		
		for (FavoriteField favoriteField : favoriteFieldList) {
			favoriteFieldNameList.add(favoriteField.getCategory().getSubCategoryName());
			favoriteFieldCodeList.add(favoriteField.getCategory().getSubCategoryCode());
		}
		/* ?????? ?????? ?????? ????????? ?????? ??? */
		
		/* response ????????? ?????? */
		ProfileRes profileRes = new ProfileRes();
		
		if (Double.isNaN(evaluationScore)) {
			evaluationScore = 0;
		}
		
		/* ?????? ??? ?????? ????????? ?????? */
		boolean isFollowing = false;
		if (!token.equals("null")) {
			TokenResponse tokenResponse = new TokenResponse();
			tokenResponse = kakaoUtil.getKakaoUserIdByToken(token);
	
			Long kakaoId = tokenResponse.getKakaoId();
			
			Optional<User> me = userRepository.findByKakaoId(kakaoId);
			
			for (FavoriteUser favoriteUser : user.get().getFavoriteUser()) {
				if (favoriteUser.getFollowingUser().getId().equals(me.get().getId())) {
					isFollowing =  true;
					break;
				}
			}
		}
		
		profileRes.setEvaluationScore(evaluationScore);
		profileRes.setFavoriteFieldCodeList(favoriteFieldCodeList);
		profileRes.setFavoriteFieldNameList(favoriteFieldNameList);
		profileRes.setPoint(point);
		profileRes.setRemainingPoint(remainingPoint);
		profileRes.setNickname(user.get().getNickname());
		profileRes.setIntroduction(user.get().getIntroduction());
		profileRes.setLinkText(user.get().getLinkText());
		profileRes.setProfileUrl(user.get().getProfileUrl());
		
		profileRes.setWeekdayTime(user.get().getUserCalendar().getWeekdayTime());
		profileRes.setWeekendTime(user.get().getUserCalendar().getWeekendTime());
		
		profileRes.setFollowers(user.get().getFavoriteUser().size());
		profileRes.setFollowings(user.get().getFavoriteFollowingUser().size());
		profileRes.setFollowing(isFollowing);
		/* response ????????? ?????? ??? */
		
		return profileRes;
	}

	//????????? ????????? ?????? ?????? ?????? ??????
	@Override
	public void updateProfile(ProfileUpdatePatchReq profileUpdatePatchReq, String accessToken, HttpServletResponse response) {
		String token = accessToken.split(" ")[1];

		TokenResponse tokenResponse = new TokenResponse();
		
		tokenResponse = kakaoUtil.getKakaoUserIdByToken(token);

		Long kakaoId = tokenResponse.getKakaoId();
		
		if(tokenResponse.getAccessToken() != null) {
			response.setHeader("Authorization", tokenResponse.getAccessToken());
		}

		
		User user = userRepository.findByKakaoId(kakaoId).get();
		//????????? ?????? ????????? ?????? ??????
		user.setNickname(profileUpdatePatchReq.getNickname());
		user.setProfileUrl(profileUpdatePatchReq.getProfileUrl());
		user.setIntroduction(profileUpdatePatchReq.getIntroduction());
		user.setLinkText(profileUpdatePatchReq.getLinkText());
		
		userRepository.save(user);
		
		List<String> categoryList = profileUpdatePatchReq.getCategoryCodeList();
		favoriteFieldRepository.deleteByUserId(user.getId());
		
		if(categoryList == null) {
            return;
        }
		
		for (String subCategoryCode : categoryList) {
			FavoriteField favoriteField = new FavoriteField();
			String id = "";
			
			if(categoryList == null) {
	            return;
	        }
			
			while(true) {
				id = RandomIdUtil.makeRandomId(13);
				
				if(favoriteFieldRepository.findById(id).orElse(null) == null)
					break;
						
			}
			
			Category category = categoryRepository.findBySubCategoryCode(subCategoryCode);
			
			if(category == null)
				continue;
			
			favoriteField.setId(id);
			favoriteField.setUser(user);
			favoriteField.setCategory(category);
			
			favoriteFieldRepository.save(favoriteField);
		}
	}

	@Override
	public void updateProfilePossibleTime(ProfilePossibleTimePatchReq profilePossibleTimePatchReq, String accessToken, HttpServletResponse response) {
		String token = accessToken.split(" ")[1];

		TokenResponse tokenResponse = new TokenResponse();
		
		tokenResponse = kakaoUtil.getKakaoUserIdByToken(token);

		Long kakaoId = tokenResponse.getKakaoId();
		
		if(tokenResponse.getAccessToken() != null) {
			response.setHeader("Authorization", tokenResponse.getAccessToken());
		}

		
		User user = userRepository.findByKakaoId(kakaoId).get();
		
		UserCalendar userCalendar = userCalendarRepository.findByUserId(user.getId());
		
		userCalendar.setWeekdayTime(profilePossibleTimePatchReq.getWeekdayTime());
		userCalendar.setWeekendTime(profilePossibleTimePatchReq.getWeekendTime());
		
		userCalendarRepository.save(userCalendar);
		
	}

	/* ?????? ??? ?????? ??????
	 * tabNum
	 * 0: SOLVE ??????
	 * 1: ?????? ??????
	 * 2: ?????? ?????? 
	 * */
	@Override
	public ProfileTabRes getProfileTabInfo(String nickname, int tabNum) {
		ProfileTabRes profileTabRes = new ProfileTabRes();
		User user = userRepository.findByNickname(nickname).orElse(null);
		
		String userId = user.getId();
		
		if(tabNum == 0) {
			List<Answer> textAnswerList = answerRepository.findTextAnswerByUserId(userId);
			List<Answer> videoAnswerList = answerRepository.findVideoAnswerByUserId(userId);
			
			int textAnswerCount = textAnswerList.size();
			int videoAnswerCount = videoAnswerList.size();
			int videoAnswerTime = 0;
			
			List<ConferenceLog> conferenceEnterList = conferenceLogRepository.findByUserIdOrderByConferenceId(userId);
			
			for (ConferenceLog conferenceLog : conferenceEnterList) {
				//??????
				if(conferenceLog.getCode().getCode().equals("030")) {
					videoAnswerTime -= conferenceLog.getRegDt().getTime();
				}
				//??????
				else if(conferenceLog.getCode().getCode().equals("031")) {
					videoAnswerTime += conferenceLog.getRegDt().getTime();
				}
			}
			
			videoAnswerTime /= (1000*60);
			
			List<Answer> myAnswerList = new ArrayList<Answer>();
			myAnswerList.addAll(textAnswerList);
			myAnswerList.addAll(videoAnswerList);
			
			profileTabRes = ProfileTabRes.makeAnswerStatistics(videoAnswerTime, videoAnswerCount, textAnswerCount, myAnswerList);
		}
		//????????? ?????? ?????? ??????
		else if(tabNum == 1) {
			List<Answer> answerList = answerRepository.findByUserId(userId);
			
			Set<String> questionIdSet = new HashSet<>();
			
			for (Answer answer : answerList) {
				questionIdSet.add(answer.getQuestion().getId());
			}
			
			List<String> questionIdList = new ArrayList<>(questionIdSet);
			
			if(questionIdList.size() != 0) {
				List<Question> answerQuestionList = questionRepository.findAllById(questionIdList);
			
				profileTabRes = ProfileTabRes.makeAnswerQuestionList(answerQuestionList);
			}
		}
		//?????? ????????? ?????? ??????
		else if(tabNum == 2) {
			List<Question> questionList = questionRepository.findByUserId(userId);
			
			profileTabRes = ProfileTabRes.makeMyQuestionList(questionList);
		}
		//?????? ???????????? ?????? ??????
		else if(tabNum == 3) {
			List<BookmarkQuestion> bookmarkList = bookmarkQuestionRepository.findAllByUserId(userId);
			
			Set<String> questionIdSet = new HashSet<>();
			
			for (BookmarkQuestion bookmark : bookmarkList) {
				questionIdSet.add(bookmark.getQuestion().getId());
			}
			
			List<String> questionIdList = new ArrayList<>(questionIdSet);
			
			List<Question> questionList = questionRepository.findAllById(questionIdList);
			
			profileTabRes = ProfileTabRes.makeBookmarkQuestionList(questionList);
		}
		
		return profileTabRes;
	}

	@Override
	public int followUser(String accessToken, String nickname, HttpServletResponse response) {
		String token = accessToken.split(" ")[1];

		TokenResponse tokenResponse = new TokenResponse();
		
		tokenResponse = kakaoUtil.getKakaoUserIdByToken(token);

		Long kakaoId = tokenResponse.getKakaoId();
		
		if(tokenResponse.getAccessToken() != null) {
			response.setHeader("Authorization", tokenResponse.getAccessToken());
		}


		User myUserInfo = userRepository.findByKakaoId(kakaoId).orElse(null);

		//?????? ????????? ??????
		if (myUserInfo == null) {
			return 0;
		}

		User followedUserInfo = userRepository.findByNickname(nickname).orElse(null);
		
		//?????? ????????? ??????
		if (followedUserInfo == null) {
			return 0;
		}

		String id = "";

		while (true) {
			id = RandomIdUtil.makeRandomId(13);

			if (favoriteUserRepository.findById(id).orElse(null) == null)
				break;
		}

		//?????? ????????? ??? ????????? ??????
		if (favoriteUserRepository.findByUserIdAndFollowingUserId(followedUserInfo.getId(), myUserInfo.getId()).orElse(null) != null) {
			return 2;
		}

		
		// ?????? ?????? : ???????????? ?????? ???????????? ????????? ?????????
		Notification notification = new Notification();
		notification.setId(RandomIdUtil.makeRandomId(13));
		notification.setQuestion(null); // ????????? ???????????? null ??????
		notification.setRegDt(new Date(System.currentTimeMillis()));
		
		Code notiCode = codeRepository.findByCode("066");
		notification.setCode(notiCode);
		notification.setUser(followedUserInfo);
		
		notificationRepository.save(notification);
		
		// ????????? ?????? ??????
		FavoriteUser favoriteUser = new FavoriteUser();
		favoriteUser.setFollowingUser(myUserInfo);
		favoriteUser.setUser(followedUserInfo);
		favoriteUser.setId(id);

		favoriteUserRepository.save(favoriteUser);

		return 3;
	}
	
	@Override
	public int unFollowUser(String accessToken, String nickname, HttpServletResponse response) {
		String token = accessToken.split(" ")[1];

		TokenResponse tokenResponse = new TokenResponse();
		
		tokenResponse = kakaoUtil.getKakaoUserIdByToken(token);

		Long kakaoId = tokenResponse.getKakaoId();
		
		if(tokenResponse.getAccessToken() != null) {
			response.setHeader("Authorization", tokenResponse.getAccessToken());
		}


		User myUserInfo = userRepository.findByKakaoId(kakaoId).orElse(null);

		//?????? ????????? ??????
		if (myUserInfo == null) {
			return 0;
		}

		User followedUserInfo = userRepository.findByNickname(nickname).orElse(null);
		
		//?????? ????????? ??????
		if (followedUserInfo == null) {
			return 0;
		}
		
		FavoriteUser favoriteUser = favoriteUserRepository.findByUserIdAndFollowingUserId(followedUserInfo.getId(), myUserInfo.getId()).orElse(null);

		//????????? ?????? ?????? ????????? ??????
		if (favoriteUser == null) {
			return 2;
		}

		favoriteUserRepository.delete(favoriteUser);

		return 3;
	}

	@Override
	public User getByNickname(String token, String nickname) {
		TokenResponse tokenResponse = new TokenResponse();
		tokenResponse = kakaoUtil.getKakaoUserIdByToken(token);
		
		Long kakaoId = tokenResponse.getKakaoId();
		
		User user = userRepository.findByKakaoId(kakaoId).orElse(null);
		User profileUser = userRepository.findByNickname(nickname).orElse(null);

		//?????? ????????? ??????
		if (user == null || profileUser == null) {
			return null;
		}
		
		return profileUser;
	}

	@Override
	public List<FavoriteUser> getFollowList(User user, int mode) {
		if (mode == 0) {
			return user.getFavoriteFollowingUser();
		}
		
		return user.getFavoriteUser();
	}

	@Override
	public String updateProfileImg(MultipartFile imgFile, String accessToken, HttpServletResponse response) {
		String token = accessToken.split(" ")[1];

		TokenResponse tokenResponse = new TokenResponse();

		tokenResponse = kakaoUtil.getKakaoUserIdByToken(token);

		Long kakaoId = tokenResponse.getKakaoId();

		if (tokenResponse.getAccessToken() != null) {
			response.setHeader("Authorization", tokenResponse.getAccessToken());
		}

		User user = userRepository.findByKakaoId(kakaoId).orElse(null);
		
		if (user == null) {
			return null;
		}
		
		byte[] bytes = null;
		
		try {
			bytes = imgFile.getBytes();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		UUID uuid = UUID.randomUUID();

		String saveFilename = uuid + ".png";
		
		System.out.println(imgFile.getContentType());
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentType(imgFile.getContentType());
		
		objectMetadata.setContentLength(bytes.length);
		ByteArrayInputStream byteArrayIs = new ByteArrayInputStream(bytes);
		
		s3Client.putObject(new PutObjectRequest("solver-bucket", saveFilename, byteArrayIs, objectMetadata)
				.withCannedAcl(CannedAccessControlList.PublicRead)); // public read ?????? ??????
		
		user.setProfileUrl(s3Url+saveFilename);
		
		userRepository.save(user);	
		
		return s3Url+saveFilename;
	}
}
