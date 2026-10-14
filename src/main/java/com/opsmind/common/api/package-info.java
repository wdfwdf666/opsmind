/**
 * 所有 HTTP 接口共享的基础设施。
 * <p>这里统一成功/失败响应格式，并把业务异常转换成合适的 HTTP 状态码，
 * 避免每个 Controller 重复编写 try/catch。</p>
 */
package com.opsmind.common.api;
