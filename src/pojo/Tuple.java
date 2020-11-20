package pojo;

public final class Tuple {
    final Integer clientId;
    final String message;
    final Integer requestNum;

    public Tuple(Integer clientId, String message, Integer requestNum) {
        this.clientId = clientId;
        this.message = message;
        this.requestNum = requestNum;
    }

    public Integer getClientId() {
        return clientId;
    }

    public String getMessage() {
        return message;
    }

    public Integer getRequestNum() {
        return requestNum;
    }
}
