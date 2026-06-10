package com.training.mapper;

import com.training.entity.Certificate;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CertificateMapper {

    Certificate selectById(Long id);

    Certificate selectByCertNo(String certNo);

    Certificate selectByVerifyToken(String verifyToken);

    Certificate selectByStudentAndCourse(@Param("studentId") Long studentId,
                                         @Param("courseId") Long courseId);

    List<Certificate> selectByStudentId(Long studentId);

    List<Certificate> selectList(@Param("keyword") String keyword,
                                 @Param("status") String status);

    int insert(Certificate certificate);

    int updateById(Certificate certificate);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateVerifyToken(@Param("id") Long id,
                          @Param("verifyToken") String verifyToken,
                          @Param("verifyTokenExpiresAt") LocalDateTime verifyTokenExpiresAt);
}
