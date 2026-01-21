package smartebao.guide.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseData {
    @JsonProperty("code")
    private int code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private Object data;

    public ResponseData(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static ResponseData success(String message, Object data) {
        return new ResponseData(200, message, data);
    }

    public static ResponseData success(Object data) {
        return new ResponseData(200, "操作成功", data);
    }

    public static ResponseData error(String message) {
        return new ResponseData(500, message, null);
    }

    public static ResponseData error(int code, String message) {
        return new ResponseData(code, message, null);
    }
}