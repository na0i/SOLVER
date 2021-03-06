package com.solver.db.repository.question;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.solver.db.entity.question.BookmarkQuestion;

public interface BookmarkQuestionRepository extends JpaRepository<BookmarkQuestion, String>{

	List<BookmarkQuestion> findAllByUserId(String userId);

	Optional<BookmarkQuestion> findByUserIdAndQuestionId(String userId, String questionId);

	List<BookmarkQuestion> findByQuestionId(String questionId);

}
