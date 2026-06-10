package com.training.mapper;

import com.training.entity.CertificateRevocation;

import java.util.List;

public interface CertificateRevocationMapper {

    CertificateRevocation selectById(Long id);

    CertificateRevocation selectByCertificateId(Long certificateId);

    List<CertificateRevocation> selectAll();

    int insert(CertificateRevocation revocation);
}
