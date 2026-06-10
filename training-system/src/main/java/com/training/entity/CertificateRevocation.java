package com.training.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("certificate_revocation")
public class CertificateRevocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long certificateId;

    private String reason;

    private Long revokedBy;

    private LocalDateTime revokedAt;
}
