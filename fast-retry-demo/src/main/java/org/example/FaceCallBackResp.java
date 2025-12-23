package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FaceCallBackResp {

    /**
     * 人脸识别状态
     * 1：成功，其他为失败
     */
    private Long status;

    /**
     * 合同id
     */
    private Long contractId;

    /**
     * 签署人id
     */
    private Long signerId;

    /**
     * 人脸识别token
     */
    private String faceToken;

}
