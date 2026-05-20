package com.rice.rice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rice.rice.common.BusinessException;
import com.rice.rice.dto.DiseasePredictionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 水稻病害识别预测服务
 * 负责调用 Python 深度学习模型 API 进行图像识别
 * 
 * 支持的接口：
 * - /predict: 通过文件上传方式识别
 * - /predict_base64: 通过 Base64 编码识别
 * - /health: 健康检查
 */
@Service
public class RiceDiseasePredictor {

    // ==================== 依赖注入 ====================
    private final RestTemplate restTemplate;        // HTTP 客户端（用于调用 Python API）
    private final ObjectMapper objectMapper;        // JSON 解析工具
    private final String pythonApiBaseUrl;          // Python API 的基础 URL（默认 http://localhost:5000）

    public RiceDiseasePredictor(
            ObjectMapper objectMapper,
            @Value("${app.python-api.base-url:http://localhost:5000}") String pythonApiBaseUrl) {
        this.objectMapper = objectMapper;
        this.pythonApiBaseUrl = pythonApiBaseUrl;
        
        // 配置 RestTemplate 超时时间
        ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        ((SimpleClientHttpRequestFactory) factory).setConnectTimeout(10000);  // 连接超时 10 秒
        ((SimpleClientHttpRequestFactory) factory).setReadTimeout(60000);     // 读取超时 60 秒（模型推理可能较慢）
        this.restTemplate = new RestTemplate(factory);
        
        // 设置 UTF-8 编码，防止中文乱码
        this.restTemplate.getMessageConverters().stream()
                .filter(c -> c instanceof StringHttpMessageConverter)
                .forEach(c -> ((StringHttpMessageConverter) c).setDefaultCharset(StandardCharsets.UTF_8));
    }

    /**
     * 通过文件上传方式进行病害识别
     * 调用 Python API 的 /predict 接口（multipart/form-data）
     *
     * @param file 水稻叶片图像文件（JPG 或 PNG 格式）
     * @return 预测结果（包含类别、置信度、防治建议等）
     */
    public DiseasePredictionResult predict(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图像文件");
        }

        try {
            // 验证文件类型（仅支持 JPG 和 PNG）
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/jpeg") && !contentType.startsWith("image/png"))) {
                throw new BusinessException("仅支持JPG和PNG格式的图像文件");
            }

            // 构建 multipart 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 调用 Python API 的 /predict 接口
            String url = pythonApiBaseUrl + "/predict";
            String response = restTemplate.postForObject(url, requestEntity, String.class);

            if (response == null || response.isEmpty()) {
                throw new BusinessException("模型服务返回为空");
            }

            return parsePredictionResponse(response);

        } catch (IOException e) {
            throw new BusinessException("读取图像文件失败：" + e.getMessage());
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException("调用模型服务失败：" + e.getMessage());
        }
    }

    /**
     * 根据图片 URL 拉取图像并进行病害识别
     * 支持三种格式：
     * 1. HTTP(S) 绝对地址：从网络下载图片
     * 2. Data URL (data:image/...;base64,...)：直接提取 Base64 数据
     * 3. 其他格式：抛出异常
     *
     * @param imageUrl 图片地址
     * @return 预测结果（包含类别、置信度、防治建议等）
     */
    public DiseasePredictionResult predictFromImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException("图片地址不能为空");
        }
        String url = imageUrl.trim();
        
        // 情况 1：Data URL 格式
        if (url.startsWith("data:")) {
            String b64 = extractBase64FromDataUrl(url);
            return predictFromBase64(b64);
        }
        
        // 情况 2：HTTP(S) 地址
        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                // 从网络下载图片
                ResponseEntity<byte[]> resp = restTemplate.exchange(
                        URI.create(url), HttpMethod.GET, null, byte[].class);
                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || resp.getBody().length == 0) {
                    throw new BusinessException("无法下载图片（HTTP " + resp.getStatusCode().value() + "）");
                }
                // 将图片转换为 Base64 并调用识别接口
                String b64 = Base64.getEncoder().encodeToString(resp.getBody());
                return predictFromBase64(b64);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException("下载图片失败：" + e.getMessage());
            }
        }
        
        // 情况 3：不支持的格式
        throw new BusinessException("仅支持 http(s) 或 data:image 形式的图片地址");
    }

    /**
     * 从 Data URL 中提取 Base64 数据
     * 支持格式：data:image/png;base64,xxxxx 或 data:image/png,xxxxx
     *
     * @param dataUrl Data URL 字符串
     * @return Base64 编码的图像数据
     */
    private static String extractBase64FromDataUrl(String dataUrl) {
        int base64Idx = dataUrl.indexOf("base64,");
        if (base64Idx >= 0) {
            return dataUrl.substring(base64Idx + "base64,".length()).trim();
        }
        int comma = dataUrl.indexOf(',');
        if (comma > 0) {
            return dataUrl.substring(comma + 1).trim();
        }
        throw new BusinessException("无法解析 data URL 图片");
    }

    /**
     * 通过 Base64 编码进行病害识别
     * 调用 Python API 的 /predict_base64 接口
     *
     * @param base64Image Base64编码的图像数据（不含 data:image/...;base64, 前缀）
     * @return 预测结果
     */
    public DiseasePredictionResult predictFromBase64(String base64Image) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            throw new BusinessException("图像数据不能为空");
        }

        try {
            // 构建请求体
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("image", base64Image);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 调用 Python API
            String url = pythonApiBaseUrl + "/predict_base64";
            String response = restTemplate.postForObject(url, requestEntity, String.class);

            if (response == null || response.isEmpty()) {
                throw new BusinessException("模型服务返回为空");
            }

            // 解析响应
            return parsePredictionResponse(response);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException("调用模型服务失败：" + e.getMessage());
        }
    }

    /**
     * 解析 Python 模型返回的 JSON 响应
     * 预期格式：
     * {
     *   "class_en": "Rice Blast",
     *   "class_cn": "稻瘟病",
     *   "confidence": 0.95,
     *   "all_probs": {"Rice Blast": 0.95, ...},
     *   "treatment": "防治建议..."
     * }
     *
     * @param jsonResponse JSON 字符串
     * @return 预测结果对象
     */
    private DiseasePredictionResult parsePredictionResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);

        // 检查是否有错误
        if (root.has("error")) {
            throw new BusinessException("模型识别失败：" + root.get("error").asText());
        }

        DiseasePredictionResult result = new DiseasePredictionResult();
        
        // 解析类别信息（英文和中文名称）
        result.setClassEn(root.path("class_en").asText(null));
        result.setClassCn(root.path("class_cn").asText(null));
        
        // 解析置信度（0-1 之间的小数）
        if (root.has("confidence")) {
            result.setConfidence(root.get("confidence").asDouble());
        }

        // 解析所有类别的概率分布（用于展示 Top 5）
        if (root.has("all_probs")) {
            Map<String, Double> allProbs = new HashMap<>();
            JsonNode probsNode = root.get("all_probs");
            Iterator<Map.Entry<String, JsonNode>> fields = probsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                allProbs.put(entry.getKey(), entry.getValue().asDouble());
            }
            result.setAllProbs(allProbs);
        }

        // 解析防治建议
        result.setTreatment(root.path("treatment").asText(null));

        // 验证必要字段
        if (result.getClassEn() == null || result.getClassCn() == null) {
            throw new BusinessException("模型返回格式异常：缺少类别信息");
        }

        return result;
    }

    /**
     * 健康检查：检测 Python 模型服务是否正常运行
     * 调用 /health 接口，预期返回 {"status":"ok"}
     *
     * @return true 表示服务正常，false 表示服务不可用
     */
    public boolean isHealthy() {
        try {
            String url = pythonApiBaseUrl + "/health";
            String response = restTemplate.getForObject(url, String.class);
            
            if (response != null && response.contains("\"status\":\"ok\"")) {
                return true;
            }
            return false;
        } catch (Exception e) {
            // 任何异常都视为服务不可用
            return false;
        }
    }
}
