package com.solver.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.solver.api.request.UserRegistPostReq;
import com.solver.api.service.UserService;
import com.solver.common.model.BaseResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value="유저 API", tags = {"User"})
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
	@Autowired
	UserService	userService;
	
	@PostMapping("/signup")
	@ApiOperation(value = "회원 가입", notes = "아이디, 패스워드, 닉네임과 화상 가능 시간을 입력해 회원가입 한다.") 
    @ApiResponses({
        @ApiResponse(code = 201, message = "화원가입에 성공했습니다"),
        @ApiResponse(code = 409, message = "회원가입에 실패했습니다")
    })
	public ResponseEntity<Integer> registUser(
			@RequestBody @ApiParam(value="회원가입 정보", required=true) UserRegistPostReq userRegistPostReq)
	{
		userService.createUser(userRegistPostReq);
		
		return null;
		
	}
	
	@GetMapping("/signup/check-nickname")
	@ApiOperation(value = "닉네임 중복 체크", notes = "회원가입시 닉네임중복 체크") 
    @ApiResponses({
        @ApiResponse(code = 200, message = "사용 가능한 닉네임입니다"),
        @ApiResponse(code = 409, message = "이미 사용중인 닉네임입니다")
    })
	public ResponseEntity<? extends BaseResponse> checkNickname(
			@RequestParam @ApiParam(value="닉네임", required=true) String nickname)
	{
		if(userService.checkNickname(nickname).orElse(null) == null)
			return ResponseEntity.status(200).body(BaseResponse.of(200, "사용 가능한 닉네임입니다"));
		
		return ResponseEntity.status(409).body(BaseResponse.of(409, "이미 사용중인 닉네임입니다"));
		
	}
	
	@GetMapping("/signup/check-login")
	@ApiOperation(value = "ID 중복 체크", notes = "회원가입시 닉네임중복 체크") 
    @ApiResponses({
        @ApiResponse(code = 200, message = "사용 가능한 ID입니다"),
        @ApiResponse(code = 409, message = "이미 사용중인 ID입니다")
    })
	public ResponseEntity<? extends BaseResponse> checkLoginId(
			@RequestParam @ApiParam(value="Login ID", required=true) String loginId)
	{
		if(userService.checkLoginId(loginId).orElse(null) == null)
			return ResponseEntity.status(200).body(BaseResponse.of(200, "사용 가능한 ID입니다"));
		
		return ResponseEntity.status(409).body(BaseResponse.of(409, "이미 사용중인 ID입니다"));
		
	}
}
