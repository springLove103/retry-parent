package org.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author meng
 * @version 1.0
 * @date 2022/6/17 0017
 * @description
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AsyncNoticeRecordEntity {

	/**
	 * 合同id
	 */
	private Long contractId;

	/**
	 * 摘要地址
	 */
	private String urlDigest;

	/**
	 * 通知结果
	 */
	private String noticeResult;

	/**
	 * 通知次数
	 */
	private Integer retryTimes;

	/**
	 * 成功或失败
	 */
	private Boolean successful;

	/**
	 * 错误原因：仅在出错时有数据
	 */
	private String errorMessage;

	/**
	 * 服务编码 0 诚信签 1 云盾 2 出证 3 后台签署-人脸回调通知
	 */
	private Integer serviceCode;
}
